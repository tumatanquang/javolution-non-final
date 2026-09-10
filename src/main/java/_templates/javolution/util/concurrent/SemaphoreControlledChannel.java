/*
 * File: SemaphoreControlledChannel.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 16Jun1998    dl             Create public version
 * 5Aug1998     dl             replaced int counters with longs
 * 08dec2001    dl             reflective constructor now uses longs too.
 */
package _templates.javolution.util.concurrent;
import _templates.java.lang.NoSuchMethodException;
import _templates.java.lang.SecurityException;
import _templates.java.lang.UnsupportedOperationException;
import _templates.java.lang.reflect.InvocationTargetException;
/*@JVM-1.1+@import java.lang.reflect.Constructor;/**/
/**
 * Abstract class for channels that use Semaphores to
 * control puts and takes.
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public abstract class SemaphoreControlledChannel implements BoundedChannel {
	private final Semaphore _putGuard;
	private final Semaphore _takeGuard;
	private int _capacity;
	/**
	 * Create a channel with the given capacity and default
	 * semaphore implementation
	 * @throws IllegalArgumentException if capacity less or equal to zero
	 */
	public SemaphoreControlledChannel(int capacity)
			throws IllegalArgumentException {
		if(capacity <= 0)
			throw new IllegalArgumentException();
		_capacity = capacity;
		_putGuard = new Semaphore(capacity);
		_takeGuard = new Semaphore(0);
	}
	/**
	 * Create a channel with the given capacity and
	 * semaphore implementations instantiated from the supplied class
	 * @throws IllegalArgumentException if capacity less or equal to zero.
	 * @throws NoSuchMethodException If class does not have constructor
	 * that intializes permits
	 * @throws SecurityException if constructor information
	 * not accessible
	 * @throws InstantiationException if semaphore class is abstract
	 * @throws IllegalAccessException if constructor cannot be called
	 * @throws InvocationTargetException if semaphore constructor throws an
	 * exception
	 */
	public SemaphoreControlledChannel(int capacity, Class semaphoreClass)
			throws IllegalArgumentException, NoSuchMethodException,
			SecurityException, InstantiationException, IllegalAccessException,
			InvocationTargetException {
		/*@JVM-1.1+@
		if(true) {
			if(capacity <= 0)
				throw new IllegalArgumentException();
			_capacity = capacity;
			final Class[] longarg = {Long.TYPE};
			final Constructor ctor = semaphoreClass.getDeclaredConstructor(longarg);
			final Long[] cap = {new Long(capacity)};
			_putGuard = (Semaphore) ctor.newInstance(cap);
			final Long[] zero = {new Long(0)};
			_takeGuard = (Semaphore) ctor.newInstance(zero);
			return;
		}
		/**/
		throw new UnsupportedOperationException("J2ME Not Supported Yet");
	}
	public int capacity() {
		return _capacity;
	}
	/**
	 * Return the number of elements in the buffer.
	 * This is only a snapshot value, that may change
	 * immediately after returning.
	 */
	public int size() {
		return (int) _takeGuard.permits();
	}
	/**
	 * Internal mechanics of put.
	 */
	abstract void insert(Object x);
	/**
	 * Internal mechanics of take.
	 */
	abstract Object extract();
	public void put(Object x) throws InterruptedException {
		if(x == null)
			throw new IllegalArgumentException();
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		_putGuard.acquire();
		try {
			insert(x);
			_takeGuard.release();
		}
		catch(final ClassCastException ex) {
			_putGuard.release();
			throw ex;
		}
	}
	public boolean offer(Object x, long msecs) throws InterruptedException {
		if(x == null)
			throw new IllegalArgumentException();
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		if(!_putGuard.attempt(msecs))
			return false;
		try {
			insert(x);
			_takeGuard.release();
			return true;
		}
		catch(final ClassCastException ex) {
			_putGuard.release();
			throw ex;
		}
	}
	public Object take() throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		_takeGuard.acquire();
		try {
			final Object x = extract();
			_putGuard.release();
			return x;
		}
		catch(final ClassCastException ex) {
			_takeGuard.release();
			throw ex;
		}
	}
	public Object poll(long msecs) throws InterruptedException {
		/*@JVM-1.1+@
		if(Thread.interrupted())
			throw new InterruptedException();
		/**/
		if(!_takeGuard.attempt(msecs))
			return null;
		try {
			final Object x = extract();
			_putGuard.release();
			return x;
		}
		catch(final ClassCastException ex) {
			_takeGuard.release();
			throw ex;
		}
	}
}