package xebia.reactiveconsul

import scala.concurrent.duration._

object Boot extends App {
  implicit val system = ActorSystem("xebia-reactive-consul")
  implicit val executionContext = system.dispatcher

  val service = system.actorOf(GiniusHttpServiceActor, "webservice")

  implicit val timeout = Timeout(5 seconds)

  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)
}
