package xebia.consul.example

import akka.actor.ActorSystem
import akka.io.IO
import akka.util.Timeout
import akka.pattern._
import spray.can.Http

import scala.concurrent.duration._

object Boot extends App {
  implicit val system = ActorSystem("xebia-reactive-consul")
  implicit val executionContext = system.dispatcher

  val service = system.actorOf(ReactiveConsulHttpServiceActor.props(), "webservice")

  implicit val timeout = Timeout(5.seconds)

  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
}
