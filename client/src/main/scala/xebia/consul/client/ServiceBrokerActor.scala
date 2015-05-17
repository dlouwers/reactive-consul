package xebia.consul.client

import akka.actor.Status.Failure
import akka.actor._
import akka.util.Timeout
import xebia.consul.client.ServiceAvailabilityActor._
import xebia.consul.client.ServiceBrokerActor.{ AllConnectionProvidersAvailable, HasAvailableConnectionProviderFor, GetServiceConnection }
import xebia.consul.client.loadbalancers.LoadBalancerActor

import scala.collection.mutable
import scala.concurrent.{ Future, ExecutionContext }
import scala.concurrent.duration._

class ServiceBrokerActor(services: Map[String, ConnectionStrategy], serviceAvailabilityActorFactory: (ActorRefFactory, String, ActorRef) => ActorRef)(implicit ec: ExecutionContext) extends Actor with ActorLogging {

  // Actor state
  val loadbalancers: mutable.Map[String, ActorRef] = mutable.Map.empty

  override def preStart(): Unit = {
    services.foreach {
      case (name, strategy) =>
        loadbalancers.put(name, strategy.loadBalancerFactory(context))
        log.info(s"Starting Service Availability Actor for $name")
        serviceAvailabilityActorFactory(context, name, self)
    }
  }

  override def postStop(): Unit = {
    log.info("CRAP I GOT STOPPED")
  }

  def receive = {
    case ServiceAvailabilityUpdate(added, removed) =>
      log.debug(s"Adding connection providers for $added")
      addConnectionProviders(added)
      log.debug(s"Removing conection providers for $removed")
      removeConnectionProviders(removed)
    case GetServiceConnection(name: String) =>
      log.debug(s"Getting a service connection for $name")
      loadbalancers.get(name) match {
        case Some(loadbalancer) =>
          loadbalancer forward LoadBalancerActor.GetConnection(name)
        case None =>
          sender ! Failure(new ServiceUnavailableException(name))
      }
    case HasAvailableConnectionProviderFor(name: String) =>
      loadbalancers.get(name) match {
        case Some(loadbalancer) =>
          loadbalancer forward LoadBalancerActor.HasAvailableConnectionProvider
        case None =>
          sender ! false
      }
    case AllConnectionProvidersAvailable =>
      import akka.pattern.pipe
      queryConnectionProviderAvailability pipeTo sender
  }

  def addConnectionProviders(added: Set[Service]): Unit = {
    added.foreach { s =>
      val host = if (s.serviceAddress.isEmpty) s.address else s.serviceAddress
      val connectionProvider = services(s.serviceName).connectionProviderFactory.create(host, s.servicePort)
      loadbalancers(s.serviceName) ! LoadBalancerActor.AddConnectionProvider(s.serviceId, connectionProvider)
    }
  }

  def removeConnectionProviders(removed: Set[Service]): Unit = {
    removed.foreach { s =>
      loadbalancers(s.serviceName) ! LoadBalancerActor.RemoveConnectionProvider(s.serviceId)
    }
  }

  def queryConnectionProviderAvailability: Future[Boolean] = {
    implicit val timeout = Timeout(1.second)
    import akka.pattern.ask
    Future.sequence(loadbalancers.values.map(_.ask(LoadBalancerActor.HasAvailableConnectionProvider).mapTo[Boolean])).map(_.forall(p => p))
  }
}

object ServiceBrokerActor {
  def props(services: Map[String, ConnectionStrategy], serviceAvailabilityActorFactory: (ActorRefFactory, String, ActorRef) => ActorRef)(implicit ec: ExecutionContext): Props = Props(new ServiceBrokerActor(services, serviceAvailabilityActorFactory))
  case class GetServiceConnection(name: String)
  case object Stop
  case class HasAvailableConnectionProviderFor(name: String)
  case object AllConnectionProvidersAvailable

}
