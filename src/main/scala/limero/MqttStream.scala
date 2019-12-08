package limero

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.alpakka.mqtt.scaladsl.{MqttSink, MqttSource}
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Merge, RunnableGraph}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, ClosedShape, Supervision}
import akka.util.{ByteString, Timeout}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import limero._
import akka.pattern.ask
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

object MqttStream {
  val log = LoggerFactory.getLogger(classOf[MqttStream])

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

    RunnableGraph.fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        ms.Src[Boolean]("remote/system/alive") ~> ms.log[Boolean]("alive") ~> ms.Dst[Boolean]("drive/speed/keepGoing")
        ms.Src[Double]("remote/remote/potLeft") ~> ms.log[Double]("potLeft") ~> ms.scale(0, 1023, -180, 180, 0.1) ~> ms.threshold(0.1) ~> ms.integer ~> ms.log[Int]("rpmTarget") ~> ms.Dst[Int]("drive/motor/rpmTarget")
        ms.Src[Double]("remote/remote/potRight")  ~> ms.scale(0, 1023, -45, +45, 1) ~> ms.negative() ~> ms.Dst[Double]("drive/servo/angleTarget")

        ClosedShape
    }).run()
  }
}

class MqttStream(url: String) {
  val log = LoggerFactory.getLogger(classOf[MqttStream])


  val connectionSettings = MqttConnectionSettings(url, "", new MemoryPersistence)
    .withAutomaticReconnect(true).withCleanSession(true)
  val weight = 0.1
  var counter = 0


  def Src[T: Reads](topic: String) = {
    MqttSource.atMostOnce(
      connectionSettings.withClientId(genClientId("Src-")),
      MqttSubscriptions(Map("src/" + topic -> MqttQoS.atMostOnce)),
      bufferSize = 1
    ).map[T](msg => {
      Json.parse(msg.payload.utf8String).as[T]
    })
  }

  def Dst[T: Writes](topic: String) = {
    Flow[T].map[MqttMessage](obj => {
      val json = Json.toJson(obj)
      MqttMessage("dst/" + topic, ByteString(Json.stringify(json)))
    }).to(
      MqttSink(connectionSettings.withClientId(genClientId("Dst-")), MqttQoS.AtMostOnce))
  }



  def genClientId(prefix: String) = {
    counter += 1
    prefix + counter
  }

  def scale(x1: Double, x2: Double, y1: Double, y2: Double) = Flow[Double].map[Double](d => {
    val r = y1 + (d - x1) * (y2 - y1) / (x2 - x1)
    (math floor r * 100) / 100
  })

  def negative() = Flow[Double].map[Double](d => {
    -d
  })
  def integer() = Flow[Double].map[Int](d => {
    d.toInt
  })


  def exponentialFilter(): Flow[Double, Double, NotUsed] =
    Flow[Double].scan(0.0)(
      (result, value) => result * (1.0 - weight) + weight * value)


  def log[T](prefix: String): Flow[T, T, NotUsed] = Flow[T].map[T](jsv => {

    log.info(prefix + " : " + jsv);
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

  def trilateration() = Flow[Anchor].map[Coordinate](anchor => {
    Coordinate(anchor.location.x, anchor.location.y)
  })

  def dummyTril() = Flow[Double].map[Coordinate](anchor => {
    Coordinate(anchor, 456)
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
