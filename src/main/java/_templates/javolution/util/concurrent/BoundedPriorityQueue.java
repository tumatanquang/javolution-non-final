/*
 * File: BoundedPriorityQueue.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 16Jun1998    dl             Create public version
 * 25aug1998    dl             added peek
 * 29aug1998    dl             pulled heap mechanics into separate class
 */
package _templates.javolution.util.concurrent;
import _templates.java.lang.NoSuchMethodException;
import _templates.java.lang.SecurityException;
import _templates.java.lang.reflect.InvocationTargetException;
import _templates.java.util.Comparator;
/**
 * A heap-based priority queue, using semaphores for
 * concurrency control.
 * The take operation returns the <em>least</em> element
 * with respect to the given ordering. (If more than
 * one element is tied for least value, one of them is
 * arbitrarily chosen to be returned -- no guarantees
 * are made for ordering across ties.)
 * Ordering follows the JDK1.2 collection
 * conventions: Either the elements must be Comparable, or
 * a Comparator must be supplied. Comparison failures throw
 * ClassCastExceptions during insertions and extractions.
 * The implementation uses a standard array-based heap algorithm,
 * as described in just about any data structures textbook.
 * <p>
 * Put and take operations may throw ClassCastException
 * if elements are not Comparable, or
 * not comparable using the supplied comparator.
 * Since not all elements are compared on each operation
 * it is possible that an exception will not be thrown
 * during insertion of non-comparable element, but will later be
 * encountered during another insertion or extraction.
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class BoundedPriorityQueue extends SemaphoreControlledChannel {
	private final Heap _heap;
	/**
	 * Create a priority queue with the given capacity and comparator
	 * @throws IllegalArgumentException if capacity less or equal to zero
	 */
	public BoundedPriorityQueue(int capacity, Comparator cmp)
			throws IllegalArgumentException {
		super(capacity);
		_heap = new Heap(capacity, cmp);
	}
	/**
	 * Create a priority queue with the current default capacity
	 * and the given comparator
	 */
	public BoundedPriorityQueue(Comparator comparator) {
		this(DefaultChannelCapacity.get(), comparator);
	}
	/**
	 * Create a priority queue with the given capacity,
	 * and relying on natural ordering.
	 */
	public BoundedPriorityQueue(int capacity) {
		this(capacity, null);
	}
	/**
	 * Create a priority queue with the current default capacity
	 * and relying on natural ordering.
	 */
	public BoundedPriorityQueue() {
		this(DefaultChannelCapacity.get(), null);
	}
	/**
	 * Create a priority queue with the given capacity and comparator, using
	 * the supplied Semaphore class for semaphores.
	 * @throws IllegalArgumentException if capacity less or equal to zero
	 * @throws NoSuchMethodException If class does not have constructor
	 * that intializes permits
	 * @throws SecurityException if constructor information
	 * not accessible
	 * @throws InstantiationException if semaphore class is abstract
	 * @throws IllegalAccessException if constructor cannot be called
	 * @throws InvocationTargetException if semaphore constructor throws an
	 * exception
	 */
	public BoundedPriorityQueue(int capacity, Comparator cmp,
			Class semaphoreClass) throws IllegalArgumentException,
			NoSuchMethodException, SecurityException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		super(capacity, semaphoreClass);
		_heap = new Heap(capacity, cmp);
	}
	void insert(Object x) {
		_heap.insert(x);
	}
	Object extract() {
		return _heap.extract();
	}
	public Object peek() {
		return _heap.peek();
	}
}