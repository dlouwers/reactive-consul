package xebia.consul.client

import akka.actor.{ ActorRef, ActorRefFactory }

import scala.concurrent.Future

trait ConnectionProviderFactory {
  def create(host: String, port: Int): ConnectionProvider
}

trait ConnectionProvider {
  def getConnection(loadBalancer: ActorRef): Future[ConnectionHolder]
  def returnConnection(connection: ConnectionHolder): Unit
  def destroy(): Unit
}

trait ConnectionHolder {
  val key: String
  val loadBalancer: ActorRef
  def connection[A]: Future[A]
}

case class ConnectionStrategy(connectionProviderFactory: ConnectionProviderFactory, loadBalancerFactory: ActorRefFactory => ActorRef)
