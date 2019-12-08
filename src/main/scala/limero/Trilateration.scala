package limero
import scala.concurrent.duration._
import java.util.Date

import akka.actor.{Actor, Timers}
import akka.event.Logging
import org.slf4j.LoggerFactory
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.RealVector

/*
https://github.com/lemmingapex/trilateration
 */

case class Measurement(anchor:String,x:Double,y:Double,distance:Double,time:Long)
case class ExpireTimer()

object Trilateration {
  def main(args:Array[String])={

      val positions = Array[Array[Double]](Array(5.0, -6.0), Array(13.0, -15.0), Array(21.0, -3.0), Array(12.4, -21.2))
      val distances = Array[Double](8.06, 13.97, 23.32, 15.31)



      val solver:NonLinearLeastSquaresSolver  = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
      val optimum:Optimum = solver.solve()

      // the answer
      val  centroid :Array[Double]= optimum.getPoint().toArray()

      // error and geometry information; may throw SingularMatrixException depending the threshold argument provided
      val standardDeviation :RealVector= optimum.getSigma(0);
      val covarianceMatrix :RealMatrix= optimum.getCovariances(0);
  }
}



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