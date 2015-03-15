package xebia.consul.client

import java.net.URL

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http.{HttpRequest, HttpResponse}
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

class SprayCatalog(host: URL)(implicit actorSystem: ActorSystem) extends Catalog with DefaultJsonProtocol {

  implicit val executionContext = actorSystem.dispatcher
  implicit val serviceFormat = jsonFormat(Service, "Node", "Address", "ServiceID", "ServiceName", "ServiceTags", "ServiceAddress", "ServicePort")

  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  override def findService(service: String, dataCenter: Option[String]): Future[Seq[Service]] = {
    val request = Get(host.toString)
    val myPipeline: HttpRequest => Future[Seq[Service]] = pipeline ~> unmarshal[Seq[Service]]
    myPipeline(request)
  }
}
