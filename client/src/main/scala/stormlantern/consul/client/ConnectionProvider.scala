package stormlantern.consul.client

import akka.actor.ActorRef

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ConnectionProvider {
  def getConnection: Future[Any]
  def returnConnection(connectionHolder: ConnectionHolder): Unit = ()
  def destroy(): Unit = ()
  def getConnectionHolder(k: String, lb: ActorRef): Future[ConnectionHolder] = getConnection.map { connection =>
    new ConnectionHolder {
      override def connection: Future[Any] = getConnection
      override val loadBalancer: ActorRef = lb
      override val key: String = k
    }
  }
}

trait ConnectionProviderFactory {
  def create(host: String, port: Int): ConnectionProvider
}
