package limero
import scala.concurrent.duration._
import java.util.Date

import akka.actor.{Actor, Timers}
import akka.event.Logging
import org.slf4j.LoggerFactory


case class Measurement(anchor:String,x:Double,y:Double,distance:Double,time:Long)
case class ExpireTimer()





class Trilateration extends Actor with Timers {
  val log = LoggerFactory.getLogger(classOf[Trilateration])

  //  var anchors= new Map[Int,AnchorDistance]()
  log.info("Actor Trilateration started.")
  timers.startPeriodicTimer("expireTimer",ExpireTimer,1000.millis)

//  val log = Logging(context.system, this)
  var measurements=Map[String,Measurement]()

  def expire(timeoutMsec:Int) = {
    val expireTime = new Date().getTime-timeoutMsec
    measurements.foreach[Any](entry=>{
      if ( entry._2.time < expireTime) measurements =  measurements - entry._1
    })
  }

  override def preStart() = {
  }

  def receive = {
    case ad: AnchorDistance ⇒ {
      log.info("received test")
      sender() ! None
    }
    case ExpireTimer => {
      expire(3000)
    }
    case _ ⇒ {
      log.info("received unknown message")
      sender() ! None
    }
  }
}