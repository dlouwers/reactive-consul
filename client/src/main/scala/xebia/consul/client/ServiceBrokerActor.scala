package xebia.consul.client

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import xebia.consul.client.ServiceAvailabilityActor._
import xebia.consul.client.ServiceBrokerActor.GetServiceConnection
import xebia.consul.client.loadbalancers.LoadBalancerActor

import scala.collection.mutable
import scala.concurrent.ExecutionContext

class ServiceBrokerActor(services: Map[String, ConnectionStrategy], httpClient: CatalogHttpClient)(implicit ec: ExecutionContext) extends Actor with ActorLogging with ActorSupport {

  // Actor state
  val loadbalancers: mutable.Map[String, ActorRef] = mutable.Map.empty

  override def preStart(): Unit = {
    services.foreach {
      case (name, strategy) =>
        loadbalancers.put(name, strategy.loadbalancer)
        log.debug(s"Starting Service Availability Actor for $name")
        createChild(ServiceAvailabilityActor.props(httpClient, name, self))
    }
  }

  def receive = {
    case ServiceAvailabilityUpdate(added, removed) =>
      log.debug(s"Adding connection providers for $added")
      addConnectionProviders(added)
      log.debug(s"Removing conection providers for $removed")
      removeConnectionProviders(removed)
    case GetServiceConnection(name: String) =>
      log.debug(s"Getting a service connection for $name")
      loadbalancers(name) forward LoadBalancerActor.GetConnection

  }

  def addConnectionProviders(added: Set[Service]): Unit = {
    added.foreach { s =>
      val connectionProvider = services(s.serviceName).factory.create(s.serviceAddress, s.servicePort)
      loadbalancers(s.serviceName) ! LoadBalancerActor.AddConnectionProvider(s.serviceId, connectionProvider)
    }
  }

  def removeConnectionProviders(removed: Set[Service]): Unit = {
    removed.foreach { s =>
      loadbalancers(s.serviceName) ! LoadBalancerActor.RemoveConnectionProvider(s.serviceId)
    }
  }
}

object ServiceBrokerActor {
  def props(services: Map[String, ConnectionStrategy], httpClient: CatalogHttpClient)(implicit ec: ExecutionContext): Props = Props(new ServiceBrokerActor(services, httpClient))
  case class GetServiceConnection(name: String)
}
