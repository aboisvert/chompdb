package chompdb.server

import java.nio.ByteBuffer
import scala.reflect.runtime.universe._
import scala.collection._


trait Mapper[T, U] {
  /** Typically named `flatMap` but we're trying to be faithful
   *  to map-reduce naming conventions.
   */
  def map(t: T): U // Seq[U]
}

trait Reducer[T] {
  def reduce(t1: T, t2: T): T
}

trait MapReduce[T, U] extends Mapper[T, U] with Reducer[U]

/** A remoting key-value store exposing map-reduce push-down processing */
trait SlapChop {
  def mapReduce[T: TypeTag](catalog: String, database: String, keys: Seq[Long], mapReduce: MapReduce[ByteBuffer, T]): T
}
