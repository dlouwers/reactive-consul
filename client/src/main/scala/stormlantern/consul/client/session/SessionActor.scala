package stormlantern.consul.client.session

import java.util.UUID

import akka.actor.{ Actor, ActorRef, Props }
import stormlantern.consul.client.dao.{ KeyValueClient, ServiceDiscoveryClient }
import stormlantern.consul.client.session.SessionActor.{ MonitorSession, SessionAcquired, StartSession }

import scala.concurrent.Future

class SessionActor(httpClient: KeyValueClient, listener: ActorRef) extends Actor {

  import scala.concurrent.ExecutionContext.Implicits.global

  // Actor state
  var sessionId: Option[UUID] = None

  def receive = {
    case StartSession ⇒ startSession().map { id ⇒
      self ! SessionAcquired(id)
    }
    case SessionAcquired(id) ⇒
      sessionId = Some(id)
      listener ! SessionAcquired(id)
      self ! MonitorSession(0)
    case MonitorSession(lastIndex) ⇒

  }

  // Internal methods
  def startSession(): Future[UUID] = {
    httpClient.putSession().map { id ⇒
      sessionId = Some(id)
      id
    }
  }
}

object SessionActor {
  // Constructors
  def props(keyValueClient: KeyValueClient, listener: ActorRef) = Props(new SessionActor(keyValueClient, listener))
  // Public messages
  case object StartSession
  case class SessionAcquired(sessionId: UUID)
  // Private messages
  private case class MonitorSession(lastIndex: Long)
}
