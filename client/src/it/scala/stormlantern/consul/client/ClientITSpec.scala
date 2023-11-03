package stormlantern.consul.client

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.TestKit
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

abstract class ClientITSpec(val config: Config = ConfigFactory.load())
    extends TestKit(ActorSystem("TestSystem", config))
    with AnyFlatSpecLike
    with Matchers
    with ScalaFutures
    with Eventually
    with IntegrationPatience {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val host = "localhost"
  val port = 8500
}
