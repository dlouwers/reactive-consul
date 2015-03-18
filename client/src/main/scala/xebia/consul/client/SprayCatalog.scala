package xebia.consul.client

import java.net.URL

import akka.actor.ActorSystem
import retry.Success
import spray.client.pipelining._
import spray.http.{ HttpRequest, HttpResponse }
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import xebia.consul.client.util.{ Logging, RetryPolicy }

import scala.concurrent.Future

class SprayCatalog(host: URL)(implicit actorSystem: ActorSystem) extends Catalog with DefaultJsonProtocol with RetryPolicy with Logging {

  implicit val executionContext = actorSystem.dispatcher
  implicit val serviceFormat = jsonFormat(Service, "Node", "Address", "ServiceID", "ServiceName", "ServiceTags", "ServiceAddress", "ServicePort")

  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  // TODO: Check if there is a more reliable library offering these type of retries
  override def findService(service: String, dataCenter: Option[String]): Future[Seq[Service]] = {
    logger.info(s"Connecting to $host/catalog/service/$service")
    val request = Get(s"$host/v1/catalog/service/$service")
    val myPipeline: HttpRequest => Future[Seq[Service]] = pipeline ~> unmarshal[Seq[Service]]
    implicit val success = Success[Any](r => true)
    retry { () =>
      myPipeline(request)
    }
  }
}
