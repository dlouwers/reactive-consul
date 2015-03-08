package xebia.reactiveconsul

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import nl.webresource.gin.HttpServiceActor
import spray.can.Http

import scala.concurrent.duration._

object Boot extends App {
  implicit val system = ActorSystem("xebia-reactive-consul")
  implicit val executionContext = system.dispatcher

  val service = system.actorOf(HttpServiceActor.props(), "webservice")

  implicit val timeout = Timeout(5 seconds)

  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)
}
