package com.gu.memsub.util

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.pattern.after
import akka.actor.Scheduler

/**
 * retry implementation from Scala Future contributor
 * https://gist.github.com/viktorklang/9414163
 */
object FutureRetry {
  /**
   * Given an operation that produces a T, returns a Future containing the result of T, unless an exception is thrown,
   * in which case the operation will be retried after _delay_ time, if there are more possible retries, which is configured through
   * the _retries_ parameter. If the operation does not succeed and there is no retries left, the resulting Future will contain the last failure.
   **/
  def retry[T](op: => Future[T], delay: FiniteDuration, retries: Int)(implicit ec: ExecutionContext, s: Scheduler): Future[T] =
    op recoverWith { case _ if retries > 0 => after(delay, s)(retry(op, delay, retries - 1)) }

  def retry[T](op: => Future[T])(implicit ec: ExecutionContext, s: Scheduler): Future[T] =
    retry(op, delay = 200.milliseconds, retries = 2)
}
