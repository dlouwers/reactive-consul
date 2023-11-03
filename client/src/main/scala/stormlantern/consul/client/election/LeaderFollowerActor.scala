package stormlantern.consul.client.election

import java.util.UUID

import org.apache.pekko.actor.{ Actor, Props }
import spray.json._
import stormlantern.consul.client.dao.{ AcquireSession, BinaryData, ConsulHttpClient, KeyData }
import stormlantern.consul.client.election.LeaderFollowerActor._

class LeaderFollowerActor(httpClient: ConsulHttpClient, sessionId: UUID, key: String, host: String, port: Int) extends Actor with DefaultJsonProtocol {

  implicit val ec = context.dispatcher

  implicit val leaderInfoFormat = jsonFormat2(LeaderInfo)
  val leaderInfoBytes = LeaderInfo(host, port).toJson.compactPrint.getBytes("UTF-8")

  // Actor state
  var electionState: Option[ElectionState] = None

  // Behavior
  def receive = {
    case Participate =>
      httpClient.putKeyValuePair(key, leaderInfoBytes, Some(AcquireSession(sessionId))).map {
        case true =>
          self ! SetElectionState(Some(Leader))
          self ! MonitorLock(0)
        case false =>
          self ! MonitorLock(0)
      }
    case SetElectionState(state) =>
      electionState = state
    case MonitorLock(index) =>
      httpClient.getKeyValuePair(key, index = Some(index), wait = Some("1s")).map {
        case Seq(KeyData(_, _, newIndex, _, _, BinaryData(data), session)) =>
          if (newIndex > index) {
            if (session.isEmpty) {
              self ! SetElectionState(None)
              self ! Participate
            } else if (session.get == sessionId) {
              self ! SetElectionState(Some(Leader))
              self ! MonitorLock(newIndex)
            } else {
              val leaderInfo = new String(data, "UTF-8").parseJson.convertTo[LeaderInfo](leaderInfoFormat)
              self ! SetElectionState(Some(Follower(leaderInfo.host, leaderInfo.port)))
              self ! MonitorLock(newIndex)
            }
          } else {
            self ! MonitorLock(index)
          }
      }
  }
}

object LeaderFollowerActor {

  //Props
  def props(httpClient: ConsulHttpClient, sessionId: UUID, key: String, host: String, port: Int): Props =
    Props(new LeaderFollowerActor(httpClient, sessionId, key, host, port))

  // Election state
  sealed trait ElectionState
  case object Leader extends ElectionState
  case class Follower(host: String, port: Int) extends ElectionState

  // Internal messages
  case object Participate
  case class SetElectionState(state: Option[ElectionState])
  case class MonitorLock(lastIndex: Long)
}