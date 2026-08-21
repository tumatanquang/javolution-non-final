/*
 * File: SynchronizedByte.java
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
 * A class useful for offloading synch for byte instance variables.
 *
 * <p>[<a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>]
 */
public class SynchronizedByte extends SynchronizedVariable
		implements Comparable, Cloneable {
	byte _value;
	/**
	 * Make a new SynchronizedByte with the given initial value,
	 * and using its own internal lock.
	 */
	public SynchronizedByte(byte initialValue) {
		super();
		_value = initialValue;
	}
	/**
	 * Make a new SynchronizedByte with the given initial value,
	 * and using the supplied lock.
	 */
	public SynchronizedByte(byte initialValue, Object lock) {
		super(lock);
		_value = initialValue;
	}
	/**
	 * Return the current value
	 */
	public final byte get() {
		synchronized(_lock) {
			return _value;
		}
	}
	/**
	 * Set to newValue.
	 * @return the old value
	 */
	public byte set(byte newValue) {
		synchronized(_lock) {
			final byte old = _value;
			_value = newValue;
			return old;
		}
	}
	/**
	 * Set value to newValue only if it is currently assumedValue.
	 * @return true if successful
	 */
	public boolean commit(byte assumedValue, byte newValue) {
		synchronized(_lock) {
			final boolean success = assumedValue == _value;
			if(success) {
				_value = newValue;
			}
			return success;
		}
	}
	/**
	 * Atomically swap values with another SynchronizedByte.
	 * Uses identityHashCode to avoid deadlock when
	 * two SynchronizedBytes attempt to simultaneously swap with each other.
	 * (Note: Ordering via identyHashCode is not strictly guaranteed
	 * by the language specification to return unique, orderable
	 * values, but in practice JVMs rely on them being unique.)
	 * @return the new value
	 */
	public byte swap(SynchronizedByte other) {
		if(other == this)
			return get();
		SynchronizedByte fst = this;
		SynchronizedByte snd = other;
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
	public byte increment() {
		synchronized(_lock) {
			return ++_value;
		}
	}
	/**
	 * Decrement the value.
	 * @return the new value
	 */
	public byte decrement() {
		synchronized(_lock) {
			return --_value;
		}
	}
	/**
	 * Add amount to value (i.e., set value += amount)
	 * @return the new value
	 */
	public byte add(byte amount) {
		synchronized(_lock) {
			return _value += amount;
		}
	}
	/**
	 * Subtract amount from value (i.e., set value -= amount)
	 * @return the new value
	 */
	public byte subtract(byte amount) {
		synchronized(_lock) {
			return _value -= amount;
		}
	}
	/**
	 * Multiply value by factor (i.e., set value *= factor)
	 * @return the new value
	 */
	public byte multiply(byte factor) {
		synchronized(_lock) {
			return _value *= factor;
		}
	}
	/**
	 * Divide value by factor (i.e., set value /= factor)
	 * @return the new value
	 */
	public byte divide(byte factor) {
		synchronized(_lock) {
			return _value /= factor;
		}
	}
	/**
	 * Set the value to the negative of its old value
	 * @return the new value
	 */
	public byte negate() {
		synchronized(_lock) {
			_value = (byte) -_value;
			return _value;
		}
	}
	/**
	 * Set the value to its complement
	 * @return the new value
	 */
	public byte complement() {
		synchronized(_lock) {
			_value = (byte) ~_value;
			return _value;
		}
	}
	/**
	 * Set value to value &amp; b.
	 * @return the new value
	 */
	public byte and(byte b) {
		synchronized(_lock) {
			_value = (byte) (_value & b);
			return _value;
		}
	}
	/**
	 * Set value to value | b.
	 * @return the new value
	 */
	public byte or(byte b) {
		synchronized(_lock) {
			_value = (byte) (_value | b);
			return _value;
		}
	}
	/**
	 * Set value to value ^ b.
	 * @return the new value
	 */
	public byte xor(byte b) {
		synchronized(_lock) {
			_value = (byte) (_value ^ b);
			return _value;
		}
	}
	public int compareTo(byte other) {
		final byte val = get();
		return val < other ? -1 : val == other ? 0 : 1;
	}
	public int compareTo(SynchronizedByte other) {
		return compareTo(other.get());
	}
	public int compareTo(Object other) {
		return compareTo((SynchronizedByte) other);
	}
	public boolean equals(Object other) {
		if(other != null && other instanceof SynchronizedByte)
			return get() == ((SynchronizedByte) other).get();
		return false;
	}
	public int hashCode() {
		return get();
	}
	public String toString() {
		return String.valueOf(get());
	}
}