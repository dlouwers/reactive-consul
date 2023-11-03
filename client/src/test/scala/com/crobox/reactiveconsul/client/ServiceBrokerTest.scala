package com.crobox.reactiveconsul.client

import com.crobox.reactiveconsul.client.dao.ConsulHttpClient
import com.crobox.reactiveconsul.client.discovery.ConnectionHolder
import com.crobox.reactiveconsul.client.loadbalancers.LoadBalancerActor
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.Status.Failure

import scala.concurrent.Future

class ServiceBrokerTest extends ClientSpec {

  trait TestScope {
    val connectionHolder: ConnectionHolder = mock[ConnectionHolder]
    val httpClient: ConsulHttpClient       = mock[ConsulHttpClient]
    val loadBalancer: ActorRef             = self
  }

  "The ServiceBroker" should "return a service connection when requested" in new TestScope {
    (() => connectionHolder.connection).expects().returns(Future.successful(true))
    (() => connectionHolder.loadBalancer).expects().returns(loadBalancer)
    val sut = new ServiceBroker(self, httpClient)

    val result: Future[Boolean] = sut.withService("service1") { service: Boolean =>
      Future.successful(service)
    }
    expectMsgPF() {
      case ServiceBrokerActor.GetServiceConnection("service1") =>
        lastSender ! connectionHolder
        result.map(_ shouldEqual true).futureValue
    }
    expectMsg(LoadBalancerActor.ReturnConnection(connectionHolder))
  }

  it should "return the connection when an error occurs" in new TestScope {
    (() => connectionHolder.connection).expects().returns(Future.successful(true))
    (() => connectionHolder.loadBalancer).expects().returns(loadBalancer)
    val sut = new ServiceBroker(self, httpClient)

    val result: Future[Boolean] = sut.withService[Boolean, Boolean]("service1") { service: Boolean =>
      throw new RuntimeException()
    }
    expectMsgPF() {
      case ServiceBrokerActor.GetServiceConnection("service1") =>
        lastSender ! connectionHolder
        an[RuntimeException] should be thrownBy result.futureValue
    }
    expectMsg(LoadBalancerActor.ReturnConnection(connectionHolder))
  }

  it should "throw an error when an excpetion is returned" in new TestScope {
    val sut = new ServiceBroker(self, httpClient)

    val result: Future[Boolean] = sut.withService("service1") { service: Boolean =>
      Future.successful(service)
    }
    expectMsgPF() {
      case ServiceBrokerActor.GetServiceConnection("service1") =>
        lastSender ! Failure(new RuntimeException())
        an[RuntimeException] should be thrownBy result.futureValue
    }
  }
}
