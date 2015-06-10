package xebia.consul.client

import akka.actor.{ Actor, ActorRef, Props }
import xebia.consul.client.ServiceAvailabilityActor._
import xebia.consul.client.dao.{ IndexedServiceInstances, ServiceInstance, ConsulHttpClient }

import scala.concurrent.Future

class ServiceAvailabilityActor(httpClient: ConsulHttpClient, serviceName: String, listener: ActorRef) extends Actor {

  implicit val ec = context.dispatcher

  // Actor state
  var serviceAvailabilityState: IndexedServiceInstances = IndexedServiceInstances.empty
  var stopping = false

  override def preStart(): Unit = {
    httpClient.findServiceChange(serviceName, None).foreach { change =>
      self ! UpdateServiceAvailability(change)

    }
  }

  override def postStop(): Unit = {
    self ! Stop
  }

  def receive = {
    case UpdateServiceAvailability(services: IndexedServiceInstances) =>
      val (update, serviceChange) = updateServiceAvailability(services)
      update.foreach(listener ! _)
      serviceChange.foreach { change =>
        if (!stopping) {
          self ! UpdateServiceAvailability(change)
        }
      }
    case Stop =>
      stopping = true
  }

  def updateServiceAvailability(services: IndexedServiceInstances): (Option[ServiceAvailabilityUpdate], Future[IndexedServiceInstances]) = {
    val update = if (serviceAvailabilityState.index != services.index) {
      val oldServices = serviceAvailabilityState
      serviceAvailabilityState = services
      Some(createServiceAvailabilityUpdate(oldServices, services))
    } else {
      None
    }
    (update, httpClient.findServiceChange(serviceName, Some(services.index), Some("1s")))
  }

  def createServiceAvailabilityUpdate(oldState: IndexedServiceInstances, newState: IndexedServiceInstances): ServiceAvailabilityUpdate = {
    val deleted = oldState.resource.diff(newState.resource)
    val added = newState.resource.diff(oldState.resource)
    ServiceAvailabilityUpdate(added, deleted)
  }

}

object ServiceAvailabilityActor {

  def props(httpClient: ConsulHttpClient, service: String, listener: ActorRef): Props = Props(new ServiceAvailabilityActor(httpClient, service, listener))

  // Messages
  private case class UpdateServiceAvailability(services: IndexedServiceInstances)
  private[client] case class ServiceAvailabilityUpdate(added: Set[ServiceInstance], removed: Set[ServiceInstance])
  private[client] object ServiceAvailabilityUpdate {
    def empty = ServiceAvailabilityUpdate(Set.empty, Set.empty)
  }
  private[client] object Stop
}