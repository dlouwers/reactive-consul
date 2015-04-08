package xebia.consul.client.loadbalancers

import xebia.consul.client.{ ConnectionProvider, LoadBalancer }

import scala.concurrent.Future

class BaseLoadbalancer extends LoadBalancer {

  override def getConnection[T]: Future[T] = ???

  override def returnConnection[T](connection: T): Unit = ???

  override def removeConnectionProvider(key: String): Unit = ???

  override def addConnectionProvider(key: String, provider: ConnectionProvider): Unit = ???

}
