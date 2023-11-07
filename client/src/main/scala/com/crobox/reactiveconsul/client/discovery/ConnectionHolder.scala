package com.crobox.reactiveconsul.client.discovery

import org.apache.pekko.actor.ActorRef

import scala.concurrent.Future

trait ConnectionHolder {
  def id: String
  def loadBalancer: ActorRef
  def connection: Future[Any]
}
