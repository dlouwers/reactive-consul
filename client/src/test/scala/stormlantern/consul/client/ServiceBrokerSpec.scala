package stormlantern.consul.client

import akka.actor.{ ActorRef, ActorSystem }
import akka.actor.Status.Failure
import akka.testkit.{ ImplicitSender, TestKit }
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }
import stormlantern.consul.client.dao.ConsulHttpClient
import stormlantern.consul.client.discovery.ConnectionHolder
import stormlantern.consul.client.helpers.CallingThreadExecutionContext
import stormlantern.consul.client.loadbalancers.LoadBalancerActor
import stormlantern.consul.client.util.Logging

import scala.concurrent.Future

class ServiceBrokerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FlatSpecLike
    with Matchers with ScalaFutures with BeforeAndAfterAll with MockFactory with Logging {

  implicit val ec = CallingThreadExecutionContext()
  def this() = this(ActorSystem("ServiceBrokerSpec"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  trait TestScope {
    val connectionHolder: ConnectionHolder = mock[ConnectionHolder]
    val httpClient: ConsulHttpClient = mock[ConsulHttpClient]
    val loadBalancer: ActorRef = self
  }

  "The ServiceBroker" should "return a service connection when requested" in new TestScope {
    (connectionHolder.connection _).expects().returns(Future.successful(true))
    (connectionHolder.loadBalancer _).expects().returns(loadBalancer)
    val sut = new ServiceBroker(self, httpClient)
    val result: Future[Boolean] = sut.withService("service1") { service: Boolean ⇒
      Future.successful(service)
    }
    expectMsgPF() {
      case ServiceBrokerActor.GetServiceConnection("service1") ⇒
        lastSender ! connectionHolder
        result.map(_ shouldEqual true).futureValue
    }
    expectMsg(LoadBalancerActor.ReturnConnection(connectionHolder))
  }

  it should "return the connection when an error occurs" in new TestScope {
    (connectionHolder.connection _).expects().returns(Future.successful(true))
    (connectionHolder.loadBalancer _).expects().returns(loadBalancer)
    val sut = new ServiceBroker(self, httpClient)
    val result: Future[Boolean] = sut.withService[Boolean, Boolean]("service1") { service: Boolean ⇒
      throw new RuntimeException()
    }
    expectMsgPF() {
      case ServiceBrokerActor.GetServiceConnection("service1") ⇒
        lastSender ! connectionHolder
        an[RuntimeException] should be thrownBy result.futureValue
    }
    expectMsg(LoadBalancerActor.ReturnConnection(connectionHolder))
  }

  it should "throw an error when an excpetion is returned" in new TestScope {
    val sut = new ServiceBroker(self, httpClient)
    val result: Future[Boolean] = sut.withService("service1") { service: Boolean ⇒
      Future.successful(service)
    }
    expectMsgPF() {
      case ServiceBrokerActor.GetServiceConnection("service1") ⇒
        lastSender ! Failure(new RuntimeException())
        an[RuntimeException] should be thrownBy result.futureValue
    }
  }
}
