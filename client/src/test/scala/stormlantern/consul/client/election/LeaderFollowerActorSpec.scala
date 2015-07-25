package stormlantern.consul.client.election

import akka.actor.ActorSystem
import akka.testkit.{ TestActorRef, ImplicitSender, TestKit }
import org.scalatest.{ BeforeAndAfterAll, Matchers, FlatSpecLike }

class LeaderFollowerActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FlatSpecLike
    with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("LeaderFollowerActorSpec"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "The LeaderFollowerActor" should "participate in and win an election" in {
    //val subject = TestActorRef(new LeaderFollowerActor())
  }
}
