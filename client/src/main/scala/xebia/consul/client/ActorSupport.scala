package xebia.consul.client

import akka.actor.{ ActorLogging, Props, Actor }

trait ActorSupport extends ActorLogging { this: Actor =>
  def createChild(props: Props) = this.context.actorOf(props)
}
