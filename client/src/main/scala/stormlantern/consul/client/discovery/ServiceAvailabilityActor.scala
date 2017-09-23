package stormlantern.consul.client
package discovery

import scala.concurrent.{ ExecutionContext, Future }
import akka.actor._
import akka.pattern.pipe
import dao._
import ServiceAvailabilityActor._

class ServiceAvailabilityActor(httpClient: ConsulHttpClient, serviceDefinition: ServiceDefinition, listener: ActorRef) extends Actor {

  implicit val ec: ExecutionContext = context.dispatcher

  // Actor state
  var initialized = false
  var serviceAvailabilityState: IndexedServiceInstances = IndexedServiceInstances.empty

  def receive: Receive = {
    case Start ⇒
      self ! UpdateServiceAvailability(None)
    case UpdateServiceAvailability(services: Option[IndexedServiceInstances]) ⇒
      val (update, serviceChange) = updateServiceAvailability(services.getOrElse(IndexedServiceInstances.empty))
      update.foreach(listener ! _)
      if (!initialized && services.isDefined) {
        initialized = true
        listener ! Started
      }
      serviceChange.map(changes ⇒ UpdateServiceAvailability(Some(changes))) pipeTo self
  }

  def updateServiceAvailability(services: IndexedServiceInstances): (Option[ServiceAvailabilityUpdate], Future[IndexedServiceInstances]) = {
    val update = if (serviceAvailabilityState.index != services.index) {
      val oldServices = serviceAvailabilityState
      serviceAvailabilityState = services.filterForTags(serviceDefinition.serviceTags)
      Some(createServiceAvailabilityUpdate(oldServices, serviceAvailabilityState))
    } else {
      None
    }
    (update, httpClient.getService(
      serviceDefinition.serviceName,
      serviceDefinition.serviceTags.headOption,
      Some(services.index),
      Some("1s")
    ))
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
  case object Started
  case object Initialized
  private case class UpdateServiceAvailability(services: Option[IndexedServiceInstances])
  private[client] case class ServiceAvailabilityUpdate(key: String, added: Set[ServiceInstance] = Set.empty,
    removed: Set[ServiceInstance] = Set.empty)
}
