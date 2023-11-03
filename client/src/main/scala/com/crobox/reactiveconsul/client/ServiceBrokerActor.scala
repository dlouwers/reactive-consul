package com.crobox.reactiveconsul.client

import com.crobox.reactiveconsul.client.discovery.ServiceAvailabilityActor.{Start, Started}
import com.crobox.reactiveconsul.client.discovery.{ConnectionStrategy, ServiceDefinition}
import com.crobox.reactiveconsul.client.loadbalancers.LoadBalancerActor
import com.crobox.reactiveconsul.client.loadbalancers.LoadBalancerActor.{GetConnection, HasAvailableConnectionProvider}
import org.apache.pekko.actor.Status.Failure
import org.apache.pekko.actor._
import org.apache.pekko.util.Timeout
import ServiceBrokerActor._
import com.crobox.reactiveconsul.client.discovery.ServiceAvailabilityActor._
import LoadBalancerActor.{GetConnection, HasAvailableConnectionProvider}
import com.crobox.reactiveconsul.client.dao.ServiceInstance

import java.util.UUID
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ServiceBrokerActor(
                          services: Set[ConnectionStrategy],
                          serviceAvailabilityActorFactory: (ActorRefFactory, ServiceDefinition, ActorRef, Boolean) => ActorRef)(implicit ec: ExecutionContext)
  extends Actor with ActorLogging with Stash {

  // Actor state
  val indexedServices: Map[String, ConnectionStrategy] = services.map(s => (s.serviceDefinition.key, s)).toMap
  val loadbalancers: mutable.Map[String, ActorRef] = mutable.Map.empty
  val serviceAvailability: mutable.Set[ActorRef] = mutable.Set.empty
  val sessionId: Option[UUID] = None
  var initializationCountdown: Int = services.size

  override def preStart(): Unit = {
    indexedServices.foreach {
      case (key, strategy) =>
        loadbalancers.put(key, strategy.loadBalancerFactory(context))
        log.info(s"Starting service availability Actor for $key")
        val serviceAvailabilityActorRef = serviceAvailabilityActorFactory(context, strategy.serviceDefinition, self, strategy.onlyHealthyServices)
        serviceAvailabilityActorRef ! Start
        serviceAvailability += serviceAvailabilityActorRef
    }
  }

  def receive: Receive = {
    case Started =>
      log.debug(s"Service availability initialized for ${sender()}")
      initializationCountdown -= 1
      if (initializationCountdown == 0) {
        unstashAll()
      }
    case ServiceAvailabilityUpdate(key, added, removed) =>
      log.debug(s"Adding connection providers for $key: $added")
      addConnectionProviders(key, added)
      log.debug(s"Removing conection providers for $key: $removed")
      removeConnectionProviders(key, removed)
    case GetServiceConnection(key: String) =>
      if (initializationCountdown != 0) {
        stash()
      } else {
        log.debug(s"Getting a service connection for $key")
        loadbalancers.get(key) match {
          case Some(loadbalancer) => loadbalancer forward GetConnection
          case None => sender() ! Failure(ServiceUnavailableException(key))
        }
      }
    case HasAvailableConnectionProviderFor(key: String) =>
      loadbalancers.get(key) match {
        case Some(loadbalancer) =>
          loadbalancer forward HasAvailableConnectionProvider
        case None =>
          sender() ! false
      }
    case AllConnectionProvidersAvailable =>
      import org.apache.pekko.pattern.pipe
      queryConnectionProviderAvailability pipeTo sender()
    case JoinElection(key) =>
  }

  // Internal methods
  def addConnectionProviders(key: String, added: Set[ServiceInstance]): Unit = {
    added.foreach { s =>
      val host = if (s.serviceAddress.isEmpty) s.address else s.serviceAddress
      val connectionProvider = indexedServices(key).connectionProviderFactory.create(host, s.servicePort)
      loadbalancers(key) ! LoadBalancerActor.AddConnectionProvider(s.serviceId, connectionProvider)
    }
  }

  def removeConnectionProviders(key: String, removed: Set[ServiceInstance]): Unit = {
    removed.foreach { s =>
      loadbalancers(key) ! LoadBalancerActor.RemoveConnectionProvider(s.serviceId)
    }
  }

  def queryConnectionProviderAvailability: Future[Boolean] = {
    implicit val timeout: Timeout = 1.second
    import org.apache.pekko.pattern.ask
    Future.sequence(loadbalancers.values.map(_.ask(LoadBalancerActor.HasAvailableConnectionProvider).mapTo[Boolean]))
      .map(_.forall(p => p))
  }
}

object ServiceBrokerActor {
  // Constructors
  def props(
             services: Set[ConnectionStrategy],
             serviceAvailabilityActorFactory: (ActorRefFactory, ServiceDefinition, ActorRef, Boolean) => ActorRef)(implicit ec: ExecutionContext): Props =
    Props(new ServiceBrokerActor(services, serviceAvailabilityActorFactory))

  case class GetServiceConnection(key: String)

  case object Stop

  case class HasAvailableConnectionProviderFor(key: String)

  case object AllConnectionProvidersAvailable

  case class JoinElection(key: String)
}
