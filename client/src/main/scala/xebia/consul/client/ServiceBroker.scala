package xebia.consul.client

import akka.actor.{ ActorRefFactory, ActorRef }
import akka.util.Timeout
import xebia.consul.client.loadbalancers.LoadBalancerActor
import xebia.consul.client.util.RetryPolicy
import scala.concurrent.duration._

import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

class ServiceBroker(serviceBrokerActor: ActorRef)(implicit ec: ExecutionContext) extends RetryPolicy {

  import akka.pattern.ask

  private[this] implicit val timeout = Timeout(10.second)

  def withService[A: ClassTag, B](name: String)(f: A => Future[B]): Future[B] = {
    serviceBrokerActor.ask(ServiceBrokerActor.GetServiceConnection(name)).mapTo[ConnectionHolder].flatMap {
      case connectionHolder: ConnectionHolder =>
        try {
          connectionHolder.connection[A].flatMap(f)
        } finally {
          connectionHolder.loadBalancer ! LoadBalancerActor.ReturnConnection(connectionHolder)
        }
    }
  }
}

object ServiceBroker {
  def apply(rootActor: ActorRefFactory, httpClient: CatalogHttpClient, services: Map[String, ConnectionStrategy])(implicit ec: ExecutionContext): ServiceBroker = {
    val serviceAvailabilityActorFactory = (factory: ActorRefFactory, service: String, listener: ActorRef) => factory.actorOf(ServiceAvailabilityActor.props(httpClient, service, listener))
    val actorRef = rootActor.actorOf(ServiceBrokerActor.props(services, serviceAvailabilityActorFactory), "ServiceBroker")
    new ServiceBroker(actorRef)
  }
}

case class ServiceUnavailableException(service: String) extends RuntimeException(s"$service service unavailable")