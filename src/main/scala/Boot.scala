import akka.actor.{ActorSystem, Props}
import akka.contrib.persistence.mongodb.{MongoReadJournal, ScalaDslMongoReadJournal}
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{Marshaller, Marshalling}
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import akka.util.{ByteString, Timeout}
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

final case class WorkflowModel(workflowId: Int)

object Boot extends SprayJsonSupport with DefaultJsonProtocol {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher
    implicit val timeout = Timeout(5 seconds)

    implicit val numberFormat = jsonFormat1(WorkflowModel)

    implicit val jsonEntityStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

    implicit val stringFormat = Marshaller[String, ByteString] { ec ⇒ s ⇒
      Future.successful {
        List(Marshalling.WithFixedContentType(ContentTypes.`application/json`, () ⇒
          ByteString("\"" + s + "\"")) // "raw string" to be rendered as json element in our stream must be enclosed by ""
        )
      }
    }

    val readJournal = PersistenceQuery(system).readJournalFor[ScalaDslMongoReadJournal](MongoReadJournal.Identifier)

    val route =
      path("workflows") {
        get {
          complete(readJournal.currentPersistenceIds())
        } ~
        post {
          val uuid = java.util.UUID.randomUUID()
          val workflow = system.actorOf(Props(classOf[Workflow], uuid.toString))
          onComplete(workflow ? Start) {
            case Success(_) => complete("workflow created and started")
            case Failure(ex) => complete((InternalServerError, s"An error occurred ${ex}"))
          }
        }
      } ~
        path("workflows" / Segment / "pause") { workflowId =>
          post {
            val workflow = system.actorOf(Props(classOf[Workflow], workflowId.toString))
            workflow ! Pause
            complete("workflow successfully paused")
          }
        }

    Http().bindAndHandle(route, "localhost", 8081)
  }
}