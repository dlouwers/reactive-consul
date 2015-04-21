package xebia.consul.client

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ConnectionProviderFactory {
  def create[T](host: String, port: Int): ConnectionProvider
}

trait ConnectionProvider {
  def getConnection: Future[ConnectionHolder]
  def returnConnection(connection: ConnectionHolder): Unit
  def destroy(): Unit
}

abstract class ConnectionHolder(connectionProvider: ConnectionProvider) {
  def connection[A]: Future[A]
  def withConnection[A, B](f: A => Future[B]): Future[B] = try {
    connection.flatMap((c: A) => f(c))
  } finally {
    connectionProvider.returnConnection(this)
  }
}

trait LoadBalancer {
  def getConnection: ConnectionHolder
  def returnConnection(connection: ConnectionHolder): Unit
  def addConnectionProvider(key: String, provider: ConnectionProvider): Unit
  def removeConnectionProvider(key: String): Unit
}

case class ConnectionStrategy(factory: ConnectionProviderFactory, loadbalancer: LoadBalancer)
