package stormlantern.consul.client

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.TestKit
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import stormlantern.consul.client.dao.akka.AkkaHttpConsulClient
import stormlantern.consul.client.dao.{ConsulHttpClient, ServiceRegistration}
import stormlantern.consul.client.discovery.{ConnectionProvider, ConnectionProviderFactory, ConnectionStrategy, ServiceDefinition}
import stormlantern.consul.client.loadbalancers.RoundRobinLoadBalancer
import stormlantern.consul.client.util.{ConsulDockerContainer, Logging}

import java.net.URL
import scala.concurrent.Future

abstract class ClientITSpec(val config: Config = ConfigFactory.load())
  extends TestKit(ActorSystem("TestSystem", config.getConfig("crobox.clickhouse.client")))
    with AnyFlatSpecLike
    with Matchers
    with ScalaFutures with Eventually with IntegrationPatience with ConsulDockerContainer with Logging {

  import scala.concurrent.ExecutionContext.Implicits.global

  "The ServiceBroker" should "provide a usable connection to consul" in withConsulHost { (host, port) =>
    val akkaHttpClient = new AkkaHttpConsulClient(new URL(s"http://$host:$port"))
    // Register the HTTP interface
    akkaHttpClient.putService(ServiceRegistration("consul-http", Some("consul-http-1"), address = Some(host), port = Some(port)))
    akkaHttpClient.putService(ServiceRegistration("consul-http", Some("consul-http-2"), address = Some(host), port = Some(port)))
    val connectionProviderFactory = new ConnectionProviderFactory {
      override def create(host: String, port: Int): ConnectionProvider = new ConnectionProvider {
        logger.info(s"Asked to create connection provider for $host:$port")
        val httpClient: ConsulHttpClient = new AkkaHttpConsulClient(new URL(s"http://$host:$port"))

        override def getConnection: Future[Any] = Future.successful(httpClient)
      }
    }
    val connectionStrategy = ConnectionStrategy(ServiceDefinition("consul-http"), connectionProviderFactory, new RoundRobinLoadBalancer, onlyHealthyServices = true)
    val sut = ServiceBroker(system, akkaHttpClient, Set(connectionStrategy))
    eventually {
      sut.withService("consul-http") { connection: ConsulHttpClient =>
        connection.getService("bogus").map(_.resource should have size 0)
      }
      sut
    }
  }
}
