package chompdb.server

import java.util.Arrays
import scala.collection._
import scala.reflect.ClassTag

case class NodeNotFoundException(smth: String) extends Exception

abstract class HashRing[KEY, NODE: ClassTag](
  val replicationFactor: Int,
  val nodes: Set[NODE]
) {

  assert (replicationFactor >= 1)
  assert (nodes.size >= 1)
  assert (nodes.size > replicationFactor)

  private val (sortedReplicators: Array[NODE], sortedHashes: Array[Int]) = {
    val entriesSortedByHash = nodes.toSeq map { e => (e, hashNode(e)) } sortBy (_._2)
    val (sortedReplicators, sortedHashes) = entriesSortedByHash.unzip
    (sortedReplicators.toArray, sortedHashes.toArray)
  }

  /** Implementations need to provide Node-hashing logic. */
  def hashNode(node: NODE): Int

  /** Implementations need to provide Key-hashing logic. */
  def hashKey(key: KEY): Int

  def replicators(key: KEY): Iterator[NODE] = new Iterator[NODE] {
    private var pos = math.abs(Arrays.binarySearch(sortedHashes, hashKey(key)))
    private var left = replicationFactor
    override def hasNext: Boolean = left > 0
    override def next: NODE = {
      if (left <= 0) throw new IllegalStateException("Iterator is empty")
      if (pos >= sortedReplicators.length) pos = 0
      val result = sortedReplicators(pos)
      pos += 1
      left -= 1
      result
    }
  }
}