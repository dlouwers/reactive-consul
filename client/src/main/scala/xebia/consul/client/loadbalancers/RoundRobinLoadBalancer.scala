package xebia.consul.client.loadbalancers

import xebia.consul.client.{ ConnectionProvider, ConnectionHolder }

import scala.collection.{ AbstractIterator, Iterator, mutable }
import scala.concurrent.Future

class RoundRobinLoadBalancer extends LoadBalancer {

  val list = new CircularLinkedHashSet[String]
  var iterator = list.iterator

  override def connectionProviderAdded(key: String): Unit = {
    list.add(key)
    iterator = list.iterator
  }

  override def connectionProviderRemoved(key: String): Unit = {
    list.remove(key)
    iterator = list.iterator
  }

  override def selectConnection: Option[String] = {
    if (iterator.hasNext) Some(iterator.next())
    else None
  }
}

class CircularLinkedHashSet[A] extends mutable.LinkedHashSet[A] {
  override def iterator: Iterator[A] = new AbstractIterator[A] {
    private var cur = firstEntry
    def hasNext = firstEntry ne null
    def next() =
      if (hasNext) {
        val res = cur.key
        if (cur.later == null)
          cur = firstEntry
        else
          cur = cur.later
        res
      } else Iterator.empty.next()
  }
}