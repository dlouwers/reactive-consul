package xebia.consul.client.loadbalancers

import akka.actor.Actor
import xebia.consul.client.loadbalancers.BaseLoadBalancerActor.{ AddConnectionProvider, GetConnection, RemoveConnectionProvider, ReturnConnection }
import xebia.consul.client.{ ConnectionHolder, ConnectionProvider }

import scala.collection.mutable

trait BaseLoadBalancerActor extends Actor {

  // Actor state
  val connectionProviders = mutable.Map.empty[String, ConnectionProvider]

  def selectConnection: Option[ConnectionHolder]

  def receive = {
    case GetConnection =>
      sender ! selectConnection
    case ReturnConnection(connection) =>
      connectionProviders.get(connection.key).foreach(_.returnConnection(connection))
    case AddConnectionProvider(key, provider) =>
      connectionProviders.put(key, provider)
    case RemoveConnectionProvider(key) =>
      connectionProviders.remove(key).foreach(_.destroy())
  }

}

object BaseLoadBalancerActor {
  // Messsages
  case object GetConnection
  case class ReturnConnection(connection: ConnectionHolder)
  case class AddConnectionProvider(key: String, provider: ConnectionProvider)
  case class RemoveConnectionProvider(key: String)
}
