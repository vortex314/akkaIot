package be.limero.message

import com.google.gson.JsonArray

case class KV(key: String, value: Any) {
  def toArray: Array[Any] = {
    Array(key, value)
  }
}

case class Header(source: String, var id: Int, erc: Int) {
  def this(source: String, id: Int) = this(source, id, -1)

  def inc(): Header = {
    id=id+1;
    Header(source, id , erc);
  }

  def toArray: Array[Any] = {
    Array(source, id, erc)
  }

}

object Header {
  def apply(src: String, id: Int) = new Header(src, id, -1);
}

case class Event(source: String, value: Any) {
  def toArray: Array[Any] = {
    Array(source, value)
  }
}

case class GetRequest(header: Header, properties: String*) {
  def toArray: Array[Any] = {
    Array(this.getClass().getSimpleName) ++ header.toArray ++ properties
  }
}

case class GetReply(header: Header, properties: Array[KV]) {
  def toArray: Array[Any] = {
    header.toArray ++ properties.flatMap(_.toArray)
  }
}

case class SetRequest(header: Header, result: Array[KV]) {
  def toArray: Array[Any] = {
    header.toArray ++ result.flatMap(_.toArray)
  }
}

case class SetReply(header: Header, result: Array[Int]) {
  def toArray: Array[Any] = {
    header.toArray ++ result
  }
}

case class ConfigRequest(header: Header, configs: Array[KV]) {
  def toArray: Array[Any] = {
    Array(this.getClass().getSimpleName) ++ header.toArray ++ configs.flatMap(_.toArray)
  }

  override def toString: String = {
    var s = "ConfigRequest(" + header.toString;
    for (kv <- configs) {
      s += "," + kv.key + ":" + kv.value
    }
    s += ")"
    s
  }
}

case class ConfigReply(header: Header, configErcs: Int*)

case class VarArg(header: Header, arg: String*)

case class Reset(header: Header)


object Message {
  def main(args: Array[String]): Unit = {
    println("Hello from main of class")
    println(VarArg(Header("dst/esp32/system/get", 1), "upTime"))
    val a = Array("a", 1, true, 8.9, 'a');
    val keys = Array("upTime", "alive")
    println(" a=" + a);
    println(GetRequest(Header("dst", 2), "alive", "upTime"))
    println(Reset(Header("", 2)))
    println(ConfigRequest(Header("src", 12), Array(KV("key1", "value1"), KV("key2", 123))))
    val cfg = ConfigRequest(Header("src", 12), Array(KV("key1", "value1"), KV("key2", 123)))
    val v = cfg.toArray
    println(v)
  }
}

class Message {

}