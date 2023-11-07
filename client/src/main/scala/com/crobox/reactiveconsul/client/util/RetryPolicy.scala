package com.crobox.reactiveconsul.client.util

import scala.concurrent._
import scala.concurrent.duration._

import org.apache.pekko.actor.Scheduler
import org.apache.pekko.pattern.after

trait RetryPolicy {
  def maxRetries = 4
  def retry[T](
    delay: FiniteDuration = 500.milli,
    retries: Int = 4,
    backoff: Int = 2,
    predicate: T => Boolean = (r: T) => true
  )(f: => Future[T])(implicit ec: ExecutionContext, s: Scheduler): Future[T] = {
    f.map {
      case r if !predicate(r) => throw new IllegalStateException("Result does not satisfy the predicate specified")
      case r                  => r
    } recoverWith { case _ if retries > 0 => after(delay, s)(retry(delay * backoff, retries - 1, backoff, predicate)(f)) }
  }
}
