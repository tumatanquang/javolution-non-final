/*
 * File: Heap.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 29Aug1998    dl             Refactored from BoundedPriorityQueue
 * 08dec2001    dl             Null out slots of removed items
 * 03feb2002    dl             Also null out in clear
 */
package _templates.javolution.util.concurrent;
import _templates.java.lang.Comparable;
import _templates.java.util.Comparator;
/**
 * A heap-based priority queue, without any concurrency control
 * (i.e., no blocking on empty/full states).
 * This class provides the data structure mechanics for BoundedPriorityQueue.
 * <p>
 * The class currently uses a standard array-based heap, as described
 * in, for example, Sedgewick's Algorithms text. All methods
 * are fully synchronized. In the future,
 * it may instead use structures permitting finer-grained locking.
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class Heap {
	private Object[] _nodes; // the tree nodes, packed into an array
	private int _count = 0; // number of used slots
	private final Comparator _cmp; // for ordering
	/**
	 * Create a Heap with the given initial capacity and comparator
	 * @throws IllegalArgumentException if capacity less or equal to zero
	 */
	public Heap(int capacity, Comparator cmp) throws IllegalArgumentException {
		if(capacity <= 0)
			throw new IllegalArgumentException();
		_nodes = new Object[capacity];
		_cmp = cmp;
	}
	/**
	 * Create a Heap with the given capacity,
	 * and relying on natural ordering.
	 */
	public Heap(int capacity) {
		this(capacity, null);
	}
	/** perform element comaprisons using comparator or natural ordering */
	private int compare(Object a, Object b) {
		if(_cmp == null)
			return ((Comparable) a).compareTo(b);
		return _cmp.compare(a, b);
	}
	// indexes of heap parents and children
	private static final int parent(int k) {
		return (k - 1) / 2;
	}
	private static final int left(int k) {
		return 2 * k + 1;
	}
	private static final int right(int k) {
		return 2 * (k + 1);
	}
	/**
	 * insert an element, resize if necessary
	 */
	public synchronized void insert(Object x) {
		if(_count >= _nodes.length) {
			final int newcap = 3 * _nodes.length / 2 + 1;
			final Object[] newnodes = new Object[newcap];
			System.arraycopy(_nodes, 0, newnodes, 0, _nodes.length);
			_nodes = newnodes;
		}
		int k = _count;
		++_count;
		while(k > 0) {
			final int par = parent(k);
			if(compare(x, _nodes[par]) < 0) {
				_nodes[k] = _nodes[par];
				k = par;
			}
			else {
				break;
			}
		}
		_nodes[k] = x;
	}
	/**
	 * Return and remove least element, or null if empty
	 */
	public synchronized Object extract() {
		if(_count < 1)
			return null;
		int k = 0; // take element at root;
		final Object least = _nodes[k];
		--_count;
		final Object x = _nodes[_count];
		_nodes[_count] = null;
		for(;;) {
			final int l = left(k);
			if(l >= _count) {
				break;
			}
			final int r = right(k);
			final int child = r >= _count || compare(_nodes[l], _nodes[r]) < 0
					? l
					: r;
			if(compare(x, _nodes[child]) > 0) {
				_nodes[k] = _nodes[child];
				k = child;
			}
			else {
				break;
			}
		}
		_nodes[k] = x;
		return least;
	}
	/** Return least element without removing it, or null if empty */
	public synchronized Object peek() {
		if(_count > 0)
			return _nodes[0];
		return null;
	}
	/** Return number of elements */
	public synchronized int size() {
		return _count;
	}
	/**
	 * remove all elements
	 */
	public synchronized void clear() {
		for(int i = 0; i < _count; ++i) {
			_nodes[i] = null;
		}
		_count = 0;
	}
}