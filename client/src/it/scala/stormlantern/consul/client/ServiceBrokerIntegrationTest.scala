package stormlantern.consul.client

import java.net.URL

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import retry.Success
import stormlantern.consul.client.dao.{ ConsulHttpClient, ServiceRegistration, SprayConsulHttpClient }
import stormlantern.consul.client.loadbalancers.RoundRobinLoadBalancer
import stormlantern.consul.client.util.{ ConsulDockerContainer, Logging, RetryPolicy, TestActorSystem }

import scala.concurrent.Future

class ServiceBrokerIntegrationTest extends FlatSpec with Matchers with ScalaFutures with ConsulDockerContainer with RetryPolicy with TestActorSystem with Logging {

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val defaultPatience = PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

  "The ServiceBroker" should "provide a usable connection to consul" in withConsulHost { (host, port) =>
    withActorSystem { implicit actorSystem =>
      val sprayHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
      // Register the HTTP interface
      sprayHttpClient.registerService(ServiceRegistration("consul-http", Some("consul-http-1"), address = Some(host), port = Some(port)))
      sprayHttpClient.registerService(ServiceRegistration("consul-http", Some("consul-http-2"), address = Some(host), port = Some(port)))
      val connectionProviderFactory = new ConnectionProviderFactory {
        override def create(host: String, port: Int): ConnectionProvider = new ConnectionProvider {
          logger.info(s"Asked to create connection provider for $host:$port")
          val httpClient: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
          override def getConnection: Future[Any] = Future.successful(httpClient)
        }
      }
      val connectionStrategy = ConnectionStrategy(ServiceDefinition("consul-http"), connectionProviderFactory, new RoundRobinLoadBalancer)
      val sut = ServiceBroker(actorSystem, sprayHttpClient, Set(connectionStrategy))
      val success = Success[Unit](r => true)
      val r = retry { () =>
        sut.withService("consul-http") { connection: ConsulHttpClient =>
          connection.findServiceChange("bogus").map(_.resource should have size 0)
        }
      }(success, actorSystem.dispatcher).futureValue
    }
  }
}
