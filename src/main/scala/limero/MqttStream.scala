package limero

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.alpakka.mqtt.scaladsl.{MqttSink, MqttSource}
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, ClosedShape, Supervision}
import akka.util.{ByteString, Timeout}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json}
//import limero.Messages._
import limero._
import akka.pattern.ask


import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

object MqttStream {

  def main(args: Array[String]): Unit = {
    val decider: Supervision.Decider = {
      case _: Exception => Supervision.Resume
      case _ => Supervision.Stop
    }
    implicit val system = ActorSystem()

    implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system)
      .withSupervisionStrategy(decider))
    implicit val loggingAdapter = system.log
    implicit val askTimeout = Timeout(5, TimeUnit.SECONDS)


    val ms = new MqttStream("tcp://limero.ddns.net:1883");

    /*    val jsv: JsValue = Json.parse("""{"x":10,"y":100,"id":1234,"distance":1.234,"location":{"x":10,"y":101}}""")

        val anchor = jsv.as[AnchorDistance]*/

    val tril: ActorRef = system.actorOf(Props[Trilateration], "trilateration")


    RunnableGraph.fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        ms.Src("remote/controller/potLeft").map(jsv => (jsv \ "value").as[Double]) ~> ms.scale(0, 1023, -5, 5, 0.1) ~> ms.threshold(0.1) ~> ms.Dst("drive/motor/targetSpeed")
        ms.Src("remote/controller/potRight").map(jsv => (jsv \ "value").as[Double])~> ms.log[Double]("potRight") ~> ms.scale(0, 1023, -90, +90, 1) ~> ms.Dst("drive/steer/targetAngle")
        ms.SrcBoolean("remote/system/alive") ~> ms.log[Boolean]("alive") ~> ms.Dst("drive/motor/keepGoing")
        ClosedShape
    }).run()
  }
}

class MqttStream(url: String) {
  val log = LoggerFactory.getLogger(classOf[Trilateration])


  val connectionSettings = MqttConnectionSettings("tcp://limero.ddns.net:1883", "", new MemoryPersistence)
    .withAutomaticReconnect(true)
  val weight = 0.1
  var counter = 0

  def SrcDouble(topic: String) = {
    MqttSource.atMostOnce(
      connectionSettings.withClientId(genClientId("Src-")),
      MqttSubscriptions(Map("src/" + topic -> MqttQoS.atMostOnce)),
      bufferSize = 8
    ).map[Double](msg => msg.payload.utf8String.toDouble)
  }

  def SrcBoolean(topic: String) = {
    MqttSource.atMostOnce(
      connectionSettings.withClientId(genClientId("Src-")),
      MqttSubscriptions(Map("src/" + topic -> MqttQoS.atMostOnce)),
      bufferSize = 8
    ).map[Boolean](msg => msg.payload.utf8String.toBoolean)
  }

  def Src(topic: String, prefix: String = "src/") = MqttSource.atMostOnce(
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

    log.info( prefix + " : " + jsv);
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
