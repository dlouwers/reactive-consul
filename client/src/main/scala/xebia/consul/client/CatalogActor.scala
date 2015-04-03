package xebia.consul.client

import akka.actor.{ Actor, ActorRef, Props }
import akka.event.Logging
import xebia.consul.client.CatalogActor._

import scala.collection.mutable

class CatalogActor(httpClient: CatalogHttpClient) extends Actor {

  val log = Logging(context.system, this)
  implicit val ec = context.dispatcher

  // Actor state
  case class ServiceAvailability(listeners: mutable.Set[ActorRef], state: IndexedServices)
  case class SystemAvailability(services: mutable.Map[String, ServiceAvailability] = mutable.Map.empty) {

    def registerListener(name: String, listener: ActorRef): Unit = {
      if (services.contains(name)) {
        // Add the service listener
        services(name).listeners.add(listener)
      } else {
        // Create a new service with listener
        services.put(name, ServiceAvailability(mutable.Set(listener), IndexedServices(0, Seq.empty)))
        // Start listening for updates
        self ! FindServiceAvailabilityChanges(name)
      }
    }

    def deregisterListener(name: String, listener: ActorRef): Unit = {
      if (services.contains(name)) {
        // Remove the service listener
        services(name).listeners.remove(listener)
        // Remove the service if there are no more listeners
        if (services(name).listeners.isEmpty) {
          services.remove(name)
        }
      }
    }

    def updateService(name: String, services: IndexedServices): ServiceAvailabilityUpdate = {
      val deleted = this.services(name).state.instances.filterNot(sv => services.instances.contains(sv)).toSet
      val added = services.instances.filterNot(s => this.services(name).state.instances.contains(s)).toSet
      this.services.put(name, this.services(name).copy(state = services))
      ServiceAvailabilityUpdate(added, deleted)
    }
  }
  val systemAvailability = SystemAvailability()

  def receive = {
    case RegisterServiceAvailabilityListener(name, listener) =>
      registerServiceAvailabilityListener(name, listener)
    case FindServiceAvailabilityChanges(name, index) =>
      log.debug("Actor initializing")
      findServiceAvailabilityChanges(name, index)
    case UpdateServiceAvailability(name, s) =>
      log.debug(s"Updating service $name")
      updateServiceAvailability(name, s)
  }

  def findServiceAvailabilityChanges(name: String, index: Option[Long]): Unit = {
    httpClient.findServiceChange(name, index).foreach { is =>
      self ! UpdateServiceAvailability(name, is)
      if (systemAvailability.services.contains(name)) {
        // Query for services availability changes if there are listeners for it
        self ! FindServiceAvailabilityChanges(name)
      }
    }
  }

  def registerServiceAvailabilityListener(name: String, listener: ActorRef): Unit = {
    // Register the listener and return the current state
    systemAvailability.registerListener(name, listener)
  }

  def deregisterServiceAvailabilityListener(name: String, listener: ActorRef): Unit = {
    systemAvailability.deregisterListener(name, listener)
  }

  def updateServiceAvailability(name: String, indexedServices: IndexedServices): Unit = {
    val changes = systemAvailability.updateService(name, indexedServices)
    // Send changes to listeners if there are any
    systemAvailability.services.get(name).foreach { service =>
      service.listeners.foreach(_ ! changes)
    }
  }
}

object CatalogActor {

  def props(httpClient: CatalogHttpClient): Props = Props(new CatalogActor(httpClient))

  // Messages
  private case class FindServiceAvailabilityChanges(name: String, index: Option[Long] = None)
  private case class UpdateServiceAvailability(name: String, services: IndexedServices)
  private case class RegisterServiceAvailabilityListener(name: String, ref: ActorRef)
  private case class UnregisterServiceAvailabilityListener(name: String, ref: ActorRef)

  private[client] case class ServiceAvailabilityUpdate(added: Set[Service], removed: Set[Service])
}