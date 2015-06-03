package xebia.consul.client

import akka.actor.{ ActorRefFactory, ActorRef }
import akka.util.Timeout
import xebia.consul.client.dao.{ ServiceRegistration, ConsulHttpClient }
import xebia.consul.client.loadbalancers.LoadBalancerActor
import xebia.consul.client.util.{ Logging, RetryPolicy }
import scala.concurrent.duration._

import scala.concurrent.{ ExecutionContext, Future }

class ServiceBroker(serviceBrokerActor: ActorRef, consulClient: ConsulHttpClient)(implicit ec: ExecutionContext) extends RetryPolicy with Logging {

  import akka.pattern.ask

  private[this] implicit val timeout = Timeout(10.seconds)

  def withService[A, B](name: String)(f: A => Future[B]): Future[B] = {
    logger.info(s"Trying to get connection for service $name")
    serviceBrokerActor.ask(ServiceBrokerActor.GetServiceConnection(name)).mapTo[ConnectionHolder].flatMap { connectionHolder =>
      logger.info(s"Received connectionholder $connectionHolder")
      try {
        connectionHolder.connection[A].flatMap(f)
      } finally {
        connectionHolder.loadBalancer ! LoadBalancerActor.ReturnConnection(connectionHolder)
      }
    }
  }

  def registerService(registration: ServiceRegistration): Future[Unit] = consulClient.registerService(registration)
}

object ServiceBroker {
  def apply(rootActor: ActorRefFactory, httpClient: ConsulHttpClient, services: Map[String, ConnectionStrategy]): ServiceBroker = {
    implicit val ec = rootActor.dispatcher
    val serviceAvailabilityActorFactory = (factory: ActorRefFactory, service: String, listener: ActorRef) => factory.actorOf(ServiceAvailabilityActor.props(httpClient, service, listener))
    val actorRef = rootActor.actorOf(ServiceBrokerActor.props(services, serviceAvailabilityActorFactory), "ServiceBroker")
    new ServiceBroker(actorRef, httpClient)
  }
}

case class ServiceUnavailableException(service: String) extends RuntimeException(s"$service service unavailable")