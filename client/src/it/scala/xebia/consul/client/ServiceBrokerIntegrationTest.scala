package xebia.consul.client

import java.net.URL
import java.util.concurrent.TimeUnit._

import akka.actor._
import akka.testkit.TestKit
import org.specs2.execute.Result
import org.specs2.mutable.Specification
import org.specs2.specification
import xebia.consul.client.loadbalancers.LoadBalancerActor
import xebia.consul.client.util.{ ConsulDockerContainer, Logging }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

class ServiceBrokerIntegrationTest extends Specification with ConsulDockerContainer with Logging {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After {
    implicit val ec = system.dispatcher
    override def after: Any = system.shutdown()
  }

  "The ServiceBroker" should {
    "provide a usable connection to consul" in withConsulHost { (host, port) =>
      new ActorScope {
        val connectionProviderFactory = new ConnectionProviderFactory {
          override def create(host: String, port: Int): ConnectionProvider = new ConnectionProvider {
            val httpClient: CatalogHttpClient = new SprayCatalogHttpClient(new URL(s"http://$host:$port"))
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
          override def selectConnection: Option[Future[ConnectionHolder]] = connectionProviders.get("consul").map(_.getConnection(self))
        }
        val loadBalancerFactory = (f: ActorRefFactory) => f.actorOf(Props(new NaiveLoadBalancer))
        val httpClient = new SprayCatalogHttpClient(new URL(s"http://$host:$port"))
        val connectionStrategy = ConnectionStrategy(connectionProviderFactory, loadBalancerFactory)
        val sut = ServiceBroker(system, httpClient, services = Map("consul" -> connectionStrategy))
        val result = sut.withService("consul") { connection: CatalogHttpClient =>
          connection.findServiceChange("bogus").map(_ should beAnInstanceOf[IndexedServiceInstances])
        }
        logger.error(result.toString)
        Await.result(result, Duration("10s")) should beAnInstanceOf[String]
        false shouldEqual true
      }
    }
  }
}
