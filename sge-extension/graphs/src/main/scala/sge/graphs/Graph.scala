/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs

import scala.collection.mutable.LinkedHashMap

import sge.graphs.algorithms.Algorithms
import sge.graphs.internal.NodeMap
import sge.graphs.utils.WeightFunction

/** Abstract graph with node/edge management. Subclassed by DirectedGraph and UndirectedGraph. */
abstract class Graph[V] {

  // ================================================================================
  // Members
  // ================================================================================

  private[graphs] val nodeMap: NodeMap[V] = NodeMap[V](this)

  /** This is a map so that for undirected graphs, a consistent edge instance can be obtained from either (u, v) or (v, u)
    */
  private[graphs] val edgeMap: LinkedHashMap[Connection[V], Connection[V]] = LinkedHashMap.empty

  private var _defaultEdgeWeight: WeightFunction[V] = new WeightFunction[V] {
    def getWeight(a: V, b: V): Float = 1f
  }

  // ================================================================================
  // Constructors (secondary)
  // ================================================================================

  private[graphs] def initVertices(vertices: Iterable[V]): Unit =
    vertices.foreach(addVertex)

  private[graphs] def initFromGraph(graph: Graph[V]): Unit = {
    initVertices(graph.vertices)
    graph.edges.foreach(e => addEdge(e))
  }

  // ================================================================================
  // Abstract Methods
  // ================================================================================

  private[graphs] def obtainEdge(): Connection[V]

  def createNew(): Graph[V]

  def algorithms: Algorithms[V]

  // ================================================================================
  // Public Methods
  // ================================================================================

  /** Adds a vertex to the graph.
    * @return
    *   true if the vertex was not already in the graph, false otherwise
    */
  def addVertex(v: V): Boolean = nodeMap.put(v) != null

  /** Adds all the vertices in the collection to the graph. */
  def addVertices(vertices: Iterable[V]): Unit = vertices.foreach(addVertex)

  def addVertices(vertices: V*): Unit = vertices.foreach(addVertex)

  /** Removes a vertex from the graph, and any adjacent edges.
    * @return
    *   true if the vertex was in the graph, false otherwise
    */
  def removeVertex(v: V): Boolean = {
    val existing = nodeMap.remove(v)
    if (existing == null) {
      false
    } else {
      disconnectNode(existing)
      true
    }
  }

  def disconnect(v: V): Unit = {
    val existing = nodeMap.get(v)
    if (existing == null) throw IllegalArgumentException("Vertex is not in the graph")
    disconnectNode(existing)
  }

  protected def disconnectNode(node: Node[V]): Unit = {
    var i = node.outEdges.size - 1
    while (i >= 0) {
      removeConnection(node, node.outEdges.get(i).nodeB)
      i -= 1
    }
    if (node.inEdges != null) {
      i = node.inEdges.size - 1
      while (i >= 0) {
        removeConnection(node.inEdges.get(i).nodeA, node)
        i -= 1
      }
    }
    node.disconnect()
  }

  /** Removes all the vertices in the collection from the graph, and any adjacent edges. */
  def removeVertices(vertices: Iterable[V]): Unit = vertices.foreach(removeVertex)

  def removeVertexIf(predicate: V => Boolean): Unit =
    removeVertices(vertices.filter(predicate).toList)

  /** Add an edge to the graph, from v to w. The edge will have a default weight of 1. If there is already an edge between v and w, its weight will be set to 1.
    */
  def addEdge(v: V, w: V): Connection[V] = addEdge(v, w, defaultEdgeWeightFunction)

  /** Add an edge to the graph, with the same endpoints as the given edge. If the endpoints are not in the graph they will be added. If there is already an edge between v and w, its weight will be set
    * to the weight of given edge.
    */
  def addEdge(edge: Edge[V]): Connection[V] = {
    addVertex(edge.a)
    addVertex(edge.b)
    addEdge(edge.a, edge.b, edge.weightFunction)
  }

  /** Add an edge to the graph, from v to w and with the specified weight. */
  def addEdge(v: V, w: V, weight: Float): Connection[V] = {
    val fixed = weight
    addEdge(v, w, new WeightFunction[V] { def getWeight(a: V, b: V): Float = fixed })
  }

  /** Add an edge to the graph, from v to w and with the specified weight function. */
  def addEdge(v: V, w: V, weightFunction: WeightFunction[V]): Connection[V] = {
    if (v == null || w == null) throw IllegalArgumentException("Vertices cannot be null")
    if (v.equals(w)) throw IllegalArgumentException("Self loops are not allowed")
    val a = getNode(v)
    val b = getNode(w)
    if (a == null || b == null) throw IllegalArgumentException("At least one vertex is not in the graph")
    addConnection(a, b, weightFunction)
  }

  /** Removes the edge from v to w from the graph. */
  def removeEdge(v: V, w: V): Boolean = {
    val a = getNode(v)
    val b = getNode(w)
    if (a == null || b == null) throw IllegalArgumentException("At least one vertex is not in the graph")
    removeConnection(a, b)
  }

  def removeEdge(edge: Edge[V]): Boolean = removeConnection(edge.internalNodeA, edge.internalNodeB)

  def removeEdges(edges: Iterable[Edge[V]]): Unit =
    edges.foreach(e => removeConnection(e.internalNodeA, e.internalNodeB))

  def removeEdgeIf(predicate: Edge[V] => Boolean): Unit =
    removeEdges(edges.filter(predicate).toList)

  /** Removes all edges from the graph. */
  def removeAllEdges(): Unit = {
    val iter = nodeMap.nodeIterator
    while (iter.hasNext)
      iter.next().disconnect()
    edgeMap.clear()
  }

  /** Removes all vertices and edges from the graph. */
  def removeAllVertices(): Unit = {
    edgeMap.clear()
    nodeMap.clear()
  }

  /** Sort the vertices using the provided comparator. */
  def sortVertices(comparator: Ordering[V]): Unit =
    nodeMap.sort(comparator)

  /** Sort the edges using the provided comparator. */
  def sortEdges(comparator: Ordering[Connection[V]]): Unit = {
    val entryList = edgeMap.toList.sortWith((a, b) => comparator.compare(a._1, b._1) < 0)
    edgeMap.clear()
    entryList.foreach { case (k, v) => edgeMap.put(k, v) }
  }

  // ================================================================================
  // Internal Methods
  // ================================================================================

  private[graphs] def addConnection(a: Node[V], b: Node[V]): Connection[V] = {
    val e = a.getEdge(b)
    if (e != null) e else addConnection(a, b, defaultEdgeWeightFunction)
  }

  private[graphs] def addConnection(a: Node[V], b: Node[V], weight: WeightFunction[V]): Connection[V] = {
    var e = a.getEdge(b)
    if (e == null) {
      e = obtainEdge()
      e.set(a, b, weight)
      a.addEdge(e)
      edgeMap.put(e, e)
    } else {
      e.setWeight(weight)
    }
    e
  }

  private[graphs] def removeConnection(a: Node[V], b: Node[V]): Boolean =
    removeConnection(a, b, removeFromMap = true)

  private[graphs] def removeConnection(a: Node[V], b: Node[V], removeFromMap: Boolean): Boolean = {
    val e = a.removeEdge(b)
    if (e == null) {
      false
    } else {
      if (removeFromMap) edgeMap.remove(e)
      true
    }
  }

  // ================================================================================
  // Getters
  // ================================================================================

  /** Check if the graph contains a vertex. */
  def contains(v: V): Boolean = nodeMap.contains(v)

  /** Retrieve the edge which is from v to w, or null if none. */
  def getEdge(v: V, w: V): Edge[V] = {
    val a = getNode(v)
    val b = getNode(w)
    if (a == null || b == null) throw IllegalArgumentException("At least one vertex is not in the graph")
    getConnection(a, b)
  }

  /** Check if the graph contains an edge from v to w. */
  def edgeExists(v: V, w: V): Boolean = {
    val a = getNode(v)
    val b = getNode(w)
    if (a == null || b == null) throw IllegalArgumentException("At least one vertex is not in the graph")
    connectionExists(a, b)
  }

  /** Get all the edges which have v as a tail. */
  def getEdges(v: V): Iterable[Edge[V]] = {
    val node = getNode(v)
    if (node == null) null.asInstanceOf[Iterable[Edge[V]]] // @nowarn — null return matches original API
    else node.outEdges.asInstanceOf[Iterable[Edge[V]]]
  }

  /** Get all edges in the graph. */
  def edges: Iterable[Edge[V]] = edgeMap.values.asInstanceOf[Iterable[Edge[V]]]

  /** Get all vertices in the graph. */
  def vertices: Iterable[V] = new Iterable[V] {
    def iterator:           Iterator[V] = nodeMap.vertexIterator
    override def knownSize: Int         = nodeMap.size
  }

  /** Check if the graph is directed. */
  def isDirected: Boolean = true

  /** Get the number of vertices in the graph. */
  def size: Int = nodeMap.size

  /** Get the number of edges in the graph. */
  def edgeCount: Int = edgeMap.size

  /** Get the current default edge weight function. */
  def defaultEdgeWeightFunction: WeightFunction[V] = _defaultEdgeWeight

  /** Set the default edge weight function. */
  def defaultEdgeWeightFunction_=(f: WeightFunction[V]): Unit =
    _defaultEdgeWeight = f

  /** Set a constant default edge weight. */
  def setDefaultEdgeWeight(weight: Float): Unit = {
    val fixed = weight
    _defaultEdgeWeight = new WeightFunction[V] { def getWeight(a: V, b: V): Float = fixed }
  }

  /** @return whether the graph is connected */
  def isConnected: Boolean = numberOfComponents() == 1

  def numberOfComponents(): Int = {
    var visited    = 1
    var components = 0
    while (visited < size) {
      components += 1
      algorithms.depthFirstSearch(vertices.iterator.next(), step => visited += 1)
    }
    components
  }

  // ================================================================================
  // Internal Getters
  // ================================================================================

  private[graphs] def getNode(v: V): Node[V] = nodeMap.get(v)

  private[graphs] def getNodes: Iterable[Node[V]] = new Iterable[Node[V]] {
    def iterator:           Iterator[Node[V]] = nodeMap.nodeIterator
    override def knownSize: Int               = nodeMap.size
  }

  private[graphs] def connectionExists(u: Node[V], v: Node[V]): Boolean = u.getEdge(v) != null

  private[graphs] def getConnection(a: Node[V], b: Node[V]): Connection[V] = a.getEdge(b)

  override def toString: String =
    (if (isDirected) "Directed" else "Undirected") + " graph with " +
      size + " vertices and " + edgeCount + " edges"
}
