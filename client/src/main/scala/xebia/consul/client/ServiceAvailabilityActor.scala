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
    httpClient.findServiceChange(serviceName, None).map { change =>
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
      serviceChange.map { change =>
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
    (update, httpClient.findServiceChange(serviceName, Some(services.index)))
  }

  def createServiceAvailabilityUpdate(oldState: IndexedServiceInstances, newState: IndexedServiceInstances): ServiceAvailabilityUpdate = {
    val deleted = oldState.resource.filterNot(sv => newState.resource.contains(sv))
    val added = newState.resource.filterNot(s => oldState.resource.contains(s))
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