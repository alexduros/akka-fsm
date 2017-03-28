import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState

import scala.reflect.ClassTag

import scala.reflect._

final case class SetNumber(num: Integer)
final case class Reset()

sealed trait State extends FSMState
case object Idle extends State {
	override def identifier: String = "Idle"
}
case object Active extends State {
	override def identifier: String = "Active"
}

sealed trait Data {
	def add(number: Integer): Data
	def empty(): Data
}
case object Empty extends Data {
	def add(number: Integer) = Numbers(Vector(number))
	def empty() = this
}
final case class Numbers(queue: Seq[Integer]) extends Data {
	def add(number: Integer) = Numbers(queue :+ number)
	def empty() = Empty
}

sealed trait DomainEvt
case class SetNumberEvt(num: Integer) extends DomainEvt
case class ResetEvt() extends DomainEvt

class Generator extends PersistentFSM[State, Data, DomainEvt] {
	override def applyEvent(domainEvent: DomainEvt, currentData: Data): Data = {
		domainEvent match {
			case SetNumberEvt(num) =>
				val data = currentData.add(num)
				println(data)
				data
			case ResetEvt() =>
				deleteMessages(1000)
				println("RESET")
				currentData.empty()
		}
	}

	override def persistenceId: String = "generator"

	override def domainEventClassTag: ClassTag[DomainEvt] = classTag[DomainEvt]

	startWith(Idle, Empty)

	when(Idle) {
		case Event(SetNumber(num), _) =>
			println("STARTING IDLE")
			goto(Active) applying SetNumberEvt(num)
		case Event(Reset, _) => goto(Active) applying ResetEvt() replying "ALREADY RESET"
	}

	when(Active) {
		case Event(SetNumber(num), numbers: Data) => stay applying SetNumberEvt(num)
		case Event(Reset, _) => goto(Idle) applying ResetEvt() replying "RESET COMPLETED"
	}
}
