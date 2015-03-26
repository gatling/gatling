package io.gatling.core.result.writer

import akka.actor.FSM
import io.gatling.core.akka.BaseActor
import io.gatling.core.config.GatlingConfiguration

private[writer] trait DataWriterFSM extends BaseActor with FSM[DataWriterState, DataWriterData]

private[writer] sealed trait DataWriterState
private[writer] case object Uninitialized extends DataWriterState
private[writer] case object Initialized extends DataWriterState
private[writer] case object Terminated extends DataWriterState

trait DataWriterData
private[writer] case object NoData extends DataWriterData
case class InitData(configuration: GatlingConfiguration) extends DataWriterData
