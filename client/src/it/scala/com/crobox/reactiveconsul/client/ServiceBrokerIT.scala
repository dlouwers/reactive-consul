package com.crobox.reactiveconsul.client

import com.crobox.reactiveconsul.client.dao.pekko.PekkoHttpConsulClient
import com.crobox.reactiveconsul.client.dao.{ConsulHttpClient, ServiceRegistration}
import com.crobox.reactiveconsul.client.discovery._
import com.crobox.reactiveconsul.client.loadbalancers.RoundRobinLoadBalancer
import com.crobox.reactiveconsul.client.util.Logging

import java.net.URL
import scala.concurrent.Future

class ServiceBrokerIT extends ClientITSpec with Logging {

  "The ServiceBroker" should "provide a usable connection to consul" in {
    val pekkoHttpClient = new PekkoHttpConsulClient(new URL(s"http://$host:$port"))

    // Register the HTTP interface
    pekkoHttpClient.putService(
      ServiceRegistration("consul-http", Some("consul-http-1"), address = Some(host), port = Some(port))
    )
    pekkoHttpClient.putService(
      ServiceRegistration("consul-http", Some("consul-http-2"), address = Some(host), port = Some(port))
    )

    val connectionProviderFactory = new ConnectionProviderFactory {
      override def create(host: String, port: Int): ConnectionProvider = new ConnectionProvider {
        logger.info(s"Asked to create connection provider for $host:$port")
        val httpClient: ConsulHttpClient = new PekkoHttpConsulClient(new URL(s"http://$host:$port"))

        override def getConnection: Future[Any] = Future.successful(httpClient)
      }
    }
    val connectionStrategy = ConnectionStrategy(ServiceDefinition("consul-http"),
                                                connectionProviderFactory,
                                                new RoundRobinLoadBalancer,
                                                onlyHealthyServices = true)
    val sut = ServiceBroker(system, pekkoHttpClient, Set(connectionStrategy))
    eventually {
      sut.withService("consul-http") { connection: ConsulHttpClient =>
        connection.getService("bogus").map(_.resource should have size 0)
      }
      sut
    }
  }
}
