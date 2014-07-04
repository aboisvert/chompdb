package chompdb.server

trait Mapper[T, U] {
  def map(t: T): U
}

trait Reducer[T] {
  def reduce(t1: T, t2: T): T
}

trait MapReduce[T, U] extends Mapper[T, U] with Reducer[U]

