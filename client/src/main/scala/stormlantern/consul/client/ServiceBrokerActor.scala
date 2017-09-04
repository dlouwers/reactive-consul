package stormlantern.consul.client

import java.util.UUID

import akka.actor.Status.Failure
import akka.actor._
import akka.util.Timeout
import stormlantern.consul.client.dao.ServiceInstance
import stormlantern.consul.client.discovery.{ ServiceAvailabilityActor, ServiceDefinition, ConnectionStrategy }
import stormlantern.consul.client.loadbalancers.LoadBalancerActor
import stormlantern.consul.client.loadbalancers.LoadBalancerActor.{ HasAvailableConnectionProvider, GetConnection }
import ServiceAvailabilityActor._
import stormlantern.consul.client.ServiceBrokerActor.{ JoinElection, AllConnectionProvidersAvailable, HasAvailableConnectionProviderFor, GetServiceConnection }

import scala.collection.mutable
import scala.concurrent.{ Future, ExecutionContext }
import scala.concurrent.duration._

class ServiceBrokerActor(services: Set[ConnectionStrategy], serviceAvailabilityActorFactory: (ActorRefFactory, ServiceDefinition, ActorRef) ⇒ ActorRef)(implicit ec: ExecutionContext) extends Actor with ActorLogging {

  // Actor state
  val indexedServices: Map[String, ConnectionStrategy] = services.map(s ⇒ (s.serviceDefinition.key, s)).toMap
  val loadbalancers: mutable.Map[String, ActorRef] = mutable.Map.empty
  val serviceAvailability: mutable.Set[ActorRef] = mutable.Set.empty
  val sessionId: Option[UUID] = None

  override def preStart(): Unit = {
    indexedServices.foreach {
      case (key, strategy) ⇒
        loadbalancers.put(key, strategy.loadBalancerFactory(context))
        log.info(s"Starting service availability Actor for $key")
        val serviceAvailabilityActorRef = serviceAvailabilityActorFactory(context, strategy.serviceDefinition, self)
        serviceAvailabilityActorRef ! Start
        serviceAvailability += serviceAvailabilityActorRef
    }
  }

  def receive = {
    case ServiceAvailabilityUpdate(key, added, removed) ⇒
      log.debug(s"Adding connection providers for $key: $added")
      addConnectionProviders(key, added)
      log.debug(s"Removing conection providers for $key: $removed")
      removeConnectionProviders(key, removed)
    case GetServiceConnection(key: String) ⇒
      log.debug(s"Getting a service connection for $key")
      loadbalancers.get(key) match {
        case Some(loadbalancer) ⇒
          loadbalancer forward GetConnection
        case None ⇒
          sender ! Failure(new ServiceUnavailableException(key))
      }
    case HasAvailableConnectionProviderFor(key: String) ⇒
      loadbalancers.get(key) match {
        case Some(loadbalancer) ⇒
          loadbalancer forward HasAvailableConnectionProvider
        case None ⇒
          sender ! false
      }
    case AllConnectionProvidersAvailable ⇒
      import akka.pattern.pipe
      queryConnectionProviderAvailability pipeTo sender
    case JoinElection(key) ⇒

  }

  // Internal methods
  def addConnectionProviders(key: String, added: Set[ServiceInstance]): Unit = {
    added.foreach { s ⇒
      val host = if (s.serviceAddress.isEmpty) s.address else s.serviceAddress
      val connectionProvider = indexedServices(key).connectionProviderFactory.create(host, s.servicePort)
      loadbalancers(key) ! LoadBalancerActor.AddConnectionProvider(s.serviceId, connectionProvider)
    }
  }

  def removeConnectionProviders(key: String, removed: Set[ServiceInstance]): Unit = {
    removed.foreach { s ⇒
      loadbalancers(key) ! LoadBalancerActor.RemoveConnectionProvider(s.serviceId)
    }
  }

  def queryConnectionProviderAvailability: Future[Boolean] = {
    implicit val timeout: Timeout = 1.second
    import akka.pattern.ask
    Future.sequence(loadbalancers.values.map(_.ask(LoadBalancerActor.HasAvailableConnectionProvider).mapTo[Boolean])).map(_.forall(p ⇒ p))
  }
}

object ServiceBrokerActor {
  // Constructors
  def props(services: Set[ConnectionStrategy], serviceAvailabilityActorFactory: (ActorRefFactory, ServiceDefinition, ActorRef) ⇒ ActorRef)(implicit ec: ExecutionContext): Props = Props(new ServiceBrokerActor(services, serviceAvailabilityActorFactory))
  // Public messages
  case class GetServiceConnection(key: String)
  case object Stop
  case class HasAvailableConnectionProviderFor(key: String)
  case object AllConnectionProvidersAvailable
  case class JoinElection(key: String)
}
