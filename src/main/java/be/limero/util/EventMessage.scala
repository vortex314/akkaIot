package be.limero.util

case class KV(key: String, value: Any)

object Request {
  var _staticId: Int = 0;

  def staticId = {
    _staticId += 1
    _staticId
  }

}

case class Request(dst: String, src: String, var _msg: String = "Request", id: Int = Request.staticId, var arg: Array[Any] = Array()) {
  _msg = "Request"

  def msg = _msg

  def msg_=(cls: Any): Unit = {
    _msg = cls.getClass().getSimpleName()
  }

  def toArray: Array[Any] = {
    Array(dst, src, _msg, id) ++ arg
  }

}


class Reply(dst: String, src: String, msg: String, id: Int, erc: Int, arg: Array[Any]) {
  def toArray: Array[Any] = {
    Array(dst, src, msg, id, erc) ++ arg
  }
}

class Event(src: String, msg: String, value: Any) {
  def toArray: Array[Any] = {
    Array(src, msg, value)
  }
}

case class StartRequest(var request: Request) {
  request.msg = this

  def toArray(): Array[Any] = {
    request.toArray
  }
}

case class GetRequest(dst: String, src: String, props: Array[String]) extends
  Request(dst, src, null, Request.staticId, Array()) {
}

case class ConfigRequest(request: Request, props: Array[KV] = Array()) {
  request.msg = this

  def toArray(): Array[Any] = {
    request.toArray
  }
}

object EventMessage {

  def main(args: Array[String]): Unit = {
    println(Request("AA", "BB"))
    println(StartRequest(Request("dst/xxx", "dst/yyy")))
    println(StartRequest(Request("dst/xxx", "dst/yyy")))
    println(GetRequest("DST", "SRC", Array()));
  }
}