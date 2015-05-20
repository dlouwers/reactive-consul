package xebia.consul.client

import java.net.URL
import java.util.concurrent.TimeUnit._

import akka.actor._
import akka.testkit.{ TestActorRef, TestKit }
import akka.util.Timeout
import org.specs2.execute.{ ResultLike, AsResult }
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import org.specs2.specification
import retry.Success
import xebia.consul.client.loadbalancers.LoadBalancerActor
import xebia.consul.client.util.{ RetryPolicy, Logging }
import xebia.dockertestkit.ConsulDockerContainer

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

class ServiceBrokerIntegrationTest extends Specification with ConsulDockerContainer with Logging {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After {
    implicit val ec = system.dispatcher
    override def after: Any = system.shutdown()
  }

  "The ServiceBroker" should {

    "provide a usable connection to consul" in withConsulHost { (host, port) =>
      new ActorScope with RetryPolicy {
        val sprayHttpClient = new SprayCatalogHttpClient(new URL(s"http://$host:$port"))
        val connectionProviderFactory = new ConnectionProviderFactory {
          override def create(host: String, port: Int): ConnectionProvider = new ConnectionProvider {
            // The provided host and port are not correct since they are running inside of docker
            // TODO: See if this can be solved using registrator
            // val httpClient: CatalogHttpClient = new SprayCatalogHttpClient(new URL(s"http://$host:$port"))
            val httpClient: CatalogHttpClient = sprayHttpClient
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
          override def serviceName = "consul"
          override def selectConnection: Option[Future[ConnectionHolder]] = connectionProviders.get("consul").map(_.getConnection(self))
        }
        val loadBalancerFactory = (f: ActorRefFactory) => f.actorOf(Props(new NaiveLoadBalancer))
        val connectionStrategy = ConnectionStrategy(connectionProviderFactory, loadBalancerFactory)
        val sut = ServiceBroker(system, sprayHttpClient, services = Map("consul" -> connectionStrategy))
        val success = Success[ResultLike](r => true)
        val result = retry { () =>
          sut.withService("consul") { connection: CatalogHttpClient =>
            connection.findServiceChange("bogus").map(_.instances should haveSize(0))
          }
        }(success, ec).await(0, Duration(20, SECONDS))
      }
    }
  }
}
