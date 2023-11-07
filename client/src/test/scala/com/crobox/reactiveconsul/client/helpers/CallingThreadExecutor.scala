package com.crobox.reactiveconsul.client.helpers

import java.util.concurrent.Executor

import scala.concurrent.ExecutionContext

class CallingThreadExecutor extends Executor {
  override def execute(command: Runnable): Unit = command.run()
}

object CallingThreadExecutionContext {
  def apply(): ExecutionContext = ExecutionContext.fromExecutor(new CallingThreadExecutor)
}
