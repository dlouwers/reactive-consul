package xebia.consul.client

import java.net.URL

import akka.actor.{ ActorSystem, ActorRefFactory, ActorRef }
import akka.util.Timeout
import xebia.consul.client.dao.{ SprayConsulHttpClient, ServiceRegistration, ConsulHttpClient }
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

  def apply(services: Map[String, ConnectionStrategy], host: String, port: Int = 8500): ServiceBroker = {
    implicit val ec = ExecutionContext.Implicits.global
    implicit val rootActor = ActorSystem("reactive-consul")
    val httpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
    val serviceAvailabilityActorFactory = (factory: ActorRefFactory, service: String, listener: ActorRef) => factory.actorOf(ServiceAvailabilityActor.props(httpClient, service, listener))
    val actorRef = rootActor.actorOf(ServiceBrokerActor.props(services, serviceAvailabilityActorFactory), "ServiceBroker")
    new ServiceBroker(actorRef, httpClient)
  }
}

case class ServiceUnavailableException(service: String) extends RuntimeException(s"$service service unavailable")