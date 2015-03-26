package io.gatling.core.result.writer

import akka.actor.FSM
import io.gatling.core.akka.BaseActor
import io.gatling.core.config.GatlingConfiguration

private[writer] trait DataWriterFSM extends BaseActor with FSM[DataWriterState, DataWriterData]

private[writer] sealed trait DataWriterState
case object Uninitialized extends DataWriterState
case object Initialized extends DataWriterState
case object Terminated extends DataWriterState

trait DataWriterData
case object NoData extends DataWriterData
case class InitData(configuration: GatlingConfiguration) extends DataWriterData
