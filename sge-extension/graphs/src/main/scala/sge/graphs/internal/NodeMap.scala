/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 368
 * Covenant-baseline-methods: MinTableLength,NodeMap,ResizeThreshold,addToList,bucketHead,checkLength,clear,contains,current,currentNode,cursor,edgeIter,edges,get,getIndex,getMiddle,hasNext,hash,hashcode,head,i,insertIntoList,insertIntoListAfter,insertIntoListBefore,mergeSort,next,nodeIterator,occupiedBuckets,previousNode,put,recursiveTopologicalSort,remove,removeFromList,size,sort,sortedMerge,table,tail,threshold,topologicalSort,vertexIterator
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package graphs
package internal

import scala.annotation.nowarn
import scala.util.boundary
import scala.util.boundary.break

/** A hash structure with objects of type V as keys and Node[V] objects as values. Keys assigned to the same bucket are chained in a singly linked list. All the Node[V] objects additionally form a
  * separate doubly linked list to allow a consistent iteration order.
  */
private[graphs] class NodeMap[V](val graph: Graph[V]) {

  private val MinTableLength  = 32
  private val ResizeThreshold = 0.7f

  // array of "buckets"
  private var table: Array[Node[V]] = new Array[Node[V]](MinTableLength)

  // linked list of map entries
  var head: Node[V] = null.asInstanceOf[Node[V]] // @nowarn — linked list head
  var tail: Node[V] = null.asInstanceOf[Node[V]] // @nowarn — linked list tail

  var size: Int = 0
  @nowarn("msg=private variable was mutated but not read") // tracked for potential diagnostics, matching original design
  private var occupiedBuckets: Int = 0
  private var threshold:       Int = (ResizeThreshold * MinTableLength).toInt

  /** Return the Node[V] to which the vertex v is mapped, or null if not in the map. */
  def get(v: Any): Node[V] = boundary {
    val hash        = NodeMap.hash(v)
    val i           = getIndex(hash)
    var currentNode = table(i)
    if (currentNode == null) {
      break(null.asInstanceOf[Node[V]]) // @nowarn — null return matches original API
    }
    while (currentNode != null) {
      if (v.equals(currentNode.obj)) {
        break(currentNode)
      }
      currentNode = currentNode.nextInBucket
    }
    null.asInstanceOf[Node[V]] // @nowarn — null return matches original API
  }

  def contains(v: Any): Boolean = get(v) != null

  /** Create a Node[V] and associate the vertex v with it.
    * @return
    *   the Node[V] if v is not in the map, or null if it already is.
    */
  def put(v: V): Node[V] = boundary {
    // checking the size before adding might resize even if v is already in the map,
    // but it will only be off by one
    checkLength(1)

    val hash       = NodeMap.hash(v)
    val i          = getIndex(hash)
    var bucketHead = table(i)
    if (bucketHead == null) {
      // first in bucket
      bucketHead = Node[V](v, graph.isDirected, hash)
      bucketHead.mapHash = hash
      table(i) = bucketHead
      size += 1
      occupiedBuckets += 1
      addToList(bucketHead)
      break(bucketHead)
    }

    // find last in bucket
    var currentNode = bucketHead
    var previousNode: Node[V] = null.asInstanceOf[Node[V]] // @nowarn — traversal pointer
    while (currentNode != null) {
      if (v.equals(currentNode.obj)) {
        break(null.asInstanceOf[Node[V]]) // @nowarn — already present
      }
      previousNode = currentNode
      currentNode = currentNode.nextInBucket
    }

    currentNode = Node[V](v, graph.isDirected, hash)
    currentNode.mapHash = hash
    previousNode.nextInBucket = currentNode
    size += 1
    addToList(currentNode)
    currentNode
  }

  /** Add the node to the tail of the linked list. */
  private def addToList(node: Node[V]): Unit =
    if (head == null) {
      head = node
      tail = node
    } else {
      node.prevInOrder = tail
      tail.nextInOrder = node
      tail = node
    }

  /** Insert the node at a specific point in the linked list. */
  private def insertIntoList(v: Node[V], at: Node[V], before: Boolean): Unit =
    if (before) {
      v.nextInOrder = at
      v.prevInOrder = at.prevInOrder
      at.prevInOrder = v
      if (v.prevInOrder != null) v.prevInOrder.nextInOrder = v
      else head = v
    } else {
      v.prevInOrder = at
      v.nextInOrder = at.nextInOrder
      at.nextInOrder = v
      if (v.nextInOrder != null) v.nextInOrder.prevInOrder = v
      else tail = v
    }

  def insertIntoListAfter(v:  Node[V], at: Node[V]): Unit = insertIntoList(v, at, false)
  def insertIntoListBefore(v: Node[V], at: Node[V]): Unit = insertIntoList(v, at, true)

  /** Remove the vertex V from the map.
    * @return
    *   the Node[V] that v was associated with, or null if v is not in the map.
    */
  def remove(v: V): Node[V] = boundary {
    val hash        = NodeMap.hash(v)
    val i           = getIndex(hash)
    var currentNode = table(i)

    // node is first in bucket
    if (currentNode != null && v.equals(currentNode.obj)) {
      table(i) = currentNode.nextInBucket
      size -= 1
      removeFromList(currentNode)
      break(currentNode)
    }

    // find node
    var previousNode: Node[V] = null.asInstanceOf[Node[V]] // @nowarn — traversal pointer
    while (currentNode != null) {
      if (v.equals(currentNode.obj)) {
        if (previousNode != null) previousNode.nextInBucket = currentNode.nextInBucket
        size -= 1
        removeFromList(currentNode)
        break(currentNode)
      }
      previousNode = currentNode
      currentNode = currentNode.nextInBucket
    }

    null.asInstanceOf[Node[V]] // @nowarn — not found
  }

  /** Remove the node from the linked list. */
  def removeFromList(node: Node[V]): Unit =
    if (head eq node) {
      head = node.nextInOrder
      if (head != null) head.prevInOrder = null.asInstanceOf[Node[V]] // @nowarn — list head has no prev
    } else if (tail eq node) {
      tail = node.prevInOrder
      if (tail != null) tail.nextInOrder = null.asInstanceOf[Node[V]] // @nowarn — list tail has no next
    } else {
      node.prevInOrder.nextInOrder = node.nextInOrder
      node.nextInOrder.prevInOrder = node.prevInOrder
    }

  /** Increase the length of the table if the size exceeds the capacity. */
  private def checkLength(sizeChange: Int): Boolean =
    if (size + sizeChange > threshold) {
      occupiedBuckets = 0
      val newLength = 2 * table.length
      val oldTable  = table
      val newTable  = new Array[Node[V]](newLength)

      var i = 0
      while (i < oldTable.length) {
        if (oldTable(i) != null) {
          var tail1: Node[V] = null.asInstanceOf[Node[V]] // @nowarn — bucket rebuild pointers
          var tail2: Node[V] = null.asInstanceOf[Node[V]] // @nowarn — bucket rebuild pointers
          var current = oldTable(i)
          while (current != null) {
            val newIndex = NodeMap.getIndex(current.mapHash, newLength)
            if (newIndex == i) {
              if (tail1 == null) {
                newTable(newIndex) = current
                occupiedBuckets += 1
              } else {
                tail1.nextInBucket = current
              }
              tail1 = current
            } else {
              if (tail2 == null) {
                newTable(newIndex) = current
                occupiedBuckets += 1
              } else {
                tail2.nextInBucket = current
              }
              tail2 = current
            }
            val next = current.nextInBucket
            current.nextInBucket = null.asInstanceOf[Node[V]] // @nowarn — clear old chain
            current = next
          }
        }
        i += 1
      }
      threshold = (ResizeThreshold * newLength).toInt
      table = newTable
      true
    } else {
      false
    }

  def clear(): Unit = {
    table = new Array[Node[V]](table.length)
    size = 0
    occupiedBuckets = 0
    head = null.asInstanceOf[Node[V]] // @nowarn — clearing list
    tail = null.asInstanceOf[Node[V]] // @nowarn — clearing list
  }

  private def getIndex(hash: Int): Int = NodeMap.getIndex(hash, table.length)

  /** Iterate nodes in linked-list order. */
  def nodeIterator: Iterator[Node[V]] = new Iterator[Node[V]] {
    private var current: Node[V] = head
    def hasNext:         Boolean = current != null
    def next():          Node[V] = {
      val n = current
      current = current.nextInOrder
      n
    }
  }

  /** Iterate vertex objects in linked-list order. */
  def vertexIterator: Iterator[V] = nodeIterator.map(_.obj)

  // ================================================================================
  // Sorting (merge sort on linked list)
  // ================================================================================

  def sort(comparator: Ordering[V]): Unit =
    if (size >= 2) {
      head = mergeSort(head, comparator)

      // the sort only sets references to the next in list for each element,
      // need to iterate through and set references to previous
      var node: Node[V] = head
      var prev: Node[V] = null.asInstanceOf[Node[V]] // @nowarn — sort rebuild
      while (node != null) {
        node.prevInOrder = prev
        prev = node
        node = node.nextInOrder
      }
      tail = prev
    }

  private def mergeSort(h: Node[V], comparator: Ordering[V]): Node[V] =
    if (h == null || h.nextInOrder == null) {
      h
    } else {
      val middle     = getMiddle(h)
      val middleNext = middle.nextInOrder
      middle.nextInOrder = null.asInstanceOf[Node[V]] // @nowarn — split list

      val left  = mergeSort(h, comparator)
      val right = mergeSort(middleNext, comparator)
      sortedMerge(left, right, comparator)
    }

  private def sortedMerge(aNode: Node[V], bNode: Node[V], comparator: Ordering[V]): Node[V] =
    if (aNode == null) {
      bNode
    } else if (bNode == null) {
      aNode
    } else if (comparator.compare(aNode.obj, bNode.obj) < 0) {
      aNode.nextInOrder = sortedMerge(aNode.nextInOrder, bNode, comparator)
      aNode
    } else {
      bNode.nextInOrder = sortedMerge(aNode, bNode.nextInOrder, comparator)
      bNode
    }

  private def getMiddle(h: Node[V]): Node[V] =
    if (h == null) {
      null.asInstanceOf[Node[V]] // @nowarn — null for empty
    } else {
      var slow = h
      var fast = h
      while (fast.nextInOrder != null && fast.nextInOrder.nextInOrder != null) {
        slow = slow.nextInOrder
        fast = fast.nextInOrder.nextInOrder
      }
      slow
    }

  // ================================================================================
  // Topological sorting
  // ================================================================================

  private var cursor: Node[V] = null.asInstanceOf[Node[V]] // @nowarn — temp state for topo sort

  def topologicalSort(): Boolean =
    if (size < 2 || graph.edgeCount < 1) {
      true
    } else {
      // start the cursor at the tail and work towards the head
      cursor = tail

      var success = true
      while (success && cursor != null)
        success = recursiveTopologicalSort(cursor, graph.algorithms.requestRunID())

      cursor = null.asInstanceOf[Node[V]] // @nowarn — cleanup
      success
    }

  private def recursiveTopologicalSort(v: Node[V], runID: Int): Boolean = boundary {
    v.resetAlgorithmAttribs(runID)

    if (v.processed) {
      break(true)
    }
    if (v.seen) {
      break(false) // not a DAG
    }

    v.seen = true

    val edges    = v.outEdges
    val edgeIter = edges.iterator
    while (edgeIter.hasNext) {
      val e = edgeIter.next()
      if (!recursiveTopologicalSort(e.nodeB, runID)) {
        break(false)
      }
    }

    v.seen = false
    v.processed = true

    if (!(cursor eq v)) {
      // move v from its current position to just after the cursor
      removeFromList(v)
      insertIntoListAfter(v, cursor)
    } else {
      // v is already in the cursor position, just need to move the cursor along
      cursor = cursor.prevInOrder
    }

    true
  }
}

private[graphs] object NodeMap {

  def hash(v: Any): Int = {
    val hashcode = v.hashCode()
    hashcode ^ (hashcode >>> 16)
  }

  def getIndex(hash: Int, length: Int): Int = hash & (length - 1)
}
