package stormlantern.consul.client.loadbalancers

import org.specs2.mutable.Specification

class RoundRobinLoadBalancerSpec extends Specification {

  "The RoundRobinLoadBalancer" should {
    "select a connection" in {
      val sut = new RoundRobinLoadBalancer
      sut.selectConnection should beNone
      sut.connectionProviderAdded("one")
      sut.selectConnection should beSome("one")
      sut.selectConnection should beSome("one")
      sut.connectionProviderAdded("two")
      sut.connectionProviderAdded("three")
      sut.selectConnection should beSome("one")
      sut.selectConnection should beSome("two")
      sut.selectConnection should beSome("three")
      sut.selectConnection should beSome("one")
      sut.connectionProviderRemoved("two")
      sut.selectConnection should beSome("one")
      sut.selectConnection should beSome("three")
    }

  }
}
