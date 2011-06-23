package com.excilys.ebi.gatling.http.runner

import com.excilys.ebi.gatling.core.runner.Runner
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.http.context.HttpContext
import com.excilys.ebi.gatling.http.context.builder.HttpContextBuilder._

import java.util.concurrent.TimeUnit

import akka.actor.Scheduler

object HttpRunner {
  class HttpRunner(s: Action, numUsers: Integer, ramp: Option[Integer]) extends Runner(s, numUsers, ramp) {
    def run = {
      for (i <- 1 to numberOfUsers) {
        ramp match {
          case Some(time) => Scheduler.scheduleOnce(() => s.execute(httpContext withUserId i build), (time / numberOfUsers) * i, TimeUnit.MILLISECONDS);
          case None => s.execute(httpContext withUserId i build)
        }
      }
    }
  }

  def play(s: Action, numUsers: Integer): Unit = {
    play(s, numUsers, 0)
  }

  def play(s: Action, numUsers: Integer, ramp: Integer): Unit = {
    new HttpRunner(s, numUsers, Some(ramp)).run
  }
}