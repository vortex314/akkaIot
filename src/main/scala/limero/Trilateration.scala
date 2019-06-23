package be.limero

import akka.actor.Actor
import akka.event.Logging


class Trilateration extends Actor {
//  var anchors= new Map[Int,AnchorDistance]()
  val log = Logging(context.system, this)
  def receive = {
    case ad:AnchorDistance ⇒ {
      log.info("received test")
      sender() ! None
    }
    case _      ⇒ {
      log.info("received unknown message")
      sender() ! None
    }
  }
}