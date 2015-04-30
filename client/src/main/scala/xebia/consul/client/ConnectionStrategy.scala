package xebia.consul.client

import xebia.consul.client.loadbalancers.LoadBalancer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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
  val loadBalancer: LoadBalancer
  def connection[A]: Future[A]
  def withConnection[A, B](f: A => Future[B]): Future[B] = try {
    connection.flatMap((c: A) => f(c))
  } finally {
    loadBalancer.returnConnection(this)
  }
}

case class ConnectionStrategy(factory: ConnectionProviderFactory, loadbalancer: LoadBalancer)
