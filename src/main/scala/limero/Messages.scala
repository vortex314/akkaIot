import java.util.Date

import play.api.libs.json._
import play.api.libs.json.{JsNull, JsString, JsValue, Json}
import play.api.libs.functional.syntax._

case class Message(value: JsValue, topic: String, message: String, timestamp: Long = new Date().getTime)

case class Coordinate(x: Double, y: Double)

case class Anchor(id: Int, location: Coordinate)

case class AnchorDistance(distance: Double, anchor: Anchor)


object Coordinate {

  // def this(jsv: JsValue) = this((jsv \ "x").as[Double], (jsv \ "y").as[Double])

  implicit val coordinateReads: Reads[Coordinate] = (
    (JsPath \ "x").read[Double] and
      (JsPath \ "y").read[Double]
    ) (Coordinate.apply _)

  implicit val coordinateWrites: Writes[Coordinate] = (
    (JsPath \ "x").write[Double] and
      (JsPath \ "y").write[Double]
    ) (unlift(Coordinate.unapply))
}

object Anchor {
  implicit val anchorReads: Reads[Anchor] = (
    (JsPath \ "id").read[Int] and
      (JsPath).read[Coordinate]
    ) (Anchor.apply _)

  implicit val anchorWrites: Writes[Anchor] = (
    (JsPath \ "id").write[Int] and
      (JsPath).write[Coordinate]
    ) (unlift(Anchor.unapply))

}

object AnchorDistance {

  implicit val anchorDistanceReads: Reads[AnchorDistance] = (
    (JsPath \ "distance").read[Double] and
      (JsPath).read[Anchor]
    ) (AnchorDistance.apply _)

  implicit val anchorDistanceWrites: Writes[AnchorDistance] = (
    (JsPath \ "distance").write[Double] and
      (JsPath).write[Anchor]
    ) (unlift(AnchorDistance.unapply))
}