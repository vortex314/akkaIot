

name := "MqttStreams"

version := "0.1"

scalaVersion := "2.12.8"

resolvers += "mvnrepository" at "http://mvnrepository.com/artifact/"


libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % "1.1.2"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.7.4"
libraryDependencies ++= Seq("org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.5")
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
// https://mvnrepository.com/artifact/org.apache.commons/commons-math3
libraryDependencies += "org.apache.commons" % "commons-math3" % "3.6.1"
libraryDependencies += "junit" % "junit" % "4.13-beta-1"
