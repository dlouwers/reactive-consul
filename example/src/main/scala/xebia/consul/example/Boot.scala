package xebia.consul.example

import java.net.URL

import akka.actor.{ActorRef, ActorSystem}
import akka.io.IO
import akka.pattern._
import akka.util.Timeout
import spray.can.Http
import xebia.consul.client._
import xebia.consul.client.loadbalancers.{LoadBalancer, RoundRobinLoadBalancer}

import scala.concurrent.Future
import scala.concurrent.duration._

object Boot extends App {
  implicit val system = ActorSystem("reactive-consul")
  implicit val executionContext = system.dispatcher

  val service = system.actorOf(ReactiveConsulHttpServiceActor.props(), "webservice")

  implicit val timeout = Timeout(5.seconds)

  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
  val consulNode = new URL("http://192.168.59.103:8500")

  def cpf(k: String) = (host: String, port: Int) => new ConnectionProvider {
    val client = new SprayExampleServiceClient(new URL(s"http://$host:$port"))
    override def destroy(): Unit = ()
    override def returnConnection(connection: ConnectionHolder): Unit = ()
    override def getConnection(lb: ActorRef): Future[ConnectionHolder] = Future.successful(new ConnectionHolder {
      override def connection[A]: Future[A] = Future.successful(client).map(_.asInstanceOf[A])
      override val loadBalancer: ActorRef = lb
      override val key: String = k
    })
  }
  val connectionStrategy1 = ConnectionStrategy("example-service-1", cpf("example-service-1"), new RoundRobinLoadBalancer)
  val connectionStrategy2 = ConnectionStrategy("example-service-2", cpf("example-service-2"), new RoundRobinLoadBalancer)
  val services = Map(
    "example-service-1" -> connectionStrategy1,
    "example-service-2" -> connectionStrategy2
  )
  val serviceBroker = ServiceBroker(services, consulNode.getHost, consulNode.getPort)
  while (true) {
    Thread.sleep(5000)
    serviceBroker.withService("example-service-1") { client: SprayExampleServiceClient =>
      client.identify
    }.foreach(println)
    serviceBroker.withService("example-service-2") { client: SprayExampleServiceClient =>
      client.identify
    }.foreach(println)
  }
}
