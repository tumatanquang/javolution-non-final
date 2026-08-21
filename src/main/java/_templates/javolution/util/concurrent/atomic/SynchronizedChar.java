/*
 * File: SynchronizedChar.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 19Jun1998    dl             Create public version
 */
package _templates.javolution.util.concurrent.atomic;
import _templates.java.lang.Cloneable;
import _templates.java.lang.Comparable;
/**
 * A class useful for offloading synch for char instance variables.
 *
 * <p>[<a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>]
 */
public class SynchronizedChar extends SynchronizedVariable
		implements Comparable, Cloneable {
	char _value;
	/**
	 * Make a new SynchronizedChar with the given initial value,
	 * and using its own internal lock.
	 */
	public SynchronizedChar(char initialValue) {
		super();
		_value = initialValue;
	}
	/**
	 * Make a new SynchronizedChar with the given initial value,
	 * and using the supplied lock.
	 */
	public SynchronizedChar(char initialValue, Object lock) {
		super(lock);
		_value = initialValue;
	}
	/**
	 * Return the current value
	 */
	public final char get() {
		synchronized(_lock) {
			return _value;
		}
	}
	/**
	 * Set to newValue.
	 * @return the old value
	 */
	public char set(char newValue) {
		synchronized(_lock) {
			final char old = _value;
			_value = newValue;
			return old;
		}
	}
	/**
	 * Set value to newValue only if it is currently assumedValue.
	 * @return true if successful
	 */
	public boolean commit(char assumedValue, char newValue) {
		synchronized(_lock) {
			final boolean success = assumedValue == _value;
			if(success) {
				_value = newValue;
			}
			return success;
		}
	}
	/**
	 * Atomically swap values with another SynchronizedChar.
	 * Uses identityHashCode to avoid deadlock when
	 * two SynchronizedChars attempt to simultaneously swap with each other.
	 * (Note: Ordering via identyHashCode is not strictly guaranteed
	 * by the language specification to return unique, orderable
	 * values, but in practice JVMs rely on them being unique.)
	 * @return the new value
	 */
	public char swap(SynchronizedChar other) {
		if(other == this)
			return get();
		SynchronizedChar fst = this;
		SynchronizedChar snd = other;
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
	public char add(char amount) {
		synchronized(_lock) {
			return _value += amount;
		}
	}
	/**
	 * Subtract amount from value (i.e., set value -= amount)
	 * @return the new value
	 */
	public char subtract(char amount) {
		synchronized(_lock) {
			return _value -= amount;
		}
	}
	/**
	 * Multiply value by factor (i.e., set value *= factor)
	 * @return the new value
	 */
	public synchronized char multiply(char factor) {
		synchronized(_lock) {
			return _value *= factor;
		}
	}
	/**
	 * Divide value by factor (i.e., set value /= factor)
	 * @return the new value
	 */
	public char divide(char factor) {
		synchronized(_lock) {
			return _value /= factor;
		}
	}
	public int compareTo(char other) {
		final char val = get();
		return val < other ? -1 : val == other ? 0 : 1;
	}
	public int compareTo(SynchronizedChar other) {
		return compareTo(other.get());
	}
	public int compareTo(Object other) {
		return compareTo((SynchronizedChar) other);
	}
	public boolean equals(Object other) {
		if(other != null && other instanceof SynchronizedChar)
			return get() == ((SynchronizedChar) other).get();
		return false;
	}
	public int hashCode() { // same hash as Char
		return get();
	}
	public String toString() {
		return String.valueOf(get());
	}
}