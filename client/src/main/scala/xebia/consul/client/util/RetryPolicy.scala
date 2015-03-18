package xebia.consul.client.util

import odelay.Timer
import scala.concurrent.duration._
import retry._

trait RetryPolicy {
  def maxRetries = 4
  def initialDelay = 1.second
  implicit val timer = implicitly[Timer]

  def retry = Backoff(4, 1.second)
}
