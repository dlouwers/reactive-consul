package stormlantern.consul.client

import akka.actor.{ ActorRef, ActorRefFactory }
import stormlantern.consul.client.loadbalancers.{ LoadBalancer, RoundRobinLoadBalancer, LoadBalancerActor }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class ServiceDefinition(serviceId: String, serviceName: String, serviceTags: Set[String] = Set.empty, dataCenter: Option[String] = None)
object ServiceDefinition {
  def apply(serviceName: String): ServiceDefinition = {
    ServiceDefinition(serviceName, serviceName)
  }
}

case class ConnectionStrategy(
  serviceDefinition: ServiceDefinition,
  connectionProviderFactory: ConnectionProviderFactory,
  loadBalancerFactory: ActorRefFactory => ActorRef)

object ConnectionStrategy {

  def apply(serviceDefinition: String, connectionProviderFactory: ConnectionProviderFactory, loadBalancer: LoadBalancer): ConnectionStrategy = {
    ConnectionStrategy(serviceDefinition, connectionProviderFactory, loadBalancer)
  }

  def apply(serviceDefinition: ServiceDefinition, connectionProviderFactory: ConnectionProviderFactory, loadBalancer: LoadBalancer): ConnectionStrategy =
    ConnectionStrategy(serviceDefinition, connectionProviderFactory, ctx => ctx.actorOf(LoadBalancerActor.props(loadBalancer, serviceDefinition.serviceId)))

  def apply(serviceDefinition: ServiceDefinition, connectionProviderFactory: (String, Int) => ConnectionProvider, loadBalancer: LoadBalancer): ConnectionStrategy = {
    val cpf = new ConnectionProviderFactory {
      override def create(host: String, port: Int): ConnectionProvider = connectionProviderFactory(host, port)
    }
    ConnectionStrategy(serviceDefinition, cpf, ctx => ctx.actorOf(LoadBalancerActor.props(loadBalancer, serviceDefinition.serviceId)))
  }

  def apply(serviceName: String, connectionProviderFactory: (String, Int) => ConnectionProvider, loadBalancer: LoadBalancer): ConnectionStrategy = {
    ConnectionStrategy(ServiceDefinition(serviceName), connectionProviderFactory, loadBalancer)
  }

  def apply(serviceName: String, connectionProviderFactory: (String, Int) => ConnectionProvider): ConnectionStrategy = {
    ConnectionStrategy(serviceName, connectionProviderFactory, new RoundRobinLoadBalancer)
  }

}
