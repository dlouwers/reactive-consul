package xebia.consul.client.loadbalancers

import xebia.consul.client.{ ConnectionHolder, ConnectionProvider, LoadBalancer }

class BaseLoadbalancer extends LoadBalancer {

  override def getConnection: ConnectionHolder = ???

  override def returnConnection(connection: ConnectionHolder): Unit = ???

  override def removeConnectionProvider(key: String): Unit = ???

  override def addConnectionProvider(key: String, provider: ConnectionProvider): Unit = ???

}
