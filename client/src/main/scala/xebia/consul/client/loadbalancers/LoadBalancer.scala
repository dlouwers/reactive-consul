package xebia.consul.client.loadbalancers

import xebia.consul.client.ConnectionHolder

import scala.concurrent.Future

trait LoadBalancer {
  def selectConnection: Future[Option[ConnectionHolder]]
}
