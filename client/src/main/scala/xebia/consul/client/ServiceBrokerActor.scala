package xebia.consul.client

import akka.actor.{ Actor, Props }
import akka.event.Logging
import xebia.consul.client.ServiceAvailabilityActor._
import xebia.consul.client.ServiceBrokerActor.{ ReturnServiceConnection, GetServiceConnection }

import scala.collection.mutable

class ServiceBrokerActor(services: Map[String, ConnectionStrategy], httpClient: CatalogHttpClient) extends Actor with ActorSupport {

  val log = Logging(context.system, this)

  // Actor state
  val loadBalancers: mutable.Map[String, LoadBalancer] = mutable.Map.empty

  override def preStart(): Unit = {
    services.foreach {
      case (name, strategy) =>
        loadBalancers.put(name, strategy.loadbalancer)
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
      sender ! getServiceConnection(name: String)
    case ReturnServiceConnection(name: String, connection: Any) =>
      log.debug(s"Returning service connection for $name")

  }

  def getServiceConnection(name: String): Any = {
    loadBalancers(name).getConnection
  }

  def returnServiceConnection(name: String, connection: Any): Unit = {
    loadBalancers(name).returnConnection(connection)
  }

  def addConnectionProviders(added: Set[Service]): Unit = {
    added.foreach { s =>
      val connectionProvider = services(s.serviceName).factory.create(s.serviceAddress, s.servicePort)
      loadBalancers(s.serviceName).addConnectionProvider(s.serviceId, connectionProvider)
    }
  }

  def removeConnectionProviders(removed: Set[Service]): Unit = {
    removed.foreach { s =>
      loadBalancers(s.serviceName).removeConnectionProvider(s.serviceId)
    }
  }

}

object ServiceBrokerActor {
  def props(services: Map[String, ConnectionStrategy], httpClient: CatalogHttpClient): Props = Props(new ServiceBrokerActor(services, httpClient))
  case class GetServiceConnection(name: String)
  case class ReturnServiceConnection[T](name: String, connection: T)
}
