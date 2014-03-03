package chompdb.server

import scala.collection._
import org.scalatest._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class HashRingTest extends WordSpec with ShouldMatchers {
  val Seq(node1, node2, node3) = for (i <- 1 to 3) yield Node(i.toString)
  val nodes = Set(node1, node2, node3)

  "HashRing" should {

    "calculate replicators with replicationFactor = 1" in {
      val h = new HashRing[Int, Node](replicationFactor = 1, nodes) {
        override def hashNode(n: Node) = n.id.toInt
        override def hashKey(k: Int) = k
      }
      h.replicators(1).toSeq should === (Seq(node1))
      h.replicators(2).toSeq should === (Seq(node2))
      h.replicators(3).toSeq should === (Seq(node3))
      h.replicators(4).toSeq should === (Seq(node1)) // loop around
    }

    "calculate replicators with replicationFactor = 2" in {
      val h = new HashRing[Int, Node](replicationFactor = 2, nodes) {
        override def hashNode(n: Node) = n.id.toInt
        override def hashKey(k: Int) = k
      }
      h.replicators(1).toSeq should === (Seq(node1, node2))
      h.replicators(2).toSeq should === (Seq(node2, node3))
      h.replicators(3).toSeq should === (Seq(node3, node1)) // loop around
      h.replicators(4).toSeq should === (Seq(node1, node2))
    }

    "calculate replicators with replicationFactor = 3" in {
      val h = new HashRing[Int, Node](replicationFactor = 3, nodes) {
        override def hashNode(n: Node) = n.id.toInt
        override def hashKey(k: Int) = k
      }
      h.replicators(1).toSeq should === (Seq(node1, node2, node3))
      h.replicators(2).toSeq should === (Seq(node2, node3, node1)) // loop around
      h.replicators(3).toSeq should === (Seq(node3, node1, node2))
      h.replicators(4).toSeq should === (Seq(node1, node2, node3))
    }
  }
}