package nl.webresource.gin

import akka.actor.{Actor, Props}

import scala.concurrent.ExecutionContext
import spray.routing._

class HttpServiceActor extends Actor with HttpService {

  def actorRefFactory = context

  def receive = runRoute(reactiveConsulRoute)
}

object HttpServiceActor {
  def props() = Props(classOf[HttpServiceActor])
}

trait HttpService extends HttpService {
  implicit def executionContext: ExecutionContext = actorRefFactory.dispatcher

  val reactiveConsulRoute =
    path("api" / "ping") {
      get { "PONG" }
    }
}