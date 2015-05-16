package xebia.consul.client.loadbalancers

import akka.actor.Status.Failure
import akka.actor.{ Actor, ActorLogging }
import xebia.consul.client.loadbalancers.LoadBalancerActor._
import xebia.consul.client.{ ServiceUnavailableException, ConnectionHolder, ConnectionProvider }

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait LoadBalancerActor extends Actor with LoadBalancer with ActorLogging {

  import akka.pattern.pipe

  // Actor state
  val connectionProviders = mutable.Map.empty[String, ConnectionProvider]

  def selectConnection: Option[Future[ConnectionHolder]]

  def receive = {

    case GetConnection(name) =>
      selectConnection match {
        case Some(connectionHolder) => connectionHolder pipeTo sender
        case None => sender ! Failure(new ServiceUnavailableException(name))
      }
    case ReturnConnection(connection) =>
      connectionProviders.get(connection.key).foreach(_.returnConnection(connection))
    case AddConnectionProvider(key, provider) =>
      connectionProviders.put(key, provider)
    case RemoveConnectionProvider(key) =>
      connectionProviders.remove(key).foreach(_.destroy())
    case HasAvailableConnectionProvider =>
      sender ! (connectionProviders.size > 0)
  }
}

object LoadBalancerActor {
  // Messsages
  case class GetConnection(name: String)
  case class ReturnConnection(connection: ConnectionHolder)
  case class AddConnectionProvider(key: String, provider: ConnectionProvider)
  case class RemoveConnectionProvider(key: String)
  case object HasAvailableConnectionProvider
}
