package xebia.consul.client.loadbalancers

import xebia.consul.client.{ ConnectionProvider, ConnectionHolder }

import scala.concurrent.Future

trait LoadBalancer {
  def getConnection: Future[ConnectionHolder]
  def returnConnection(connection: ConnectionHolder): Unit
  def addConnectionProvider(key: String, provider: ConnectionProvider): Unit
  def removeConnectionProvider(key: String): Unit
}
