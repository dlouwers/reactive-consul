package stormlantern.consul.client.loadbalancers

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class RoundRobinLoadBalancerTest extends AnyFlatSpecLike with Matchers {

  "The RoundRobinLoadBalancer" should "select a connection" in {
    val sut = new RoundRobinLoadBalancer
    sut.selectConnection shouldBe empty
    sut.connectionProviderAdded("one")
    sut.selectConnection should contain("one")
    sut.selectConnection should contain("one")
    sut.connectionProviderAdded("two")
    sut.connectionProviderAdded("three")
    sut.selectConnection should contain("one")
    sut.selectConnection should contain("two")
    sut.selectConnection should contain("three")
    sut.selectConnection should contain("one")
    sut.connectionProviderRemoved("two")
    sut.selectConnection should contain("one")
    sut.selectConnection should contain("three")
  }

}
