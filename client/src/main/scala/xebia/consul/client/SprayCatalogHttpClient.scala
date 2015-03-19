package xebia.consul.client

import java.net.URL

import akka.actor.ActorSystem
import retry.Success
import spray.client.pipelining._
import spray.http.{ HttpRequest, HttpResponse }
import spray.httpx.{ UnsuccessfulResponseException, PipelineException }
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.json.DefaultJsonProtocol
import xebia.consul.client.util.{ Logging, RetryPolicy }

import scala.concurrent.Future

class SprayCatalogHttpClient(host: URL)(implicit actorSystem: ActorSystem) extends CatalogHttpClient with DefaultJsonProtocol with RetryPolicy with Logging {

  implicit val executionContext = actorSystem.dispatcher
  implicit val serviceFormat = jsonFormat(Service, "Node", "Address", "ServiceID", "ServiceName", "ServiceTags", "ServiceAddress", "ServicePort")

  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  def unmarshalWithIndex: HttpResponse ⇒ IndexedServices =
    response ⇒
      if (response.status.isSuccess)
        response.as[Seq[Service]] match {
          case Right(value) ⇒ response.headers.find(h => h.name == "X-Consul-Index").map { idx =>
            IndexedServices(idx.value, value)
          }.getOrElse(throw new PipelineException("X-Consul-Index header not found"))
          case Left(error: MalformedContent) ⇒
            throw new PipelineException(error.errorMessage, error.cause.orNull)
          case Left(error) ⇒ throw new PipelineException(error.toString)
        }
      else throw new UnsuccessfulResponseException(response)

  // TODO: Check if there is a more reliable library offering these type of retries
  // TODO: Change implementation to read and return the X-Consul-Index value in the return value
  // TODO: Add an optional consulIndex parameter to send as the X-Consul_index header to watch for changes
  def findServiceChange(service: String, dataCenter: Option[String] = None): Future[IndexedServices] = {
    val parameters = dataCenter.map(dc => s"dc=$dc").getOrElse("")
    val request = Get(s"$host/v1/catalog/service/$service?$parameters")
    val myPipeline: HttpRequest => Future[IndexedServices] = pipeline ~> unmarshalWithIndex
    implicit val success = Success[Any](r => true)
    retry { () =>
      myPipeline(request)
    }
  }
}
