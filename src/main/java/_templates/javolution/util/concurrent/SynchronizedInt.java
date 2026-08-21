/*
 * File: SynchronizedInt.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 19Jun1998    dl             Create public version
 * 15Apr2003    dl             Removed redundant "synchronized" for multiply()
 */
package _templates.javolution.util.concurrent;
import _templates.java.lang.Cloneable;
import _templates.java.lang.Comparable;
/**
 * A class useful for offloading synch for int instance variables.
 *
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class SynchronizedInt extends SynchronizedVariable
		implements Comparable, Cloneable {
	private int _value;
	/**
	 * Make a new SynchronizedInt with the given initial value,
	 * and using its own internal lock.
	 */
	public SynchronizedInt(int initialValue) {
		super();
		_value = initialValue;
	}
	/**
	 * Make a new SynchronizedInt with the given initial value,
	 * and using the supplied lock.
	 */
	public SynchronizedInt(int initialValue, Object lock) {
		super(lock);
		_value = initialValue;
	}
	/**
	 * Return the current value
	 */
	public final int get() {
		synchronized(_lock) {
			return _value;
		}
	}
	/**
	 * Set to newValue.
	 * @return the old value
	 */
	public int set(int newValue) {
		synchronized(_lock) {
			final int old = _value;
			_value = newValue;
			return old;
		}
	}
	/**
	 * Set value to newValue only if it is currently assumedValue.
	 * @return true if successful
	 */
	public boolean commit(int assumedValue, int newValue) {
		synchronized(_lock) {
			final boolean success = assumedValue == _value;
			if(success) {
				_value = newValue;
			}
			return success;
		}
	}
	/**
	 * Atomically swap values with another SynchronizedInt.
	 * Uses identityHashCode to avoid deadlock when
	 * two SynchronizedInts attempt to simultaneously swap with each other.
	 * (Note: Ordering via identyHashCode is not strictly guaranteed
	 * by the language specification to return unique, orderable
	 * values, but in practice JVMs rely on them being unique.)
	 * @return the new value
	 */
	public int swap(SynchronizedInt other) {
		if(other == this)
			return get();
		SynchronizedInt fst = this;
		SynchronizedInt snd = other;
		if(System.identityHashCode(fst) > System.identityHashCode(snd)) {
			fst = other;
			snd = this;
		}
		synchronized(fst._lock) {
			synchronized(snd._lock) {
				fst.set(snd.set(fst.get()));
				return get();
			}
		}
	}
	/**
	 * Increment the value.
	 * @return the new value
	 */
	public int increment() {
		synchronized(_lock) {
			return ++_value;
		}
	}
	/**
	 * Decrement the value.
	 * @return the new value
	 */
	public int decrement() {
		synchronized(_lock) {
			return --_value;
		}
	}
	/**
	 * Add amount to value (i.e., set value += amount)
	 * @return the new value
	 */
	public int add(int amount) {
		synchronized(_lock) {
			return _value += amount;
		}
	}
	/**
	 * Subtract amount from value (i.e., set value -= amount)
	 * @return the new value
	 */
	public int subtract(int amount) {
		synchronized(_lock) {
			return _value -= amount;
		}
	}
	/**
	 * Multiply value by factor (i.e., set value *= factor)
	 * @return the new value
	 */
	public int multiply(int factor) {
		synchronized(_lock) {
			return _value *= factor;
		}
	}
	/**
	 * Divide value by factor (i.e., set value /= factor)
	 * @return the new value
	 */
	public int divide(int factor) {
		synchronized(_lock) {
			return _value /= factor;
		}
	}
	/**
	 * Set the value to the negative of its old value
	 * @return the new value
	 */
	public int negate() {
		synchronized(_lock) {
			_value = -_value;
			return _value;
		}
	}
	/**
	 * Set the value to its complement
	 * @return the new value
	 */
	public int complement() {
		synchronized(_lock) {
			_value = ~_value;
			return _value;
		}
	}
	/**
	 * Set value to value &amp; b.
	 * @return the new value
	 */
	public int and(int b) {
		synchronized(_lock) {
			_value = _value & b;
			return _value;
		}
	}
	/**
	 * Set value to value | b.
	 * @return the new value
	 */
	public int or(int b) {
		synchronized(_lock) {
			_value = _value | b;
			return _value;
		}
	}
	/**
	 * Set value to value ^ b.
	 * @return the new value
	 */
	public int xor(int b) {
		synchronized(_lock) {
			_value = _value ^ b;
			return _value;
		}
	}
	public int compareTo(int other) {
		final int val = get();
		return val < other ? -1 : val == other ? 0 : 1;
	}
	public int compareTo(SynchronizedInt other) {
		return compareTo(other.get());
	}
	public int compareTo(Object other) {
		return compareTo((SynchronizedInt) other);
	}
	public boolean equals(Object other) {
		if(other != null && other instanceof SynchronizedInt)
			return get() == ((SynchronizedInt) other).get();
		return false;
	}
	public int hashCode() {
		return get();
	}
	public String toString() {
		return String.valueOf(get());
	}
}