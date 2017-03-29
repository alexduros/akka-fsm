name := """akka-fsm"""

version := "1.0"

scalaVersion := "2.11.8"

fork in run := false

val akkaVersion = "2.4.16"

val akkaHttpVersion = "10.0.5"

libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-actor" % akkaVersion,
	"com.typesafe.akka" %% "akka-persistence" % akkaVersion,
	"com.typesafe.akka" %% "akka-persistence-query-experimental" % akkaVersion,

	"com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
	"com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
	"com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
	"com.typesafe.akka" %% "akka-http-jackson" % akkaHttpVersion,
	"com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.11.1",

	"com.github.scullxbones" %% "akka-persistence-mongo-casbah" % "1.4.1",

	"org.mongodb" % "mongodb-driver" % "3.4.2",
	"org.mongodb" %% "casbah" % "3.1.1"
)

resolvers += "spray repo" at "http://repo.spray.io"