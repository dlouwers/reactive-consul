package xebia.consul.client

import java.net.URL

import akka.actor._
import org.specs2.execute.ResultLike
import org.specs2.mutable.Specification
import retry.Success
import xebia.consul.client.loadbalancers.LoadBalancerActor
import xebia.consul.client.util._

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class ServiceBrokerIntegrationTest extends Specification with ConsulRegistratorDockerContainer with RetryPolicy with TestActorSystem with Logging {

  import java.util.concurrent.TimeUnit._

  import scala.concurrent.ExecutionContext.Implicits.global

  "The ServiceBroker" should {

    "provide a usable connection to consul" in withConsulHost { (host, port) =>
      withActorSystem { implicit actorSystem =>
        val sprayHttpClient = new SprayCatalogHttpClient(new URL(s"http://$host:$port"))
        val connectionProviderFactory = new ConnectionProviderFactory {
          override def create(host: String, port: Int): ConnectionProvider = new ConnectionProvider {
            //            val httpClient: CatalogHttpClient = new SprayCatalogHttpClient(new URL(s"http://$host:$port"))
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
        val sut = ServiceBroker(actorSystem, sprayHttpClient, services = Map("consul" -> connectionStrategy))
        val success = Success[ResultLike](r => true)
        retry { () =>
          sut.withService("consul") { connection: CatalogHttpClient =>
            connection.findServiceChange("bogus").map(_.instances should haveSize(0))
          }
        }(success, actorSystem.dispatcher).await(0, Duration(20, SECONDS))
      }
    }
  }
}
