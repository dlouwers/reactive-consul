package nl.webresource.gin

import akka.actor.{Actor, Props}
import spray.routing._

import scala.concurrent.ExecutionContext

class HttpServiceActor extends Actor with HttpService {

  def actorRefFactory = context

  def receive = runRoute(reactiveConsulRoute)
}

object HttpServiceActor {
  def props(dataGatheringService: ScraperService) = Props(classOf[HttpServiceActor], dataGatheringService)
}

trait HttpService extends HttpService {
  implicit def executionContext: ExecutionContext = actorRefFactory.dispatcher

  val reactiveConsulRoute =
    path("api" / "ping") {
      get { "PONG" }
    }
}