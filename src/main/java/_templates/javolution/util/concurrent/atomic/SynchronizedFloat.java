/*
 * File: SynchronizedFloat.java
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
package _templates.javolution.util.concurrent.atomic;
import _templates.java.lang.Cloneable;
import _templates.java.lang.Comparable;
/**
 * A class useful for offloading synch for float instance variables.
 *
 * <p>[<a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>]
 */
public class SynchronizedFloat extends SynchronizedVariable
		implements Comparable, Cloneable {
	float _value;
	/**
	 * Make a new SynchronizedFloat with the given initial value,
	 * and using its own internal lock.
	 */
	public SynchronizedFloat(float initialValue) {
		super();
		_value = initialValue;
	}
	/**
	 * Make a new SynchronizedFloat with the given initial value,
	 * and using the supplied lock.
	 */
	public SynchronizedFloat(float initialValue, Object lock) {
		super(lock);
		_value = initialValue;
	}
	/**
	 * Return the current value
	 */
	public final float get() {
		synchronized(_lock) {
			return _value;
		}
	}
	/**
	 * Set to newValue.
	 * @return the old value
	 */
	public float set(float newValue) {
		synchronized(_lock) {
			final float old = _value;
			_value = newValue;
			return old;
		}
	}
	/**
	 * Set value to newValue only if it is currently assumedValue.
	 * @return true if successful
	 */
	public boolean commit(float assumedValue, float newValue) {
		synchronized(_lock) {
			final boolean success = assumedValue == _value;
			if(success) {
				_value = newValue;
			}
			return success;
		}
	}
	/**
	 * Atomically swap values with another SynchronizedFloat.
	 * Uses identityHashCode to avoid deadlock when
	 * two SynchronizedFloats attempt to simultaneously swap with each other.
	 * (Note: Ordering via identyHashCode is not strictly guaranteed
	 * by the language specification to return unique, orderable
	 * values, but in practice JVMs rely on them being unique.)
	 * @return the new value
	 */
	public float swap(SynchronizedFloat other) {
		if(other == this)
			return get();
		SynchronizedFloat fst = this;
		SynchronizedFloat snd = other;
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
	 * Add amount to value (i.e., set value += amount)
	 * @return the new value
	 */
	public float add(float amount) {
		synchronized(_lock) {
			return _value += amount;
		}
	}
	/**
	 * Subtract amount from value (i.e., set value -= amount)
	 * @return the new value
	 */
	public float subtract(float amount) {
		synchronized(_lock) {
			return _value -= amount;
		}
	}
	/**
	 * Multiply value by factor (i.e., set value *= factor)
	 * @return the new value
	 */
	public float multiply(float factor) {
		synchronized(_lock) {
			return _value *= factor;
		}
	}
	/**
	 * Divide value by factor (i.e., set value /= factor)
	 * @return the new value
	 */
	public float divide(float factor) {
		synchronized(_lock) {
			return _value /= factor;
		}
	}
	public int compareTo(float other) {
		final float val = get();
		return val < other ? -1 : val == other ? 0 : 1;
	}
	public int compareTo(SynchronizedFloat other) {
		return compareTo(other.get());
	}
	public int compareTo(Object other) {
		return compareTo((SynchronizedFloat) other);
	}
	public boolean equals(Object other) {
		if(other != null && other instanceof SynchronizedFloat)
			return get() == ((SynchronizedFloat) other).get();
		return false;
	}
	public int hashCode() {
		return Float.floatToIntBits(get());
	}
	public String toString() {
		return String.valueOf(get());
	}
}