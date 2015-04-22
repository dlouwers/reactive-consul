package xebia.consul.client

import akka.actor.{ Actor, Props }
import xebia.consul.client.ServiceAvailabilityActor._
import xebia.consul.client.ServiceBrokerActor.{ GetServiceConnection, ReturnServiceConnection }

import scala.collection.mutable

class ServiceBrokerActor(services: Map[String, ConnectionStrategy], httpClient: CatalogHttpClient) extends Actor with ActorSupport {

  // Actor state
  val loadbalancers: mutable.Map[String, LoadBalancer] = mutable.Map.empty

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
      sender ! getServiceConnection(name: String)
    case ReturnServiceConnection(name: String, connection: Any) =>
      log.debug(s"Returning service connection for $name")

  }

  def getServiceConnection(name: String): Any = {
    loadbalancers(name).getConnection
  }

  def returnServiceConnection(name: String, connection: ConnectionHolder): Unit = {
    loadbalancers(name).returnConnection(connection)
  }

  def addConnectionProviders(added: Set[Service]): Unit = {
    added.foreach { s =>
      val connectionProvider = services(s.serviceName).factory.create(s.serviceAddress, s.servicePort)
      loadbalancers(s.serviceName).addConnectionProvider(s.serviceId, connectionProvider)
    }
  }

  def removeConnectionProviders(removed: Set[Service]): Unit = {
    removed.foreach { s =>
      loadbalancers(s.serviceName).removeConnectionProvider(s.serviceId)
    }
  }
}

object ServiceBrokerActor {
  def props(services: Map[String, ConnectionStrategy], httpClient: CatalogHttpClient): Props = Props(new ServiceBrokerActor(services, httpClient))
  case class GetServiceConnection(name: String)
  case class ReturnServiceConnection[T](name: String, connection: T)
}
