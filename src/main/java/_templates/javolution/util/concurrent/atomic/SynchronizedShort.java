/*
 * File: SynchronizedShort.java
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
 * A class useful for offloading synch for short instance variables.
 *
 * <p>[<a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>]
 */
public class SynchronizedShort extends SynchronizedVariable
		implements Comparable, Cloneable {
	short _value;
	/**
	 * Make a new SynchronizedShort with the given initial value,
	 * and using its own internal lock.
	 */
	public SynchronizedShort(short initialValue) {
		super();
		_value = initialValue;
	}
	/**
	 * Make a new SynchronizedShort with the given initial value,
	 * and using the supplied lock.
	 */
	public SynchronizedShort(short initialValue, Object lock) {
		super(lock);
		_value = initialValue;
	}
	/**
	 * Return the current value
	 */
	public final short get() {
		synchronized(_lock) {
			return _value;
		}
	}
	/**
	 * Set to newValue.
	 * @return the old value
	 */
	public short set(short newValue) {
		synchronized(_lock) {
			final short old = _value;
			_value = newValue;
			return old;
		}
	}
	/**
	 * Set value to newValue only if it is currently assumedValue.
	 * @return true if successful
	 */
	public boolean commit(short assumedValue, short newValue) {
		synchronized(_lock) {
			final boolean success = assumedValue == _value;
			if(success) {
				_value = newValue;
			}
			return success;
		}
	}
	/**
	 * Atomically swap values with another SynchronizedShort.
	 * Uses identityHashCode to avoid deadlock when
	 * two SynchronizedShorts attempt to simultaneously swap with each other.
	 * (Note: Ordering via identyHashCode is not strictly guaranteed
	 * by the language specification to return unique, orderable
	 * values, but in practice JVMs rely on them being unique.)
	 * @return the new value
	 */
	public short swap(SynchronizedShort other) {
		if(other == this)
			return get();
		SynchronizedShort fst = this;
		SynchronizedShort snd = other;
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
	public short increment() {
		synchronized(_lock) {
			return ++_value;
		}
	}
	/**
	 * Decrement the value.
	 * @return the new value
	 */
	public short decrement() {
		synchronized(_lock) {
			return --_value;
		}
	}
	/**
	 * Add amount to value (i.e., set value += amount)
	 * @return the new value
	 */
	public short add(short amount) {
		synchronized(_lock) {
			return _value += amount;
		}
	}
	/**
	 * Subtract amount from value (i.e., set value -= amount)
	 * @return the new value
	 */
	public short subtract(short amount) {
		synchronized(_lock) {
			return _value -= amount;
		}
	}
	/**
	 * Multiply value by factor (i.e., set value *= factor)
	 * @return the new value
	 */
	public short multiply(short factor) {
		synchronized(_lock) {
			return _value *= factor;
		}
	}
	/**
	 * Divide value by factor (i.e., set value /= factor)
	 * @return the new value
	 */
	public short divide(short factor) {
		synchronized(_lock) {
			return _value /= factor;
		}
	}
	/**
	 * Set the value to the negative of its old value
	 * @return the new value
	 */
	public short negate() {
		synchronized(_lock) {
			_value = (short) -_value;
			return _value;
		}
	}
	/**
	 * Set the value to its complement
	 * @return the new value
	 */
	public short complement() {
		synchronized(_lock) {
			_value = (short) ~_value;
			return _value;
		}
	}
	/**
	 * Set value to value &amp; b.
	 * @return the new value
	 */
	public short and(short b) {
		synchronized(_lock) {
			_value = (short) (_value & b);
			return _value;
		}
	}
	/**
	 * Set value to value | b.
	 * @return the new value
	 */
	public short or(short b) {
		synchronized(_lock) {
			_value = (short) (_value | b);
			return _value;
		}
	}
	/**
	 * Set value to value ^ b.
	 * @return the new value
	 */
	public short xor(short b) {
		synchronized(_lock) {
			_value = (short) (_value ^ b);
			return _value;
		}
	}
	public int compareTo(short other) {
		final short val = get();
		return val < other ? -1 : val == other ? 0 : 1;
	}
	public int compareTo(SynchronizedShort other) {
		return compareTo(other.get());
	}
	public int compareTo(Object other) {
		return compareTo((SynchronizedShort) other);
	}
	public boolean equals(Object other) {
		if(other != null && other instanceof SynchronizedShort)
			return get() == ((SynchronizedShort) other).get();
		return false;
	}
	public int hashCode() {
		return get();
	}
	public String toString() {
		return String.valueOf(get());
	}
}