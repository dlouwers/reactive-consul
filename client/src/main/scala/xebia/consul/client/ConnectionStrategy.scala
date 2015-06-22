package xebia.consul.client

import akka.actor.{ ActorRef, ActorRefFactory }
import xebia.consul.client.loadbalancers.{ RoundRobinLoadBalancer, LoadBalancerActor, LoadBalancer }

import scala.concurrent.Future

trait ConnectionProviderFactory {
  def create(host: String, port: Int): ConnectionProvider
}

trait ConnectionProvider {
  def getConnection(loadBalancer: ActorRef): Future[ConnectionHolder]
  def returnConnection(connection: ConnectionHolder): Unit
  def destroy(): Unit
}

trait ConnectionHolder {
  val key: String
  val loadBalancer: ActorRef
  def connection[A]: Future[A]
}

case class ConnectionStrategy(serviceName: String, connectionProviderFactory: ConnectionProviderFactory, loadBalancerFactory: ActorRefFactory => ActorRef)
object ConnectionStrategy {

  def apply(serviceName: String, connectionProviderFactory: ConnectionProviderFactory, loadBalancer: LoadBalancer): ConnectionStrategy =
    ConnectionStrategy(serviceName, connectionProviderFactory, ctx => ctx.actorOf(LoadBalancerActor.props(loadBalancer, serviceName)))

  def apply(serviceName: String, connectionProviderFactory: (String, Int) => ConnectionProvider, loadBalancer: LoadBalancer = new RoundRobinLoadBalancer): ConnectionStrategy = {
    val cpf = new ConnectionProviderFactory {
      override def create(host: String, port: Int): ConnectionProvider = connectionProviderFactory(host, port)
    }
    ConnectionStrategy(serviceName, cpf, ctx => ctx.actorOf(LoadBalancerActor.props(loadBalancer, serviceName)))
  }

}
