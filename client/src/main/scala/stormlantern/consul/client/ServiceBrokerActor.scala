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
  val indexedServices = services.map(s ⇒ (s.serviceDefinition.serviceId, s)).toMap
  val loadbalancers: mutable.Map[String, ActorRef] = mutable.Map.empty
  val serviceAvailability: mutable.Set[ActorRef] = mutable.Set.empty
  val sessionId: Option[UUID] = None

  override def preStart(): Unit = {
    indexedServices.foreach {
      case (name, strategy) ⇒
        loadbalancers.put(name, strategy.loadBalancerFactory(context))
        log.info(s"Starting service availability Actor for $name")
        val serviceAvailabilityActorRef = serviceAvailabilityActorFactory(context, strategy.serviceDefinition, self)
        serviceAvailabilityActorRef ! Start
        serviceAvailability += serviceAvailabilityActorRef
    }
  }

  def receive = {
    case ServiceAvailabilityUpdate(added, removed) ⇒
      log.debug(s"Adding connection providers for $added")
      addConnectionProviders(added)
      log.debug(s"Removing conection providers for $removed")
      removeConnectionProviders(removed)
    case GetServiceConnection(name: String) ⇒
      log.debug(s"Getting a service connection for $name")
      loadbalancers.get(name) match {
        case Some(loadbalancer) ⇒
          loadbalancer forward GetConnection
        case None ⇒
          sender ! Failure(new ServiceUnavailableException(name))
      }
    case HasAvailableConnectionProviderFor(name: String) ⇒
      loadbalancers.get(name) match {
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
  def addConnectionProviders(added: Set[ServiceInstance]): Unit = {
    added.foreach { s ⇒
      val host = if (s.serviceAddress.isEmpty) s.address else s.serviceAddress
      val connectionProvider = indexedServices(s.serviceName).connectionProviderFactory.create(host, s.servicePort)
      loadbalancers(s.serviceName) ! LoadBalancerActor.AddConnectionProvider(s.serviceId, connectionProvider)
    }
  }

  def removeConnectionProviders(removed: Set[ServiceInstance]): Unit = {
    removed.foreach { s ⇒
      loadbalancers(s.serviceName) ! LoadBalancerActor.RemoveConnectionProvider(s.serviceId)
    }
  }

  def queryConnectionProviderAvailability: Future[Boolean] = {
    implicit val timeout = Timeout(1.second)
    import akka.pattern.ask
    Future.sequence(loadbalancers.values.map(_.ask(LoadBalancerActor.HasAvailableConnectionProvider).mapTo[Boolean])).map(_.forall(p ⇒ p))
  }
}

object ServiceBrokerActor {
  // Constructors
  def props(services: Set[ConnectionStrategy], serviceAvailabilityActorFactory: (ActorRefFactory, ServiceDefinition, ActorRef) ⇒ ActorRef)(implicit ec: ExecutionContext): Props = Props(new ServiceBrokerActor(services, serviceAvailabilityActorFactory))
  // Public messages
  case class GetServiceConnection(name: String)
  case object Stop
  case class HasAvailableConnectionProviderFor(name: String)
  case object AllConnectionProvidersAvailable
  case class JoinElection(key: String)
}
