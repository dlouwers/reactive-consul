package xebia.consul.client

import java.net.URL
import java.util.concurrent.TimeUnit._

import akka.actor.{ ActorRef, ActorRefFactory, ActorSystem, Props }
import akka.testkit.TestKit
import org.specs2.mutable.Specification
import org.specs2.specification
import xebia.consul.client.loadbalancers.LoadBalancerActor
import xebia.consul.client.util.{ Logging, ConsulDockerContainer }

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class ServiceBrokerIntegrationTest extends Specification with ConsulDockerContainer with Logging {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After {
    implicit val ec = system.dispatcher
    override def after: Any = system.shutdown()
  }

  "The ServiceBroker" should {
    "provide a usable connection to consul" in new ActorScope {
      withConsulHost { (host, port) =>
        val connectionProviderFactory = new ConnectionProviderFactory {
          override def create(host: String, port: Int): ConnectionProvider = new ConnectionProvider {
            val httpClient: CatalogHttpClient = new SprayCatalogHttpClient(new URL(s"$host:$port"))
            override def destroy(): Unit = Unit
            override def returnConnection(connection: ConnectionHolder): Unit = Unit
            override def getConnection(lb: ActorRef): Future[ConnectionHolder] = Future.successful(new ConnectionHolder {
              override def connection[A]: Future[A] = Future.successful(httpClient).map(_.asInstanceOf[A])
              override val loadBalancer: ActorRef = lb
              override val key: String = "consul"
            })
          }
        }
        class NaiveLoadBalancer extends LoadBalancerActor {
          override def selectConnection: Future[Option[ConnectionHolder]] = connectionProviders("consul").getConnection(self).map(Some(_))
        }
        val loadBalancerFactory = (f: ActorRefFactory) => f.actorOf(Props[NaiveLoadBalancer])
        val httpClient = new SprayCatalogHttpClient(new URL(s"$host:$port"))
        val connectionStrategy = ConnectionStrategy(connectionProviderFactory, loadBalancerFactory)
        val sut = ServiceBroker(system, httpClient, services = Map("consul" -> connectionStrategy))
        sut.withService("consu") { connection: CatalogHttpClient =>
          Future.successful(failure("bah"))
        }.await(retries = 0, timeout = Duration(10, SECONDS))
      }
    }
  }
}
