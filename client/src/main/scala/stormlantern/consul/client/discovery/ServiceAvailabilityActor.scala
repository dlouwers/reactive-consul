package stormlantern.consul.client
package discovery

import scala.concurrent.Future

import akka.actor._
import akka.pattern.pipe

import dao._
import ServiceAvailabilityActor._

class ServiceAvailabilityActor(httpClient: ConsulHttpClient, serviceDefinition: ServiceDefinition, listener: ActorRef) extends Actor {

  implicit val ec = context.dispatcher

  // Actor state
  var serviceAvailabilityState: IndexedServiceInstances = IndexedServiceInstances.empty

  def receive = {
    case Start ⇒ self ! UpdateServiceAvailability(IndexedServiceInstances.empty)
    case UpdateServiceAvailability(services: IndexedServiceInstances) ⇒
      val (update, serviceChange) = updateServiceAvailability(services)
      update.foreach(listener ! _)
      serviceChange.map(UpdateServiceAvailability) pipeTo self
  }

  def updateServiceAvailability(services: IndexedServiceInstances): (Option[ServiceAvailabilityUpdate], Future[IndexedServiceInstances]) = {
    val update = if (serviceAvailabilityState.index != services.index) {
      val oldServices = serviceAvailabilityState
      serviceAvailabilityState = services.filterForTags(serviceDefinition.serviceTags)
      Some(createServiceAvailabilityUpdate(oldServices, serviceAvailabilityState))
    } else {
      None
    }
    (update, httpClient.getService(serviceDefinition.serviceName, serviceDefinition.serviceTags.headOption, Some(services.index), Some("1s")))
  }

  def createServiceAvailabilityUpdate(oldState: IndexedServiceInstances, newState: IndexedServiceInstances): ServiceAvailabilityUpdate = {
    val deleted = oldState.resource.diff(newState.resource)
    val added = newState.resource.diff(oldState.resource)
    ServiceAvailabilityUpdate(serviceDefinition.key, added, deleted)
  }

}

object ServiceAvailabilityActor {

  def props(httpClient: ConsulHttpClient, serviceDefinition: ServiceDefinition, listener: ActorRef): Props = Props(new ServiceAvailabilityActor(httpClient, serviceDefinition, listener))

  // Messages
  case object Start
  private case class UpdateServiceAvailability(services: IndexedServiceInstances)
  private[client] case class ServiceAvailabilityUpdate(key: String, added: Set[ServiceInstance], removed: Set[ServiceInstance])
  private[client] object ServiceAvailabilityUpdate {
    def empty = ServiceAvailabilityUpdate("", Set.empty, Set.empty)
  }
}
