package xebia.consul.client

import scala.concurrent.Future

trait ConnectionProviderFactory {
  def create[T](host: String, port: Int): ConnectionProvider
}

trait ConnectionProvider {
  def getConnection: Future[ConnectionHolder]
  def returnConnection(connection: ConnectionHolder): Unit
  def destroy(): Unit
}

case class ConnectionHolder() {
  def withConnection[A, B](f: A => Future[B]): Future[B] = try {
    ???
  }
}

trait LoadBalancer {
  def getConnection: ConnectionHolder
  def returnConnection(connection: ConnectionHolder): Unit
  def addConnectionProvider(key: String, provider: ConnectionProvider): Unit
  def removeConnectionProvider(key: String): Unit
}

case class ConnectionStrategy(factory: ConnectionProviderFactory, loadbalancer: LoadBalancer)
