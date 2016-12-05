package stormlantern.consul.client.util

import akka.actor.ActorSystem

trait TestActorSystem {
  def withActorSystem[T](f: ActorSystem ⇒ T): T = {
    val actorSystem = ActorSystem("test")
    try {
      f(actorSystem)
    } finally {
      actorSystem.terminate()
    }
  }
}
