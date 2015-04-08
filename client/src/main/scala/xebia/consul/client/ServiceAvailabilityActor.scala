package xebia.consul.client

import akka.actor.{ ActorRef, Actor, Props }
import akka.event.Logging
import xebia.consul.client.ServiceAvailabilityActor._

import scala.concurrent.Future

class ServiceAvailabilityActor(httpClient: CatalogHttpClient, serviceName: String, listener: ActorRef) extends Actor {

  val log = Logging(context.system, this)
  implicit val ec = context.dispatcher

  // Actor state
  var serviceAvailabilityState: IndexedServiceInstances = IndexedServiceInstances.empty
  var stopping = false

  override def preStart(): Unit = {
    httpClient.findServiceChange(serviceName, None).map { change =>
      self ! UpdateServiceAvailability(change)
    }
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
    val deleted = oldState.instances.filterNot(sv => newState.instances.contains(sv)).toSet
    val added = newState.instances.filterNot(s => oldState.instances.contains(s)).toSet
    ServiceAvailabilityUpdate(added, deleted)
  }

}

object ServiceAvailabilityActor {

  def props(httpClient: CatalogHttpClient, service: String, listener: ActorRef): Props = Props(new ServiceAvailabilityActor(httpClient, service, listener))

  // Messages
  private case class UpdateServiceAvailability(services: IndexedServiceInstances)
  private[client] case class ServiceAvailabilityUpdate(added: Set[Service], removed: Set[Service])
  private[client] object ServiceAvailabilityUpdate {
    def empty = ServiceAvailabilityUpdate(Set.empty, Set.empty)
  }
  private[client] object Stop
}