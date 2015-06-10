package xebia.consul.example

import java.net.URL

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http.{HttpResponse, HttpRequest}

import scala.concurrent.Future

class SprayExampleServiceClient(host: URL)(implicit actorSystem: ActorSystem) {

  implicit val executionContext = actorSystem.dispatcher

  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
  def stringFromResponse: HttpResponse => String = (response) => response.entity.asString

  def identify: Future[String] = {
    val myPipeline: HttpRequest => Future[String] = pipeline ~> stringFromResponse
    myPipeline(Get(s"$host/api/identify"))
  }
}
