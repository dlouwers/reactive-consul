package stormlantern.consul.client

import java.net.URL

import scala.concurrent.duration._
import scala.concurrent._

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask

import stormlantern.consul.client.dao._
import stormlantern.consul.client.dao.akka.AkkaHttpConsulClient
import stormlantern.consul.client.discovery._
import stormlantern.consul.client.election.LeaderInfo
import stormlantern.consul.client.loadbalancers.LoadBalancerActor
import stormlantern.consul.client.util._

class ServiceBroker(serviceBrokerActor: ActorRef, consulClient: ConsulHttpClient)(implicit ec: ExecutionContext) extends RetryPolicy with Logging {

  private[this] implicit val timeout = Timeout(10.seconds)

  def withService[A, B](name: String)(f: A ⇒ Future[B]): Future[B] = {
    logger.info(s"Trying to get connection for service $name")
    serviceBrokerActor.ask(ServiceBrokerActor.GetServiceConnection(name)).mapTo[ConnectionHolder].flatMap { connectionHolder ⇒
      logger.info(s"Received connectionholder $connectionHolder")
      try {
        connectionHolder.connection.flatMap(c ⇒ f(c.asInstanceOf[A]))
      } finally {
        connectionHolder.loadBalancer ! LoadBalancerActor.ReturnConnection(connectionHolder)
      }
    }
  }

  def registerService(registration: ServiceRegistration): Future[Unit] = {
    consulClient.putService(registration).map { serviceId ⇒
      // Add shutdown hook
      val deregisterService = new Runnable {
        override def run(): Unit = consulClient.deleteService(serviceId)
      }
      Runtime.getRuntime.addShutdownHook(new Thread(deregisterService))
    }
  }

  def withLeader[A](key: String)(f: Option[LeaderInfo] ⇒ Future[A]): Future[A] = {
    ???
  }

  def joinElection(key: String): Future[Unit] = {
    ???
  }
}

object ServiceBroker {

  def apply(rootActor: ActorSystem, httpClient: ConsulHttpClient, services: Set[ConnectionStrategy]): ServiceBroker = {
    implicit val ec = ExecutionContext.Implicits.global
    val serviceAvailabilityActorFactory = (factory: ActorRefFactory, service: ServiceDefinition, listener: ActorRef) ⇒
      factory.actorOf(ServiceAvailabilityActor.props(httpClient, service, listener))
    val actorRef = rootActor.actorOf(ServiceBrokerActor.props(services, serviceAvailabilityActorFactory), "ServiceBroker")
    new ServiceBroker(actorRef, httpClient)
  }

  def apply(consulAddress: URL, services: Set[ConnectionStrategy]): ServiceBroker = {
    implicit val rootActor = ActorSystem("reactive-consul")
    val httpClient = new AkkaHttpConsulClient(consulAddress)
    ServiceBroker(rootActor, httpClient, services)
  }

}

case class ServiceUnavailableException(service: String) extends RuntimeException(s"$service service unavailable")
