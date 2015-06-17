package xebia.consul.client

import java.net.URL

import akka.actor.{ ActorSystem, ActorRefFactory, ActorRef }
import akka.util.Timeout
import com.spotify.dns.DnsSrvResolvers
import xebia.consul.client.dao.{ SprayConsulHttpClient, ServiceRegistration, ConsulHttpClient }
import xebia.consul.client.loadbalancers.LoadBalancerActor
import xebia.consul.client.util.{ Logging, RetryPolicy }
import scala.concurrent.duration._
import collection.JavaConversions._

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

  def findConsul(fqdn: String): URL = {
    val resolver = DnsSrvResolvers.newBuilder().build()
    val lookupResult = resolver.resolve(fqdn).headOption.getOrElse(throw new RuntimeException(s"No record found for $fqdn"))
    new URL(s"http://${lookupResult.host()}:${lookupResult.port()}")
  }

  def apply(rootActor: ActorSystem, httpClient: ConsulHttpClient, services: Map[String, ConnectionStrategy]): ServiceBroker = {
    implicit val ec = ExecutionContext.Implicits.global
    val serviceAvailabilityActorFactory = (factory: ActorRefFactory, service: String, listener: ActorRef) => factory.actorOf(ServiceAvailabilityActor.props(httpClient, service, listener))
    val actorRef = rootActor.actorOf(ServiceBrokerActor.props(services, serviceAvailabilityActorFactory), "ServiceBroker")
    new ServiceBroker(actorRef, httpClient)
  }

  def apply(consulAddress: String, services: Map[String, ConnectionStrategy]): ServiceBroker = {
    implicit val rootActor = ActorSystem("reactive-consul")
    val httpClient = new SprayConsulHttpClient(findConsul(consulAddress))
    ServiceBroker(rootActor, httpClient, services)
  }

}

case class ServiceUnavailableException(service: String) extends RuntimeException(s"$service service unavailable")