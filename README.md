[![Build Status](https://travis-ci.org/dlouwers/reactive-consul.svg?branch=master)](https://travis-ci.org/dlouwers/reactive-consul)
[![Coverage Status](https://coveralls.io/repos/dlouwers/reactive-consul/badge.svg)](https://coveralls.io/r/dlouwers/reactive-consul)

# Reactive Consul
This project is a Consul client for Scala. It uses non-blocking I/O to communicate with a Docker cluster. You can use
the ServiceBroker to get out of the bock support for automatic-clustering, loadbalancing and failover or you can use
the low-lever ConsulHttpClient.

## Installation
Repo on Maven Central pending.

## Using the ServiceBroker
The ServiceBroker can be used as follows:

```scala
import stormlantern.consul.client.ServiceBroker

val serviceBroker = ServiceBroker("localhost", services)

val result = serviceBroker.withService("<service_name>") { myServiceConnection =>
  myServiceConnection.getData()
}
```

Services need to be specified through a ConnectionStrategy.

## Creating a ConnectionStrategy
A connection strategy can optionally encapsulate connectionpooling and provides loadbalancing but it can be really 
straightforward, especially if underlying services do most of the work for you. 

### Example for MongoDB using Casbah
The following example will create a 
connection strategy for connecting to MongoDB. MongoDB manages replication and sharding for you and will automatically
route your query to the right instance, but you will have to connect to a node first. Consul can help you keep track
of these.

```scala
import stormlantern.consul.client.ConnectionProvider
import stormlantern.consul.client.ConnectionStrategy
import stormlantern.consul.client.ServiceBroker

val mongoConnectionProvider = (host: String, port: Int) => new ConnectionProvider {
  val client = new MongoClient(host, port)
  override def getConnection: Future[Any] = Future.successful(client)
}
val mongoConnectionStrategy = ConnectionStrategy("mongodb", mongoConnectionProvider)
val serviceBroker = ServiceBroker("consul-http", Set(mongoConnectionStrategy))
```
This example assumes that you have Consul available through DNS and that you have registered Consul's HTTP interface
under the service name "consul-http" and your MongoDB instances as "mongodb".

Instead of passing the full serviceBroker to your MongoDB DAO implementation you could declare your DAO implementations
as a trait and then have them implement to following trait:
```scala
trait MongoDbService {  
  def withService[T]: (String => Future[T]) => Future[T] 
}
```
Then your MongoDB DAO implementations can be instantated as such:
```scala
val myMongoDAO = new MyMongoDAO {
  def withService[T] = serviceBroker.withService[MongoClient, T]("mongodb")     
}
```
Or, more traditionally:
```scala
class MongoDbServiceProvider(serviceBroker: ServiceBroker) {
    def withService[T] = serviceBroker.withService[MongoClient, T]("mongodb")
}
```
and pass an instance of it to your MongoDB DAO implementation.

More to follow.