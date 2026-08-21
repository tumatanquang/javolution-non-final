/*
 * File: SynchronizedDouble.java
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
 * A class useful for offloading synch for double instance variables.
 *
 * <p>[<a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>]
 */
public class SynchronizedDouble extends SynchronizedVariable
		implements Comparable, Cloneable {
	double _value;
	/**
	 * Make a new SynchronizedDouble with the given initial value,
	 * and using its own internal lock.
	 */
	public SynchronizedDouble(double initialValue) {
		super();
		_value = initialValue;
	}
	/**
	 * Make a new SynchronizedDouble with the given initial value,
	 * and using the supplied lock.
	 */
	public SynchronizedDouble(double initialValue, Object lock) {
		super(lock);
		_value = initialValue;
	}
	/**
	 * Return the current value
	 */
	public final double get() {
		synchronized(_lock) {
			return _value;
		}
	}
	/**
	 * Set to newValue.
	 * @return the old value
	 */
	public double set(double newValue) {
		synchronized(_lock) {
			final double old = _value;
			_value = newValue;
			return old;
		}
	}
	/**
	 * Set value to newValue only if it is currently assumedValue.
	 * @return true if successful
	 */
	public boolean commit(double assumedValue, double newValue) {
		synchronized(_lock) {
			final boolean success = assumedValue == _value;
			if(success) {
				_value = newValue;
			}
			return success;
		}
	}
	/**
	 * Atomically swap values with another SynchronizedDouble.
	 * Uses identityHashCode to avoid deadlock when
	 * two SynchronizedDoubles attempt to simultaneously swap with each other.
	 * (Note: Ordering via identyHashCode is not strictly guaranteed
	 * by the language specification to return unique, orderable
	 * values, but in practice JVMs rely on them being unique.)
	 * @return the new value
	 */
	public double swap(SynchronizedDouble other) {
		if(other == this)
			return get();
		SynchronizedDouble fst = this;
		SynchronizedDouble snd = other;
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
	public double add(double amount) {
		synchronized(_lock) {
			return _value += amount;
		}
	}
	/**
	 * Subtract amount from value (i.e., set value -= amount)
	 * @return the new value
	 */
	public double subtract(double amount) {
		synchronized(_lock) {
			return _value -= amount;
		}
	}
	/**
	 * Multiply value by factor (i.e., set value *= factor)
	 * @return the new value
	 */
	public double multiply(double factor) {
		synchronized(_lock) {
			return _value *= factor;
		}
	}
	/**
	 * Divide value by factor (i.e., set value /= factor)
	 * @return the new value
	 */
	public double divide(double factor) {
		synchronized(_lock) {
			return _value /= factor;
		}
	}
	public int compareTo(double other) {
		final double val = get();
		return val < other ? -1 : val == other ? 0 : 1;
	}
	public int compareTo(SynchronizedDouble other) {
		return compareTo(other.get());
	}
	public int compareTo(Object other) {
		return compareTo((SynchronizedDouble) other);
	}
	public boolean equals(Object other) {
		if(other != null && other instanceof SynchronizedDouble)
			return get() == ((SynchronizedDouble) other).get();
		return false;
	}
	public int hashCode() { // same hash as Double
		final long bits = Double.doubleToLongBits(get());
		return (int) (bits ^ bits >> 32);
	}
	public String toString() {
		return String.valueOf(get());
	}
}