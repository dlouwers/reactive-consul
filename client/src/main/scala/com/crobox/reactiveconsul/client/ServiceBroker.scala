package com.crobox.reactiveconsul.client

import com.crobox.reactiveconsul.client
import com.crobox.reactiveconsul.client.dao.pekko.PekkoHttpConsulClient
import com.crobox.reactiveconsul.client.dao.{ConsulHttpClient, ServiceRegistration}
import com.crobox.reactiveconsul.client.discovery.{ConnectionHolder, ConnectionStrategy, ServiceAvailabilityActor, ServiceDefinition}
import com.crobox.reactiveconsul.client.election.LeaderInfo
import com.crobox.reactiveconsul.client.loadbalancers.LoadBalancerActor
import com.crobox.reactiveconsul.client.util.{Logging, RetryPolicy}
import org.apache.pekko.actor._
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout

import java.net.URL
import scala.concurrent._
import scala.concurrent.duration._

class ServiceBroker(serviceBrokerActor: ActorRef, consulClient: ConsulHttpClient)(implicit ec: ExecutionContext) extends RetryPolicy with Logging {

  private[this] implicit val timeout = Timeout(10.seconds)

  def withService[A, B](name: String)(f: A => Future[B]): Future[B] = {
    logger.info(s"Trying to get connection for service $name")
    serviceBrokerActor.ask(ServiceBrokerActor.GetServiceConnection(name)).mapTo[ConnectionHolder].flatMap { connectionHolder =>
      logger.info(s"Received connectionholder $connectionHolder")
      try {
        connectionHolder.connection.flatMap(c => f(c.asInstanceOf[A]))
      } finally {
        connectionHolder.loadBalancer ! LoadBalancerActor.ReturnConnection(connectionHolder)
      }
    }
  }

  def registerService(registration: ServiceRegistration): Future[Unit] = {
    consulClient.putService(registration).map { serviceId =>
      // Add shutdown hook
      val deregisterService = new Runnable {
        override def run(): Unit = consulClient.deleteService(serviceId)
      }
      Runtime.getRuntime.addShutdownHook(new Thread(deregisterService))
    }
  }

  def withLeader[A](key: String)(f: Option[LeaderInfo] => Future[A]): Future[A] = {
    ???
  }

  def joinElection(key: String): Future[Unit] = {
    ???
  }
}

object ServiceBroker {

  def apply(rootActor: ActorSystem, httpClient: ConsulHttpClient, services: Set[ConnectionStrategy]): ServiceBroker = {
    implicit val ec = ExecutionContext.Implicits.global
    val serviceAvailabilityActorFactory = (factory: ActorRefFactory, service: ServiceDefinition, listener: ActorRef, onlyHealthyServices: Boolean) =>
      factory.actorOf(ServiceAvailabilityActor.props(httpClient, service, listener, onlyHealthyServices))
    val actorRef = rootActor.actorOf(ServiceBrokerActor.props(services, serviceAvailabilityActorFactory), "ServiceBroker")
    new ServiceBroker(actorRef, httpClient)
  }

  def apply(consulAddress: URL, services: Set[ConnectionStrategy]): ServiceBroker = {
    implicit val rootActor = ActorSystem("reactive-consul")
    val httpClient = new PekkoHttpConsulClient(consulAddress)
    client.ServiceBroker(rootActor, httpClient, services)
  }

}

case class ServiceUnavailableException(service: String) extends RuntimeException(s"$service service unavailable")
