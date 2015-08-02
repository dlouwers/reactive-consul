package stormlantern.consul.client.dao

import java.net.URL
import java.util.UUID

import akka.actor.ActorSystem
import retry.Success
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.httpx.{ PipelineException, UnsuccessfulResponseException }
import spray.json._
import stormlantern.consul.client.util.{ RetryPolicy, Logging }

import scala.concurrent.Future
import scala.util.Try

class SprayConsulHttpClient(host: URL)(implicit actorSystem: ActorSystem) extends ConsulHttpClient
    with ConsulHttpProtocol with RetryPolicy with Logging {

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

  def findServiceChange(
    service: String,
    tag: Option[String] = None,
    index: Option[Long] = None,
    wait: Option[String] = None,
    dataCenter: Option[String] = None): Future[IndexedServiceInstances] = {
    val dcParameter = dataCenter.map(dc => s"dc=$dc")
    val waitParameter = wait.map(w => s"wait=$w")
    val indexParameter = index.map(i => s"index=$i")
    val tagParameter = tag.map(t => s"tag=$t")
    val parameters = Seq(dcParameter, tagParameter, waitParameter, indexParameter).flatten.mkString("&")
    val request = Get(s"$host/v1/catalog/service/$service?$parameters")
    val myPipeline: HttpRequest => Future[IndexedServiceInstances] = pipeline ~> unmarshalWithIndex
    implicit val success = Success[IndexedServiceInstances](r => true)
    retry { () =>
      myPipeline(request)
    }
  }

  override def registerService(registration: ServiceRegistration): Future[String] = {
    val request = Put(s"$host/v1/agent/service/register", registration.toJson.asJsObject())
    val myPipeline: HttpRequest => Future[HttpResponse] = pipeline
    implicit val success = Success[HttpResponse](r => r.status.isSuccess)
    retry { () =>
      myPipeline(request)
    }.map(r => registration.id.getOrElse(registration.name))
  }

  override def deregisterService(serviceId: String): Future[Unit] = {
    val request = Delete(s"$host/v1/agent/service/deregister/$serviceId")
    val myPipeline: HttpRequest => Future[HttpResponse] = pipeline
    implicit val success = Success[HttpResponse](r => r.status.isSuccess)
    retry { () =>
      myPipeline(request)
    }.map(r => ())
  }

  override def createSession(sessionCreation: Option[SessionCreation] = None, dataCenter: Option[String] = None): Future[UUID] = {
    val dcParameter = dataCenter.map(dc => s"dc=$dc")
    val request = Put(s"$host/v1/session/create", sessionCreation.map(_.toJson.asJsObject))
    val myPipeline: HttpRequest => Future[HttpResponse] = pipeline
    implicit val success = Success[HttpResponse](r => r.status.isSuccess)
    retry { () =>
      myPipeline(request)
    }.map(r => r.entity.asString.parseJson.asJsObject.fields("ID").convertTo[UUID])
  }

  override def putKeyValuePair(key: String, value: Array[Byte], sessionOp: Option[SessionOp] = None): Future[Boolean] = {
    val opParameter = sessionOp.map {
      case AcquireSession(id) => s"acquire=$id"
      case ReleaseSession(id) => s"release=$id"
    }
    val parameters = Seq(opParameter).flatten.mkString("&")
    val request = Put(s"$host/v1/kv/$key?$parameters", HttpEntity(value))
    val myPipeline: HttpRequest => Future[HttpResponse] = pipeline
    implicit val success = Success[HttpResponse] { r => true }
    retry { () =>
      myPipeline(request)
    }.map { r =>
      if (r.status.isSuccess) {
        r.entity.asString.toBoolean
      } else if (r.status == StatusCodes.InternalServerError && r.entity.asString == "Invalid session") {
        false
      } else {
        throw new IllegalArgumentException(r.entity.asString)
      }
    }
  }

  override def readKeyValue(
    key: String,
    index: Option[Long] = None,
    wait: Option[String] = None,
    recurse: Boolean = false,
    keysOnly: Boolean = false): Future[Seq[KeyData]] = {
    val waitParameter = wait.map(p => s"wait=$p")
    val indexParameter = index.map(p => s"index=$p")
    val recurseParameter = if (recurse) Some("recurse") else None
    val keysOnlyParameter = if (keysOnly) Some("keys") else None
    val parameters = Seq(indexParameter, waitParameter, recurseParameter, keysOnlyParameter).flatten.mkString("&")
    val request = Get(s"$host/v1/kv/$key?$parameters")
    val myPipeline: HttpRequest => Future[Seq[KeyData]] = pipeline ~> unmarshal[Seq[KeyData]]
    implicit val success = Success[Seq[KeyData]](r => true)
    retry { () =>
      myPipeline(request)
    }
  }
}
