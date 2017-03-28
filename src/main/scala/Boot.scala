import akka.actor.{ActorSystem, Props}
import akka.contrib.persistence.mongodb.{MongoReadJournal, ScalaDslMongoReadJournal}
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.Success

object Boot {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher
    implicit val timeout = Timeout(5 seconds)

    implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

    val readJournal = PersistenceQuery(system).readJournalFor[ScalaDslMongoReadJournal](MongoReadJournal.Identifier)

    val route =
      path("workflows") {
        get {
          complete {
            readJournal
              .allPersistenceIds()
          }
        } ~
        post {
          val generator = system.actorOf(Props[Generator])
          onComplete(generator ? SetNumber(10)) {
            case Success("RESET") => complete("workflow reset")
            case _ => complete((InternalServerError, s"An error occurred"))
          }
        }
      }

    Http().bindAndHandle(route, "localhost", 8081)
  }
}