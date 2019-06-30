

name := "MqttStreams"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % "1.0.1"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.7.2"
libraryDependencies ++= Seq("org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.5")
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
