package xebia.consul.client

import java.net.URL

import org.specs2.execute.ResultLike
import org.specs2.mutable.Specification
import retry.Success
import xebia.consul.client.dao.{ SprayConsulHttpClient, TTLCheck, ServiceRegistration, ConsulHttpClient }
import xebia.consul.client.util.{ RetryPolicy, TestActorSystem, ConsulDockerContainer, Logging }

import scala.concurrent.duration.Duration

class SprayCatalogHttpClientIntegrationTest extends Specification with ConsulDockerContainer with TestActorSystem with RetryPolicy with Logging {

  import java.util.concurrent.TimeUnit._
  import scala.concurrent.ExecutionContext.Implicits.global

  // TODO: Remove the dead letter messages caused by the testing procedure
  "The SprayCatalogHttpClient" should {

    "retrieve a single Consul service from a freshly started Consul instance" in withConsulHost { (host, port) =>
      withActorSystem { implicit actorSystem =>
        val subject: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
        subject.findServiceChange("consul").map { result =>
          result.resource must have size 1
          result.resource.head.serviceName mustEqual "consul"
        }.await(retries = 0, timeout = Duration(10, SECONDS))
      }
    }

    "retrieve no unknown service from a freshly started Consul instance" in withConsulHost { (host, port) =>
      withActorSystem { implicit actorSystem =>
        val subject: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
        subject.findServiceChange("bogus").map { result =>
          logger.info(s"Index is ${result.index}")
          result.resource must have size 0
        }.await(retries = 0, timeout = Duration(10, SECONDS))
      }
    }

    "retrieve a single Consul service from a freshly started Consul instance and timeout after the second request if nothing changes" in withConsulHost { (host, port) =>
      withActorSystem { implicit actorSystem =>
        val subject: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
        subject.findServiceChange("consul").flatMap { result =>
          result.resource must have size 1
          result.resource.head.serviceName mustEqual "consul"
          subject.findServiceChange("consul", Some(result.index), Some("500ms")).map { secondResult =>
            secondResult.resource must have size 1
            secondResult.index mustEqual result.index
          }
        }.await(retries = 0, timeout = Duration(10, SECONDS))
      }
    }

    "register a new service with Consul" in withConsulHost { (host, port) =>
      withActorSystem { implicit actorSystem =>
        val subject: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
        subject.registerService(ServiceRegistration("newservice", Some("newservice-1"))).map { result =>
          success
        }.await(retries = 0, timeout = Duration(10, SECONDS))
        subject.registerService(ServiceRegistration("newservice", Some("newservice-2"), check = Some(TTLCheck("2s")))).map { result =>
          success
        }.await(retries = 0, timeout = Duration(10, SECONDS))
        retry { () =>
          subject.findServiceChange("newservice").map { result =>
            result.resource must have size 2
            result.resource.head.serviceName mustEqual "newservice"
          }
        }(Success[ResultLike](r => r.toResult.isSuccess), actorSystem.dispatcher).await(retries = 0, timeout = Duration(20, SECONDS))
      }
    }
  }
}
