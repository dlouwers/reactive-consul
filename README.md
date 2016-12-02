[![Build Status](https://travis-ci.org/dlouwers/reactive-consul.svg?branch=master)](https://travis-ci.org/dlouwers/reactive-consul)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/nl.stormlantern/reactive-consul/badge.svg)](https://maven-badges.herokuapp.com/maven-central/nl.stormlantern/reactive-consul)

# Reactive Consul
This project is a Consul client for Scala. It uses non-blocking I/O to communicate with a Consul cluster. You can use
the ServiceBroker to get out of the box support for automatic-clustering, loadbalancing and failover or you can use
the low-level ConsulHttpClient.

## Requirements

Java 8

## Adding it to your project
Reactive Consul is available via [Maven Central](https://search.maven.org/), simply add it to your SBT build:

```scala
libraryDependencies += "nl.stormlantern" %% "reactive-consul" % "0.1.1"
```

If you want to use a development snapshots, use the 
[Sonatype Snapshot Repository](https://oss.sonatype.org/content/repositories/snapshots/nl/stormlantern/). Add the
following lines to your SBT build:
```scala
resolvers ++= Seq(
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies += "nl.stormlantern" %% "reactive-consul" % "0.1.1-SNAPSHOT"
```

## Using the ServiceBroker
The ServiceBroker can be used as follows:

```scala
import stormlantern.consul.client.ServiceBroker

val serviceBroker = ServiceBroker("localhost", connectionStrategies)

val result = serviceBroker.withService("<service_name>") { myServiceConnection =>
  myServiceConnection.getData()
}
```

_connectionStrategies_ are discussed in the next section.

## Creating a ConnectionStrategy
A connection strategy can optionally encapsulate connectionpooling and provides loadbalancing but it can be really 
straightforward, especially if underlying services do most of the work for you. 

### Example for MongoDB using Casbah
The following example will create a connection strategy for connecting to MongoDB. MongoDB manages replication and 
sharding for you and will automatically route your query to the right instance, but you will have to connect to a node 
first. Consul can help you keep track of these.

```scala
import stormlantern.consul.client.discovery.ConnectionProvider
import stormlantern.consul.client.discovery.ConnectionStrategy
import stormlantern.consul.client.ServiceBroker

import com.mongodb.casbah.Imports._

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
  def withService[T]: (MongoClient => Future[T]) => Future[T] 
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

### Example for Postgres using c3p0 connection pooling
The following example will create a connection strategy for connecting to Postgres. This example assumes a setup with
one master and two replication servers. Consul can help you keep track of these.

```scala
import stormlantern.consul.client.discovery.ConnectionProvider
import stormlantern.consul.client.discovery.ConnectionStrategy
import stormlantern.consul.client.ServiceBroker

import scala.concurrent.Future
import java.sql.Connection
import com.mchange.v2.c3p0._


val c3p0ConnectionProvider = (host: String, port: Int) => new ConnectionProvider {
  val pool = {
    val cpds = new ComboPooledDataSource()
    cpds.setDriverClass("org.postgresql.Driver")            
    cpds.setJdbcUrl(s"jdbc:postgresql://$host:$port/mydb")
    cpds.setUser("dbuser")                                  
    cpds.setPassword("dbpassword")
    cpds
  }
  override def getConnection: Future[Any] = Future.successful(pool.getConnection())
  override def returnConnection(connectionHolder: ConnectionHolder): Unit = 
    connectionHolder.connection.foreach(_.asInstanceOf[Connection].close())
  override def destroy(): Unit = pool.close()
}

val postgresReadConnectionStrategy = ConnectionStrategy(
  ServiceDefinition("postgres-read", "postgres")), 
  c3p0ConnectionProvider,
  new RoundRobinLoadBalancer
)
val postgresWriteConnectionStrategy = ConnectionStrategy(
  ServiceDefinition("postgres-write", "postgres", Set("master"), 
  c3p0ConnectionProvider
  new RoundRobinLoadBalancer  
)
val serviceBroker = ServiceBroker("consul-http", Set(postgresReadConnectionStrategy, postgresWriteConnectionStrategy))
```

This example assumes that you have Consul available through DNS and that you have registered Consul's HTTP interface
under the service name "consul-http", your Postgres instances as "postgres" and your Postgres master is tagged as "master".
Consul's tag support is used to identify the postgres master, all writes are sent to it. Reads can go to any postgres instance.

Now you can connect to your database using:

```scala
class PostgresServiceProvider(serviceBroker: ServiceBroker) {
    def withReadingConnection[T] = serviceBroker.withService[Connection, T]("postgres")
    def withWritingConnection[T] = serviceBroker.withService[Connection, T]("postgres-master")
}

class MyDao(connectionProvider: PostgresServiceProvider) {
  def findStuff(name: String): Future[Option[Stuff]]] = {
    connectionProvider.withReadingConnection { c  =>
      Future.successful {
        c.doSqlStuff()
      }
    }
  }
}

val myDao = new MyDao(new PostgresService(serviceBroker))
```
