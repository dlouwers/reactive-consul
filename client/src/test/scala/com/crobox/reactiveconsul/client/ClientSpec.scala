package com.crobox.reactiveconsul.client

import com.crobox.reactiveconsul.client.helpers.CallingThreadExecutionContext
import com.crobox.reactiveconsul.client.util.Logging
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class ClientSpec(val config: Config = ConfigFactory.load())
    extends TestKit(ActorSystem("TestSystem", config))
    with ImplicitSender
    with AnyFlatSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll
    with MockFactory
    with Logging {

  implicit val ec = CallingThreadExecutionContext()
}
