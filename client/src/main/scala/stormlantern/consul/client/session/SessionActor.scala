package stormlantern.consul.client.session

import java.util.UUID

import akka.actor.{ Props, Actor }
import stormlantern.consul.client.dao.ConsulHttpClient
import stormlantern.consul.client.session.SessionActor.{ SessionStarted, StartSession }

import scala.concurrent.Future

class SessionActor(httpClient: ConsulHttpClient) extends Actor {

  import scala.concurrent.ExecutionContext.Implicits.global

  // Actor state
  var sessionId: Option[UUID] = None

  def receive = {
    case StartSession => startSession().map { id =>
      self ! SessionStarted(id)
    }
    case SessionStarted(id) =>
  }

  // Internal methods
  def startSession(): Future[UUID] = {
    httpClient.createSession().map { id =>
      sessionId = Some(id)
      id
    }
  }
}

object SessionActor {
  // Constructors
  def props(httpClient: ConsulHttpClient) = Props(new SessionActor(httpClient))
  // Public messages
  case object StartSession
  // Private messages
  case class SessionStarted(sessionId: UUID)
}
