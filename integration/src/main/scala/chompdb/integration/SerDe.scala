package chompdb.integration

import java.nio.ByteBuffer
import chompdb.server.MapReduce

trait Serializer[T] {
  def serialize(t: T): ByteBuffer
}

trait Deserializer[T] {
  def deserialize(b: ByteBuffer): T
}

trait SerDe[T] extends Serializer[T] with Deserializer[T]

trait SerializableMapReduce[T, U] extends MapReduce[T, U] {
  /** Serializer-deserializer for this instance */
  def serde: SerDe[MapReduce[T, U]]

  /** Serializer-deserializer for type parameter T */
  def serde_T: SerDe[T]

  /** Serializer-deserializer for type parameter U */
  def serde_U: SerDe[U]
}
