package stormlantern.consul.client.dao

import stormlantern.consul.client.ClientITSpec
import stormlantern.consul.client.dao.akka.AkkaHttpConsulClient
import stormlantern.consul.client.util.{Logging, RetryPolicy}

import java.net.URL
import java.util.UUID
import scala.util.Random

class AkkaHttpConsulClientIT extends ClientITSpec with RetryPolicy with Logging {
  val rnd                        = new Random()
  val subject                    = new AkkaHttpConsulClient(new URL(s"http://$host:$port"))
  val nonExistentSessionId: UUID = UUID.fromString("9A3BB9C-E2E7-43DF-BFD5-845417146552")

  "The AkkaHttpConsulClient" should "retrieve a single Consul service from a freshly started Consul instance" in {
    eventually {
      subject
        .getService("consul")
        .map { result =>
          result.resource should have size 1
          result.resource.head.serviceName shouldEqual "consul"
        }
        .futureValue
    }
  }

  it should "retrieve a single health aware Consul service from a freshly started Consul instance" in {
    eventually {
      subject
        .getServiceHealthAware("consul")
        .map { result =>
          result.resource should have size 1
          result.resource.head.serviceName shouldEqual "consul"
        }
        .futureValue
    }
  }

  it should "retrieve no unknown service from a freshly started Consul instance" in {
    eventually {
      subject
        .getService("bogus")
        .map { result =>
          logger.info(s"Index is ${result.index}")
          result.resource should have size 0
        }
        .futureValue
    }
  }

  it should "retrieve a single Consul service from a freshly started Consul instance and timeout after the second request if nothing changes" in {
    eventually {
      subject
        .getService("consul")
        .flatMap { result =>
          result.resource should have size 1
          result.resource.head.serviceName shouldEqual "consul"
          subject.getService("consul", None, Some(result.index), Some("500ms")).map { secondResult =>
            secondResult.resource should have size 1
            secondResult.index shouldEqual result.index
          }
        }
        .futureValue
    }
  }

  it should "register and deregister a new service with Consul" in {
    subject.putService(ServiceRegistration("newservice", Some("newservice-1"))).futureValue should equal("newservice-1")
    subject
      .putService(ServiceRegistration("newservice", Some("newservice-2"), check = Some(TTLHealthCheck("2s"))))
      .futureValue should equal("newservice-2")
    eventually {
      subject
        .getService("newservice")
        .map { result =>
          result.resource should have size 2
          result.resource.head.serviceName shouldEqual "newservice"
        }
        .futureValue
    }
    subject.deleteService("newservice-1").futureValue should equal(())
    eventually {
      subject.getService("newservice").map { result =>
        result.resource should have size 1
        result.resource.head.serviceName shouldEqual "newservice"
      }
    }
  }

  it should "retrieve a service matching tags and leave out others" in {
    subject
      .putService(ServiceRegistration("newservice", Some("newservice-1"), Set("tag1", "tag2")))
      .futureValue should equal("newservice-1")
    subject
      .putService(ServiceRegistration("newservice", Some("newservice-2"), Set("tag2", "tag3")))
      .futureValue should equal("newservice-2")
    eventually {
      subject
        .getService("newservice")
        .map { result =>
          result.resource should have size 2
          result.resource.head.serviceName shouldEqual "newservice"
        }
        .futureValue
      subject
        .getService("newservice", Some("tag2"))
        .map { result =>
          result.resource should have size 2
          result.resource.head.serviceName shouldEqual "newservice"
        }
        .futureValue
      subject
        .getService("newservice", Some("tag3"))
        .map { result =>
          result.resource should have size 1
          result.resource.head.serviceName shouldEqual "newservice"
          result.resource.head.serviceId shouldEqual "newservice-2"
        }
        .futureValue
    }
  }

  it should "register a session and get it's ID then read it back" in {
    val id: UUID = subject.putSession(Some(SessionCreation(name = Some("MySession")))).futureValue
    subject
      .getSessionInfo(id)
      .map { sessionInfo =>
        sessionInfo should be(defined)
        sessionInfo.get.id shouldEqual id
      }
      .futureValue
  }

  it should "get a session lock on a key/value pair and fail to get a second lock" in {
    val id: UUID = subject.putSession(Some(SessionCreation(name = Some("MySession")))).futureValue
    val payload  = """ { "name" : "test" } """.getBytes("UTF-8")
    val key      = "my/key" + rnd.nextInt(100000)
    subject.putKeyValuePair(key, payload, Some(AcquireSession(id))).futureValue should be(true)
    subject
      .putKeyValuePair(key, payload, Some(AcquireSession(id)))
      .futureValue should be(true) // subsequent calls for same ID is ok
    subject.putKeyValuePair(key, payload, Some(AcquireSession(nonExistentSessionId))).futureValue should be(false)
    subject.putKeyValuePair(key, payload, Some(ReleaseSession(id))).futureValue should be(true)
  }

  it should "get a session lock on a key/value pair and get a second lock after release" in {
    val id: UUID = subject.putSession(Some(SessionCreation(name = Some("MySession")))).futureValue
    val payload  = """ { "name" : "test" } """.getBytes("UTF-8")
    val key      = "my/key" + rnd.nextInt(100000)
    subject.putKeyValuePair(key, payload, Some(AcquireSession(id))).futureValue should be(true)
    subject.putKeyValuePair(key, payload, Some(ReleaseSession(id))).futureValue should be(true)
    subject.putKeyValuePair(key, payload, Some(AcquireSession(id))).futureValue should be(true)
    subject.putKeyValuePair(key, payload, Some(ReleaseSession(id))).futureValue should be(true)
  }

  it should "write a key/value pair and read it back" in {
    val payload = """ { "name" : "test" } """.getBytes("UTF-8")
    val key     = "my/key" + rnd.nextInt(100000)
    subject.putKeyValuePair(key, payload).futureValue should be(true)
    val keyDataSeq = subject.getKeyValuePair(key).futureValue
    keyDataSeq.head.value should equal(BinaryData(payload))
  }

  it should "fail when acquiring a lock on a key with a non-existent session" in {
    val payload = """ { "name" : "test" } """.getBytes("UTF-8")
    val key     = "my/key" + rnd.nextInt(100000)
    subject
      .putKeyValuePair(key, payload, Some(AcquireSession(nonExistentSessionId)))
      .futureValue should be(false)
  }
}
