package xebia.consul.client

import akka.actor.{ ActorRefFactory, ActorRef }
import xebia.consul.client.loadbalancers.LoadBalancerActor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ConnectionProviderFactory {
  def create(host: String, port: Int): ConnectionProvider
}

trait ConnectionProvider {
  def getConnection: Future[ConnectionHolder]
  def returnConnection(connection: ConnectionHolder): Unit
  def destroy(): Unit
}

trait ConnectionHolder {
  val key: String
  val loadBalancer: ActorRef
  def connection[A]: Future[A]
  def withConnection[A, B](f: A => Future[B]): Future[B] = try {
    connection.flatMap((c: A) => f(c))
  } finally {
    loadBalancer ! LoadBalancerActor.ReturnConnection(this)
  }
}

case class ConnectionStrategy(connectionProviderFactory: ConnectionProviderFactory, loadBalancerFactory: ActorRefFactory => ActorRef)
