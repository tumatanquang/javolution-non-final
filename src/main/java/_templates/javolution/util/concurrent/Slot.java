/*
 * File: Slot.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 11Jun1998    dl             Create public version
 * 25aug1998    dl             added peek
 */
package _templates.javolution.util.concurrent;
import _templates.java.lang.NoSuchMethodException;
import _templates.java.lang.SecurityException;
import _templates.java.lang.reflect.InvocationTargetException;
/**
 * A one-slot buffer, using semaphores to control access.
 * Slots are usually more efficient and controllable than using other
 * bounded buffers implementations with capacity of 1.
 * <p>
 * Among other applications, Slots can be convenient in token-passing
 * designs: Here. the Slot holds a some object serving as a token,
 * that can be obtained
 * and returned by various threads.
 *
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class Slot extends SemaphoreControlledChannel {
	/**
	 * Create a buffer with the given capacity, using
	 * the supplied Semaphore class for semaphores.
	 * @throws NoSuchMethodException If class does not have constructor
	 * that intializes permits
	 * @throws SecurityException if constructor information
	 * not accessible
	 * @throws InstantiationException if semaphore class is abstract
	 * @throws IllegalAccessException if constructor cannot be called
	 * @throws InvocationTargetException if semaphore constructor throws an
	 * exception
	 */
	public Slot(Class semaphoreClass) throws NoSuchMethodException,
			SecurityException, InstantiationException, IllegalAccessException,
			InvocationTargetException {
		super(1, semaphoreClass);
	}
	/**
	 * Create a new Slot using default Semaphore implementations
	 */
	public Slot() {
		super(1);
	}
	/**
	 * The slot
	 */
	private Object _item = null;
	/**
	 * Set the item in preparation for a take
	 */
	synchronized void insert(Object x) {
		_item = x;
	}
	/**
	 * Take item known to exist
	 */
	synchronized Object extract() {
		final Object x = _item;
		_item = null;
		return x;
	}
	public synchronized Object peek() {
		return _item;
	}
}