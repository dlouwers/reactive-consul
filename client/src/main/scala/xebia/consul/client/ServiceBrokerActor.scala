package xebia.consul.client

import akka.actor.{ Actor, ActorRef, Props }
import akka.event.Logging
import xebia.consul.client.ServiceAvailabilityActor._

class ServiceBrokerActor(serviceAvailabilityPropsFactory: (String, ActorRef) => Props, services: Set[String]) extends Actor with ActorSupport {

  val log = Logging(context.system, this)

  override def preStart(): Unit = {
    services.foreach { s =>
      log.debug(s"Starting Service Availability Actor for $s")
      createChild(serviceAvailabilityPropsFactory(s, self))
    }
  }
  def receive = {
    case ServiceAvailabilityUpdate(added, deleted) =>
      log.debug(s"Need to add connection pools for $added")
      log.debug(s"Need to clean up conection pools for $deleted")
  }

}

object ServiceBrokerActor {
  def props(serviceAvailabilityPropsFactory: (String, ActorRef) => Props, services: Set[String]): Props = Props(new ServiceBrokerActor(serviceAvailabilityPropsFactory, services))
}
