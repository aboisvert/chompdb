package chompdb.server

import chompdb._
import chompdb.store._
import chompdb.testing._
import f1lesystem.{ FileSystem, LocalFileSystem }
import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }
import java.io.IOException
import java.io.{ ObjectInputStream, ObjectOutputStream }
import java.nio.ByteBuffer
import java.util.concurrent.{ ScheduledExecutorService, TimeUnit }
import java.util.TreeMap
import scala.collection._
import scala.collection.mutable.SynchronizedSet
import scala.concurrent.duration._
import scala.reflect.runtime.universe._

import org.mockito.Mockito.{ mock, when }
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class HashRingTest extends WordSpec with ShouldMatchers {
  val Seq(node1, node2, node3) = for (i <- 1 to 3) yield Node(i.toString)
  val nodes = Set(node1, node2, node3)
  "HashRing" should {
    "calculate replicators" in {
      val h = new HashRing[Int, Node](replicationFactor = 2, nodes) {
        override def hashNode(n: Node) = n.id.toInt
        override def hashKey(k: Int) = k
      }
      h.replicators(1).toSet should be === Set(node1, node2)
      h.replicators(2).toSet should be === Set(node2, node3)
      h.replicators(3).toSet should be === Set(node3, node1)
      h.replicators(4).toSet should be === Set(node1, node2)
    }
  }
}