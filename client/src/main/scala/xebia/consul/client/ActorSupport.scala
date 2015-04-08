package xebia.consul.client

import akka.actor.{ Props, Actor }

trait ActorSupport { this: Actor =>
  def createChild(props: Props) = this.context.actorOf(props)
}
