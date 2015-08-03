package stormlantern.consul.client.election

import java.util
import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.{ TestActorRef, ImplicitSender, TestKit }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ BeforeAndAfterAll, Matchers, FlatSpecLike }
import stormlantern.consul.client.dao.{ BinaryData, KeyData, AcquireSession, ConsulHttpClient }
import stormlantern.consul.client.election.LeaderFollowerActor.Participate

import scala.concurrent.Future

class LeaderFollowerActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FlatSpecLike
    with Matchers with BeforeAndAfterAll with MockFactory {

  def this() = this(ActorSystem("LeaderFollowerActorSpec"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  trait TestScope {
    val sessionId = UUID.fromString("9A3BB9C-E2E7-43DF-BFD5-845417146552")
    val key = "path/to/our/key"
    val host = "myhost.mynetwork.net"
    val port = 1337
    val consulHttpClient = mock[ConsulHttpClient]
    val leaderInfoBytes = s"""{"host":"$host","port":$port}""".getBytes("UTF-8")
  }

  "The LeaderFollowerActor" should "participate in an election, win, watch for changes and participate again when session is lost" in new TestScope {
    val sut = TestActorRef(LeaderFollowerActor.props(consulHttpClient, sessionId, key, host, port))
    (consulHttpClient.putKeyValuePair _).expects(where { (k, lib, op) =>
      k == key && util.Arrays.equals(lib, leaderInfoBytes) && op.contains(AcquireSession(sessionId))
    }).returns(Future.successful(true))
    (consulHttpClient.getKeyValuePair _).expects(key, Some(0L), Some("1s"), false, false).returns {
      Future.successful(Seq(KeyData(key, 1, 1, 1, 0, BinaryData(leaderInfoBytes), Some(sessionId))))
    }
    (consulHttpClient.getKeyValuePair _).expects(key, Some(1L), Some("1s"), false, false).returns {
      Future.successful(Seq(KeyData(key, 1, 2, 1, 0, BinaryData(leaderInfoBytes), None)))
    }
    (consulHttpClient.putKeyValuePair _).expects(where { (k, lib, op) =>
      k == key && util.Arrays.equals(lib, leaderInfoBytes) && op.contains(AcquireSession(sessionId))
    }).onCall { p =>
      sut.stop()
      Future.successful(false)
    }
    sut ! Participate
  }

  it should "participate in an election, lose, watch for changes and participate again when session is lost" in new TestScope {
    val otherSessionId = UUID.fromString("9A3BB9C-E2E7-43DF-BFD5-845417146553")
    val sut = TestActorRef(LeaderFollowerActor.props(consulHttpClient, sessionId, key, host, port))
    (consulHttpClient.putKeyValuePair _).expects(where { (k, lib, op) =>
      k == key && util.Arrays.equals(lib, leaderInfoBytes) && op.contains(AcquireSession(sessionId))
    }).returns(Future.successful(false))
    (consulHttpClient.getKeyValuePair _).expects(key, Some(0L), Some("1s"), false, false).returns {
      Future.successful(Seq(KeyData(key, 1, 1, 1, 0, BinaryData(leaderInfoBytes), Some(otherSessionId))))
    }
    (consulHttpClient.getKeyValuePair _).expects(key, Some(1L), Some("1s"), false, false).returns {
      Future.successful(Seq(KeyData(key, 1, 2, 1, 0, BinaryData(leaderInfoBytes), None)))
    }
    (consulHttpClient.putKeyValuePair _).expects(where { (k, lib, op) =>
      k == key && util.Arrays.equals(lib, leaderInfoBytes) && op.contains(AcquireSession(sessionId))
    }).onCall { p =>
      sut.stop()
      Future.successful(true)
    }
    sut ! Participate
  }
}
