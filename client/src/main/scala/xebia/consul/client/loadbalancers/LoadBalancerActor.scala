package xebia.consul.client.loadbalancers

import akka.actor.Actor
import xebia.consul.client.loadbalancers.LoadBalancerActor.{ AddConnectionProvider, GetConnection, RemoveConnectionProvider, ReturnConnection }
import xebia.consul.client.{ ConnectionHolder, ConnectionProvider }

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait LoadBalancerActor extends Actor with LoadBalancer {

  import akka.pattern.pipe

  // Actor state
  val connectionProviders = mutable.Map.empty[String, ConnectionProvider]

  def selectConnection: Future[Option[ConnectionHolder]]

  def receive = {

    case GetConnection =>
      selectConnection pipeTo sender
    case ReturnConnection(connection) =>
      connectionProviders.get(connection.key).foreach(_.returnConnection(connection))
    case AddConnectionProvider(key, provider) =>
      connectionProviders.put(key, provider)
    case RemoveConnectionProvider(key) =>
      connectionProviders.remove(key).foreach(_.destroy())
  }

}

object LoadBalancerActor {
  // Messsages
  case object GetConnection
  case class ReturnConnection(connection: ConnectionHolder)
  case class AddConnectionProvider(key: String, provider: ConnectionProvider)
  case class RemoveConnectionProvider(key: String)
}
