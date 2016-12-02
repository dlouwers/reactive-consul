package stormlantern.consul.client.discovery

import akka.actor.{ Actor, ActorRef, Props }
import stormlantern.consul.client.dao.{ ConsulHttpClient, IndexedServiceInstances, ServiceInstance }
import stormlantern.consul.client.discovery.ServiceAvailabilityActor._

import scala.concurrent.Future
import scala.util.{ Failure, Success }

class ServiceAvailabilityActor(httpClient: ConsulHttpClient, serviceDefinition: ServiceDefinition, listener: ActorRef) extends Actor {

  implicit val ec = context.dispatcher

  // Actor state
  var serviceAvailabilityState: IndexedServiceInstances = IndexedServiceInstances.empty

  def receive = {
    case Start ⇒ self ! UpdateServiceAvailability(IndexedServiceInstances.empty)
    case UpdateServiceAvailability(services: IndexedServiceInstances) ⇒
      val (update, serviceChange) = updateServiceAvailability(services)
      update.foreach(listener ! _)
      serviceChange.onComplete {
        case Success(change) ⇒ self ! UpdateServiceAvailability(change)
        case Failure(e)      ⇒ throw e
      }
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
    ServiceAvailabilityUpdate(added, deleted)
  }

}

object ServiceAvailabilityActor {

  def props(httpClient: ConsulHttpClient, serviceDefinition: ServiceDefinition, listener: ActorRef): Props = Props(new ServiceAvailabilityActor(httpClient, serviceDefinition, listener))

  // Messages
  case object Start
  private case class UpdateServiceAvailability(services: IndexedServiceInstances)
  private[client] case class ServiceAvailabilityUpdate(added: Set[ServiceInstance], removed: Set[ServiceInstance])
  private[client] object ServiceAvailabilityUpdate {
    def empty = ServiceAvailabilityUpdate(Set.empty, Set.empty)
  }
}
