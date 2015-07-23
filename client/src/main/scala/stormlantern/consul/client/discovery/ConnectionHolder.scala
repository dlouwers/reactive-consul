package stormlantern.consul.client.discovery

import akka.actor.ActorRef

import scala.concurrent.Future

trait ConnectionHolder {
  val key: String
  val loadBalancer: ActorRef
  def connection: Future[Any]
}
