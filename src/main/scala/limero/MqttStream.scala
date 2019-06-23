package be.limero

import java.util.Date
import java.util.concurrent.TimeUnit
import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.alpakka.mqtt.scaladsl.MqttSource._
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.alpakka.mqtt.scaladsl.{MqttSink, MqttSource}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, RunnableGraph}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, ClosedShape, Supervision}
import akka.util.{ByteString, Timeout}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import play.api.libs.json._
import play.api.libs.json.{JsNull, JsString, JsValue, Json}
import play.api.libs.functional.syntax._
import Messages._
import akka.pattern.ask


import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

object MqttStream {

  def main(args: Array[String]): Unit = {
    val decider: Supervision.Decider = {
      case _: Exception => Supervision.Resume
      case _ => Supervision.Stop
    }

    implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system)
      .withSupervisionStrategy(decider))
    implicit val system = ActorSystem()
    implicit val loggingAdapter = system.log
    implicit val askTimeout = Timeout(5, TimeUnit.SECONDS)


    val ms = new MqttStream("tcp://limero.ddns.net:1883");

    val jsv: JsValue = Json.parse("""{"x":10,"y":100,"id":1234,"distance":1.234,"location":{"x":10,"y":101}}""")

    val anchor = jsv.as[AnchorDistance]

    val tril: ActorRef = system.actorOf(Props[Trilateration], "trilateration")


    RunnableGraph.fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        ms.Src("remote/controller/potLeft").map(jsv => (jsv \ "value").as[Double]) ~> ms.scale(0, 1023, -5, 5, 0.1) ~> threshold(0.1) ~> ms.Dst("drive/motor/targetSpeed")
        ms.Src("remote/controller/potRight").map(jsv => (jsv \ "value").as[Double]) ~> ms.scale(0, 1023, -90, +90, 1) ~> ms.Dst("drive/steer/targetAngle")
        /*
                SrcDouble("remote/controller/potLeft") ~> scale(0, 1023, -5, 5, 0.1) ~> Dst("drive/motor/targetSpeed")

                SrcDouble("remote/controller/potRight") ~> scale(0, 1023, -40, +40, 1) ~> Dst("drive/steer/targetAngle")


        // SrcBoolean("remote/controller/buttonLeft") ~> Dst("pi2/wiring/gpio",JsObject("pin"->6,"mode"->"out","write"->bool?1:0))

        SrcDouble("remote/controller/potRight").map(m => m > 511) ~> Dst("remote/controller/ledRight")

        Src("tag/dwm1000/anchor").map(jsv => jsv.
          as[AnchorDistance]).map(anchorDistance => (tril ? anchorDistance).
          mapTo[Option[Coordinate]]).
          filter(opt => opt.isCompleted) ~> Dst("lawnmower/location/coordinate")

        Src("remote/controller/buttonLeft").map(_.as[Boolean]) ~> Dst("pi2/wiring/gpio6")

        Src("pi2/wiring/gpio6") ~> Dst("remote/controller/ledLeft")

        Src("remote/system/alive").map(_.as[Boolean]) ~> log[Boolean]("alive") ~> Dst("drive/system/keepGoing")

        Src("+/system/+").map(jsv => (jsv \ "value").as[Double]) ~> log[Double]("addOne") ~> Dst("brain/addOne")

        SrcBoolean("remote/system/alive") ~> log[Boolean]("alive") ~> Dst("drive/motor/keepGoing")
        Src("+/system/alive").filter(jsv => (jsv \ "topic").as[String].contains("remote"))

        //        Src("+/system/alive")~> replace[Boolean]("boolean",x => !(x))~>Dst("brain/system/dead")*/

        ClosedShape
    }).run()
  }
}

class MqttStream(url: String) {


  val connectionSettings = MqttConnectionSettings("tcp://limero.ddns.net:1883", "", new MemoryPersistence)
    .withAutomaticReconnect(true)
  val weight = 0.1
  var counter = 0

  def SrcDouble(topic: String) = {
    atMostOnce(
      connectionSettings.withClientId(genClientId("Src-")),
      MqttSubscriptions(Map("src/" + topic -> MqttQoS.atMostOnce)),
      bufferSize = 8
    ).map[Double](msg => msg.payload.utf8String.toDouble)
  }

  def SrcBoolean(topic: String) = {
    atMostOnce(
      connectionSettings.withClientId(genClientId("Src-")),
      MqttSubscriptions(Map("src/" + topic -> MqttQoS.atMostOnce)),
      bufferSize = 8
    ).map[Boolean](msg => msg.payload.utf8String.toBoolean)
  }

  def Src(topic: String, prefix: String = "src/") = atMostOnce(
    connectionSettings.withClientId(genClientId("Src-")),
    MqttSubscriptions(Map(prefix + topic -> MqttQoS.atMostOnce)),
    bufferSize = 8
  ).map[JsValue](msg => {
    var json = Json.obj("message" -> msg.payload.utf8String, "topic" -> msg.topic)
    val jsv = Json.parse(msg.payload.utf8String)
    json += ("value" -> jsv)
    /*      if (jsv.isInstanceOf[JsBoolean]) json += ("boolean", jsv)
          if (jsv.isInstanceOf[JsNumber]) json += ("double", jsv)
          if (jsv.isInstanceOf[JsString]) json += ("string", jsv)
          if (jsv.isInstanceOf[JsArray]) json += ("array", jsv)
          if ( jsv.isInstanceOf[JsObject]) json += ( "object" -> jsv.as[JsObject])*/
    json
  })

  def Dst(topic: String) = {
    Flow[Any].map[MqttMessage](d => MqttMessage("dst/" + topic, ByteString(d.toString))).to(
      MqttSink(connectionSettings.withClientId(genClientId("Dst-")), MqttQoS.AtLeastOnce))
  }

  def genClientId(prefix: String) = {
    counter += 1
    println(" new id " + counter)
    prefix + counter
  }

  def scale(x1: Double, x2: Double, y1: Double, y2: Double) = Flow[Double].map[Double](d => {
    val r = y1 + (d - x1) * (y2 - y1) / (x2 - x1)
    (math floor r * 100) / 100
  })

  def exponentialFilter(): Flow[Double, Double, NotUsed] =
    Flow[Double].scan(0.0)(
      (result, value) => result * (1.0 - weight) + weight * value)


  def log[T](prefix: String): Flow[T, T, NotUsed] = Flow[T].map[T](jsv => {
    println(prefix + " : " + jsv);
    jsv
  })

  def threshold(v: Double) = Flow[Double].map[Double](d => {
    if (d.abs < v) 0
    else d
  })

  def scale(x1: Double, x2: Double, y1: Double, y2: Double, step: Double) = Flow[Double].map[Double](d => {
    val r = y1 + (d - x1) * (y2 - y1) / (x2 - x1)
    Math.round((r + step / 2) / step) * step
  })

  def trilateration() = Flow[AnchorDistance].map[Coordinate](ad => {
    Coordinate(0, ad.anchor.location.y)
  })

  def medianCalculator(seq: Seq[Int]): Int = {
    //In order if you are not sure that 'seq' is sorted
    val sortedSeq = seq.sortWith(_ < _)

    if (seq.size % 2 == 1) sortedSeq(sortedSeq.size / 2)
    else {
      val (up, down) = sortedSeq.splitAt(seq.size / 2)
      (up.last + down.head) / 2
    }
  }
}
