import akka.actor.{ActorSystem, Props}
import akka.contrib.persistence.mongodb.{MongoReadJournal, ScalaDslMongoReadJournal}
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._
import scala.util.{Failure, Success}

final case class NumberOrder(number: Int)

object Boot extends SprayJsonSupport with DefaultJsonProtocol {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher
    implicit val timeout = Timeout(5 seconds)

    implicit val numberFormat = jsonFormat1(NumberOrder)

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
          onComplete(generator ? Reset) {
            case Success(_) => complete("workflow reset")
            case Failure(ex) => complete((InternalServerError, s"An error occurred ${ex}"))
          }
        }
      } ~
        path("workflows" / "numbers") {
          post {
            val generator = system.actorOf(Props[Generator])
            entity(as[NumberOrder]) { order =>
              generator ! SetNumber(order.number)
              complete("data added")
            }
          }
        }

    Http().bindAndHandle(route, "localhost", 8081)
  }
}