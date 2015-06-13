package xebia.consul.client.dao

import java.net.URL

import akka.actor.ActorSystem
import retry.Success
import spray.client.pipelining._
import spray.http.{ HttpRequest, HttpResponse }
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.httpx.{ PipelineException, UnsuccessfulResponseException }
import spray.json._
import xebia.consul.client.util.{ Logging, RetryPolicy }

import scala.concurrent.Future
import scala.util.Try

class SprayConsulHttpClient(host: URL)(implicit actorSystem: ActorSystem) extends ConsulHttpClient with ConsulHttpProtocol with RetryPolicy with Logging {

  implicit val executionContext = actorSystem.dispatcher
  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  def extractIndex(response: HttpResponse)(block: Long => IndexedServiceInstances): IndexedServiceInstances = {
    response.headers.find(h => h.name == "X-Consul-Index").map { idx =>
      block(Try(idx.value.toLong).getOrElse(throw new PipelineException("X-Consul-Index header needs to be numerical")))
    }.getOrElse(throw new PipelineException("X-Consul-Index header not found"))
  }

  def unmarshalWithIndex: HttpResponse ⇒ IndexedServiceInstances =
    response ⇒
      if (response.status.isSuccess)
        extractIndex(response) { idx =>
          response.as[Option[Set[ServiceInstance]]] match {
            case Right(value) ⇒ value.map { v =>
              IndexedServiceInstances(idx, v)
            }.getOrElse(IndexedServiceInstances(idx, Set.empty[ServiceInstance]))
            case Left(error: MalformedContent) ⇒
              throw new PipelineException(error.errorMessage, error.cause.orNull)
            case Left(error) ⇒ throw new PipelineException(error.toString)
          }
        }
      else throw new UnsuccessfulResponseException(response)

  // TODO: Check if there is a more reliable library offering these type of retries
  def findServiceChange(service: String, index: Option[Long], wait: Option[String], dataCenter: Option[String] = None): Future[IndexedServiceInstances] = {
    val dcParameter = dataCenter.map(dc => s"dc=$dc")
    val waitParameter = wait.map(w => s"wait=$w")
    val indexParameter = index.map(i => s"index=$i")
    val parameters = Seq(dcParameter, waitParameter, indexParameter).flatten.mkString("&")
    val request = Get(s"$host/v1/catalog/service/$service?$parameters")
    val myPipeline: HttpRequest => Future[IndexedServiceInstances] = pipeline ~> unmarshalWithIndex
    val success = Success[IndexedServiceInstances](r => true)
    retry { () =>
      myPipeline(request)
    }(success, executionContext)
  }

  override def registerService(registration: ServiceRegistration): Future[Unit] = {
    val request = Put(s"$host/v1/agent/service/register", registration.toJson.asJsObject())
    val myPipeline: HttpRequest => Future[HttpResponse] = pipeline
    val success = Success[HttpResponse](r => r.status.isSuccess)
    retry { () =>
      myPipeline(request)
    }(success, executionContext).map(r => Unit)
  }
}
