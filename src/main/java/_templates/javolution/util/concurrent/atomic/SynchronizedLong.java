/**
 * File: SynchronizedLong.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date       Who                What
 * 19Jun1998  dl               Create public version
 * 15Apr2003  dl               Removed redundant "synchronized" for multiply()
 * 23jan04    dl               synchronize self-swap case for swap
 */
package _templates.javolution.util.concurrent.atomic;
import _templates.java.lang.Cloneable;
import _templates.java.lang.Comparable;
/**
 * A class useful for offloading synch for long instance variables.
 *
 * <p>[<a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>]
 */
public class SynchronizedLong extends SynchronizedVariable
		implements Comparable, Cloneable {
	long _value;
	/**
	 * Make a new SynchronizedLong with the given initial value,
	 * and using its own internal lock.
	 */
	public SynchronizedLong(long initialValue) {
		super();
		_value = initialValue;
	}
	/**
	 * Make a new SynchronizedLong with the given initial value,
	 * and using the supplied lock.
	 */
	public SynchronizedLong(long initialValue, Object lock) {
		super(lock);
		_value = initialValue;
	}
	/**
	 * Return the current value
	 */
	public final long get() {
		synchronized(_lock) {
			return _value;
		}
	}
	/**
	 * Set to newValue.
	 * @return the old value
	 */
	public long set(long newValue) {
		synchronized(_lock) {
			final long old = _value;
			_value = newValue;
			return old;
		}
	}
	/**
	 * Set value to newValue only if it is currently assumedValue.
	 * @return true if successful
	 */
	public boolean commit(long assumedValue, long newValue) {
		synchronized(_lock) {
			final boolean success = assumedValue == _value;
			if(success) {
				_value = newValue;
			}
			return success;
		}
	}
	/**
	 * Atomically swap values with another SynchronizedLong.
	 * Uses identityHashCode to avoid deadlock when
	 * two SynchronizedLongs attempt to simultaneously swap with each other.
	 * @return the new value
	 */
	public long swap(SynchronizedLong other) {
		if(other == this)
			return get();
		SynchronizedLong fst = this;
		SynchronizedLong snd = other;
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
	public long increment() {
		synchronized(_lock) {
			return ++_value;
		}
	}
	/**
	 * Decrement the value.
	 * @return the new value
	 */
	public long decrement() {
		synchronized(_lock) {
			return --_value;
		}
	}
	/**
	 * Add amount to value (i.e., set value += amount)
	 * @return the new value
	 */
	public long add(long amount) {
		synchronized(_lock) {
			return _value += amount;
		}
	}
	/**
	 * Subtract amount from value (i.e., set value -= amount)
	 * @return the new value
	 */
	public long subtract(long amount) {
		synchronized(_lock) {
			return _value -= amount;
		}
	}
	/**
	 * Multiply value by factor (i.e., set value *= factor)
	 * @return the new value
	 */
	public long multiply(long factor) {
		synchronized(_lock) {
			return _value *= factor;
		}
	}
	/**
	 * Divide value by factor (i.e., set value /= factor)
	 * @return the new value
	 */
	public long divide(long factor) {
		synchronized(_lock) {
			return _value /= factor;
		}
	}
	/**
	 * Set the value to the negative of its old value
	 * @return the new value
	 */
	public long negate() {
		synchronized(_lock) {
			_value = -_value;
			return _value;
		}
	}
	/**
	 * Set the value to its complement
	 * @return the new value
	 */
	public long complement() {
		synchronized(_lock) {
			_value = ~_value;
			return _value;
		}
	}
	/**
	 * Set value to value &amp; b.
	 * @return the new value
	 */
	public long and(long b) {
		synchronized(_lock) {
			_value = _value & b;
			return _value;
		}
	}
	/**
	 * Set value to value | b.
	 * @return the new value
	 */
	public long or(long b) {
		synchronized(_lock) {
			_value = _value | b;
			return _value;
		}
	}
	/**
	 * Set value to value ^ b.
	 * @return the new value
	 */
	public long xor(long b) {
		synchronized(_lock) {
			_value = _value ^ b;
			return _value;
		}
	}
	public int compareTo(long other) {
		final long val = get();
		return val < other ? -1 : val == other ? 0 : 1;
	}
	public int compareTo(SynchronizedLong other) {
		return compareTo(other.get());
	}
	public int compareTo(Object other) {
		return compareTo((SynchronizedLong) other);
	}
	public boolean equals(Object other) {
		if(other != null && other instanceof SynchronizedLong)
			return get() == ((SynchronizedLong) other).get();
		return false;
	}
	public int hashCode() { // same expression as java.lang.Long
		final long v = get();
		return (int) (v ^ v >> 32);
	}
	public String toString() {
		return String.valueOf(get());
	}
}