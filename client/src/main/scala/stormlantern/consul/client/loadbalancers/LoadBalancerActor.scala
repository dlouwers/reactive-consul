package stormlantern.consul.client.loadbalancers

import akka.actor.Status.Failure
import akka.actor.{ Props, Actor, ActorLogging }
import LoadBalancerActor._
import stormlantern.consul.client.discovery.{ ConnectionProvider, ConnectionHolder }
import stormlantern.consul.client.ServiceUnavailableException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable

class LoadBalancerActor(loadBalancer: LoadBalancer, service: String) extends Actor with ActorLogging {

  import akka.pattern.pipe

  // Actor state
  val connectionProviders = mutable.Map.empty[String, ConnectionProvider]

  override def postStop(): Unit = {
    log.debug(s"LoadBalancerActor for $service stopped, destroying all connection providers")
    connectionProviders.values.foreach(_.destroy())
  }

  def receive: PartialFunction[Any, Unit] = {

    case GetConnection ⇒
      selectConnection match {
        case Some((key, connectionProvider)) ⇒ connectionProvider.getConnectionHolder(key, self) pipeTo sender
        case None                            ⇒ sender ! Failure(ServiceUnavailableException(service))
      }
    case ReturnConnection(connection)         ⇒ returnConnection(connection)
    case AddConnectionProvider(key, provider) ⇒ addConnectionProvider(key, provider)
    case RemoveConnectionProvider(key)        ⇒ removeConnectionProvider(key)
    case HasAvailableConnectionProvider       ⇒ sender ! connectionProviders.nonEmpty
  }

  def selectConnection: Option[(String, ConnectionProvider)] =
    loadBalancer.selectConnection.flatMap(key ⇒ connectionProviders.get(key).map(key → _))

  def returnConnection(connection: ConnectionHolder): Unit = {
    connectionProviders.get(connection.key).foreach(_.returnConnection(connection))
    loadBalancer.connectionReturned(connection.key)
  }

  def addConnectionProvider(key: String, provider: ConnectionProvider): Unit = {
    connectionProviders.put(key, provider)
    loadBalancer.connectionProviderAdded(key)
  }

  def removeConnectionProvider(key: String): Unit = {
    connectionProviders.remove(key).foreach(_.destroy())
    loadBalancer.connectionProviderRemoved(key)
  }
}

object LoadBalancerActor {
  // Props
  def props(loadBalancer: LoadBalancer, service: String) = Props(new LoadBalancerActor(loadBalancer, service))
  // Messsages
  case object GetConnection
  case class ReturnConnection(connection: ConnectionHolder)
  case class AddConnectionProvider(key: String, provider: ConnectionProvider)
  case class RemoveConnectionProvider(key: String)
  case object HasAvailableConnectionProvider
}
