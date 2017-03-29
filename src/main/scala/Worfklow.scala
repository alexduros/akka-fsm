import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState

import scala.reflect.{ClassTag, classTag}

/*
 * Copyright (c) 2016. Canal+ Group
 * All rights reserved
 */

final case class SetState(state: String)
final case class Pause()
final case class Start()
final case class Resume()
final case class Finish()

sealed trait State extends FSMState
case object Idle extends State {
  override def identifier: String = "Idle"
}
case object IngestCreation extends State {
  override def identifier: String = "IngestCreation"
}

sealed trait Data {
  def setState(state: String): Data
  def empty(): Data
}
case object Empty extends Data {
  def setState(state: String) = WorkflowStatus(state)
  def empty() = this
}
final case class WorkflowStatus(state: String) extends Data {
  def setState(state: String) = WorkflowStatus(state)
  def empty() = Empty
}

sealed trait DomainEvt
case class SetWorkflowStatus(state: String) extends DomainEvt

final case class WorkflowDump()

case class Workflow(workflowId: String) extends PersistentFSM[State, Data, DomainEvt] {
  override def applyEvent(domainEvent: DomainEvt, currentData: Data): Data = {
    domainEvent match {
      case SetWorkflowStatus(state) =>
        val data = currentData.setState(state)
        println(data)
        data
    }
  }

  override def persistenceId: String = "workflow-" + workflowId

  override def domainEventClassTag: ClassTag[DomainEvt] = classTag[DomainEvt]

  startWith(Idle, Empty)

  when(Idle) {
    case Event(Start, _) =>
      goto(IngestCreation) replying "WORKFLOW STARTED" applying SetWorkflowStatus("INPROGRESS")
  }

  when(IngestCreation) {
    case Event(Pause, _) => stay applying SetWorkflowStatus("PAUSED")
    case Event(Resume, _) => stay applying SetWorkflowStatus("INPROGRESS")
    case Event(Finish, _) => goto(Idle) applying SetWorkflowStatus("FINISHED")
  }

  whenUnhandled {
    case Event(WorkflowDump, _) => stay replying stateData
  }
}