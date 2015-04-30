package xebia.consul.client

import java.net.URL

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.specs2.mutable.Specification
import org.specs2.specification
import xebia.consul.client.loadbalancers.LoadBalancer
import xebia.consul.client.util.ConsulDockerContainer

import scala.concurrent.Future

class ServiceBrokerIntegrationTest extends Specification with ConsulDockerContainer {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After {
    implicit val ec = system.dispatcher
    override def after: Any = system.shutdown()
  }

  "The ServiceBroker" should {
    "provide a usable connection to consul" in new ActorScope {
      withConsulHost { (host, port) =>
        val consulHttp = new SprayCatalogHttpClient(new URL(s"$host:$port"))
        val connectionProvider = new ConnectionProviderFactory {
          override def create(host: String, port: Int): ConnectionProvider = new ConnectionProvider {
            override def destroy(): Unit = ???
            override def returnConnection(connection: ConnectionHolder): Unit = ???
            override def getConnection: Future[ConnectionHolder] = ???
          }
        }
        val loadBalancer = new LoadBalancer {
          override def removeConnectionProvider(key: String): Unit = ???
          override def addConnectionProvider(key: String, provider: ConnectionProvider): Unit = ???
          override def returnConnection(connection: ConnectionHolder): Unit = ???
          override def getConnection: ConnectionHolder = ???
        }
        val services = Map("consul" -> ConnectionStrategy(connectionProvider, loadBalancer))
        val serviceBrokerActor = system.actorOf(ServiceBrokerActor.props(services, consulHttp))
        val subject = new ServiceBroker(serviceBrokerActor)
      }
    }
  }
}
