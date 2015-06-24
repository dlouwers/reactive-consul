package xebia.consul.client

import java.net.URL

import akka.actor._
import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration.{ Interval, Timeout }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import retry.Success
import xebia.consul.client.dao.{ ConsulHttpClient, ServiceRegistration, SprayConsulHttpClient }
import xebia.consul.client.loadbalancers.RoundRobinLoadBalancer
import xebia.consul.client.util._

import scala.concurrent.Future

class ServiceBrokerIntegrationTest extends FlatSpec with Matchers with ScalaFutures with ConsulDockerContainer with RetryPolicy with TestActorSystem with Logging {

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

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
      val connectionStrategy = ConnectionStrategy("consul-http", connectionProviderFactory, new RoundRobinLoadBalancer)
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
