package xebia.consul.client

import java.net.URL

import akka.actor._
import org.specs2.execute.ResultLike
import org.specs2.mutable.Specification
import retry.Success
import xebia.consul.client.dao.{ SprayConsulHttpClient, ServiceRegistration, ConsulHttpClient }
import xebia.consul.client.loadbalancers.{ LoadBalancer, LoadBalancerActor }
import xebia.consul.client.util._

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class ServiceBrokerIntegrationTest extends Specification with ConsulDockerContainer with RetryPolicy with TestActorSystem with Logging {

  import java.util.concurrent.TimeUnit._

  import scala.concurrent.ExecutionContext.Implicits.global

  "The ServiceBroker" should {

    "provide a usable connection to consul" in withConsulHost { (host, port) =>
      withActorSystem { implicit actorSystem =>
        val sprayHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
        // Register the HTTP interface
        sprayHttpClient.registerService(ServiceRegistration("consul-http", address = Some(host), port = Some(port)))
        val connectionProviderFactory = new ConnectionProviderFactory {
          override def create(host: String, port: Int): ConnectionProvider = new ConnectionProvider {
            logger.info(s"Asked to create connection provider for $host:$port")
            val httpClient: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
            override def destroy(): Unit = Unit

            override def returnConnection(connection: ConnectionHolder): Unit = Unit

            override def getConnection(lb: ActorRef): Future[ConnectionHolder] = Future.successful(new ConnectionHolder {
              override def connection[A]: Future[A] = Future.successful(httpClient).map(_.asInstanceOf[A])
              override val loadBalancer: ActorRef = lb
              override val key: String = "consul"
            })
          }
        }
        class NaiveLoadBalancer extends LoadBalancer {
          // Connection provider gets registered under the serviceID which is the same as the service name when omitted
          override def selectConnection: Option[String] = Some("consul-http")
        }
        val loadBalancerFactory = (f: ActorRefFactory) => f.actorOf(LoadBalancerActor.props(new NaiveLoadBalancer, "consul-http"))
        val connectionStrategy = ConnectionStrategy(connectionProviderFactory, loadBalancerFactory)
        val sut = ServiceBroker(actorSystem, sprayHttpClient, services = Map("consul-http" -> connectionStrategy))
        val success = Success[ResultLike](r => true)
        retry { () =>
          sut.withService("consul-http") { connection: ConsulHttpClient =>
            connection.findServiceChange("bogus").map(_.resource should haveSize(0))
          }
        }(success, actorSystem.dispatcher).await(0, Duration(20, SECONDS))
      }
    }
  }
}
