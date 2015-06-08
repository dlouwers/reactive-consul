package xebia.consul.client.loadbalancers

import xebia.consul.client.{ ConnectionProvider, ConnectionHolder }

import scala.collection.mutable
import scala.concurrent.Future

trait LoadBalancer {

  def selectConnection: Option[String]
  def connectionReturned(key: String): Unit = ()
  def connectionProviderAdded(key: String): Unit = ()
  def connectionProviderRemoved(key: String): Unit = ()
}
