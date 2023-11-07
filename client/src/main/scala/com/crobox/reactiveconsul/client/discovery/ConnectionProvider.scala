package com.crobox.reactiveconsul.client.discovery

import org.apache.pekko.actor.ActorRef

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ConnectionProvider {
  def getConnection: Future[Any]
  def returnConnection(connectionHolder: ConnectionHolder): Unit = ()
  def destroy(): Unit = ()
  def getConnectionHolder(i: String, lb: ActorRef): Future[ConnectionHolder] = getConnection.map { connection =>
    new ConnectionHolder {
      override def connection: Future[Any] = getConnection
      override val loadBalancer: ActorRef = lb
      override val id: String = i
    }
  }
}
