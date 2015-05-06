package xebia.consul.client

import akka.actor.{ ActorRefFactory, ActorRef }
import akka.util.Timeout
import xebia.consul.client.loadbalancers.LoadBalancerActor
import scala.concurrent.duration._

import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

class ServiceBroker(serviceBrokerActor: ActorRef)(implicit ec: ExecutionContext) {

  import akka.pattern.ask

  implicit val timeout = Timeout(1.second)
  def withService[A, B](name: String)(f: A => Future[B]): Future[B] = {
    serviceBrokerActor.ask(ServiceBrokerActor.GetServiceConnection(name)).mapTo[Option[ConnectionHolder]].flatMap {
      case Some(connectionHolder) =>
        try {
          connectionHolder.connection[A].flatMap(f)
        } finally {
          connectionHolder.loadBalancer ! LoadBalancerActor.ReturnConnection(connectionHolder)
        }
      case None =>
        throw new RuntimeException()
    }
  }
}

object ServiceBroker {
  def apply(factory: ActorRefFactory, httpClient: CatalogHttpClient, services: Map[String, ConnectionStrategy])(implicit ec: ExecutionContext): ServiceBroker = {
    new ServiceBroker(factory.actorOf(ServiceBrokerActor.props(services, httpClient)))
  }
}