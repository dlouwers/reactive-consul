package xebia.consul.client

import scala.concurrent.Future

trait ConnectionProviderFactory {
  def create(host: String, port: Int): ConnectionProvider
}

trait ConnectionProvider {
  def getConnection[T]: Future[T]
  def returnConnection[T](connection: T): Unit
  def destroy(): Unit
}

trait LoadBalancer {
  def getConnection[T]: Future[T]
  def addConnectionProvider(key: String, provider: ConnectionProvider): Unit
  def removeConnectionProvider(key: String): Unit
}

case class ConnectionStrategy(factory: ConnectionProviderFactory, loadbalancer: LoadBalancer)
