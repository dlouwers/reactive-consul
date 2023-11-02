package stormlantern.consul.client.util

import org.apache.pekko.actor.ActorSystem

trait TestActorSystem {
  def withActorSystem[T](f: ActorSystem => T): T = {
    val actorSystem = ActorSystem("test")
    try {
      f(actorSystem)
    } finally {
      actorSystem.terminate()
    }
  }
}
