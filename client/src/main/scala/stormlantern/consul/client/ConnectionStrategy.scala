package stormlantern.consul.client

import akka.actor.{ ActorRef, ActorRefFactory }
import stormlantern.consul.client.loadbalancers.{ LoadBalancer, RoundRobinLoadBalancer, LoadBalancerActor }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ConnectionProviderFactory {
  def create(host: String, port: Int): ConnectionProvider
}

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

trait ConnectionHolder {
  val key: String
  val loadBalancer: ActorRef
  def connection: Future[Any]
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
