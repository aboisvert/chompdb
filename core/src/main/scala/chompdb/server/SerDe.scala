package chompdb.server

import java.nio.ByteBuffer

trait Serializer[T] {
  def serialize(t: T): ByteBuffer 
}

trait Deserializer[T] {
  def deserialize(b: ByteBuffer): T 
}

trait SerDe[T] extends Serializer[T] with Deserializer[T]
