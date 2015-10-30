package stormlantern.consul.client.util

import odelay.Timer
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import retry._

trait RetryPolicy {
  def maxRetries = 4
  def initialDelay = 1.second
  implicit val timer = implicitly[Timer]

  def retry[T](f: => Future[T])(implicit success: Success[T], ec: ExecutionContext) = Backoff(4, 1.second)(timer)(() => f)

  def newRetry[T](times: Int, delay: FiniteDuration)(f: => Future[T]): Future[T] = {
    ???
  }
}
