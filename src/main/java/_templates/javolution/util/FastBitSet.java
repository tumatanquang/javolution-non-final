/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2008 - Javolution (http://javolution.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package _templates.javolution.util;
import _templates.java.lang.UnsupportedOperationException;
import _templates.java.util.Set;
import _templates.javax.realtime.MemoryArea;
import _templates.javolution.context.ObjectFactory;
import _templates.javolution.lang.MathLib;
import _templates.javolution.lang.Reusable;
/**
 * <p> This class represents either a table of bits or a set of non-negative
 *     numbers.</p>
 *
 * <p> This class is integrated with the collection framework (as
 *     a set of {@link Index indices} and obeys the collection semantic
 *     for methods such as {@link #size} (cardinality) or {@link #equals}
 *     (same set of indices).</p>
 *
 * @author  <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 6.1.0, August 22, 2026
 */
public class FastBitSet extends FastCollection
		/*<Index>*/ implements Set/*<Index>*/, Reusable {
	/**
	 * Holds the set factory.
	 */
	private static final ObjectFactory FACTORY = new ObjectFactory() {
		public Object create() {
			return new FastBitSet();
		}
	};
	/**
	 * Holds the bits (64 bits per long).
	 */
	private long[] _bits;
	/**
	 * Holds the length in words (long) of this bit set.
	 * Any word at or above the current length should be ignored (assumed
	 * to be zero).
	 */
	private int _length;
	/**
	 * Creates a bit set of small initial capacity. All bits are initially
	 * {@code false}.
	 */
	public FastBitSet() {
		this(64);
	}
	/**
	 * Creates a bit set of specified initial capacity (in bits).
	 * All bits are initially {@code false}.  This
	 * constructor reserves enough space to represent the integers
	 * from {@code 0} to {@code bitSize-1}.
	 *
	 * @param bitSize the initial capacity in bits.
	 */
	public FastBitSet(int bitSize) {
		_length = (bitSize - 1 >> 6) + 1;
		_bits = new long[_length];
	}
	/**
	 * Returns a new, preallocated or {@link #recycle recycled} set instance
	 * (on the stack when executing in a {@link _templates.javolution.context.StackContext
	 * StackContext}).
	 *
	 * @return a new, preallocated or recycled set instance.
	 */
	public static FastBitSet newInstance() {
		final FastBitSet bitSet = (FastBitSet) FACTORY.object();
		bitSet._length = 0;
		return bitSet;
	}
	/**
	 * Recycles a set {@link #newInstance() instance} immediately
	 * (on the stack when executing in a {@link _templates.javolution.context.StackContext
	 * StackContext}).
	 */
	public static void recycle(FastBitSet instance) {
		FACTORY.recycle(instance);
	}
	/**
	 * Adds the specified index to this set. This method is equivalent
	 * to <code>set(index.intValue())</code>.
	 *
	 * @param index the integer value to be appended to this set.
	 * @return {@code true} if this set did not contains the specified
	 *         index; {@code false} otherwise.
	 */
	public boolean add(Object/*{Index}*/ index) {
		final int bitIndex = ((Index) index).intValue();
		if(get(bitIndex))
			return false; // Already there.
		set(bitIndex);
		return true;
	}
	/**
	 * Performs the logical AND operation on this bit set and the
	 * given bit set. This means it builds the intersection
	 * of the two sets. The result is stored into this bit set.
	 *
	 * @param that the second bit set.
	 */
	public void and(FastBitSet that) {
		final int n = Math.min(_length, that._length);
		for(int i = 0; i < n; ++i) {
			_bits[i] &= that._bits[i];
		}
		_length = n;
	}
	/**
	 * Performs the logical AND operation on this bit set and the
	 * complement of the given bit set.  This means it
	 * selects every element in the first set, that isn't in the
	 * second set. The result is stored into this bit set.
	 *
	 * @param that the second bit set
	 */
	public void andNot(FastBitSet that) {
		for(int i = Math.min(_length, that._length); --i >= 0;) {
			_bits[i] &= ~that._bits[i];
		}
	}
	/**
	 * Returns the number of bits set to {@code true} (or the size of this
	 * set).
	 *
	 * @return the number of bits being set.
	 */
	public int cardinality() {
		int sum = 0;
		for(int i = 0; i < _length; ++i) {
			sum += MathLib.bitCount(_bits[i]);
		}
		return sum;
	}
	/**
	 * Sets all bits in the set to {@code false} (empty the set).
	 */
	public void clear() {
		_length = 0;
	}
	/**
	 * Removes the specified integer value from this set. That is
	 * the corresponding bit is cleared.
	 *
	 * @param bitIndex a non-negative integer.
	 * @throws IndexOutOfBoundsException if {@code index < 0}
	 */
	public void clear(int bitIndex) {
		final int longIndex = bitIndex >> 6;
		if(longIndex >= _length)
			return;
		_bits[longIndex] &= ~(1L << bitIndex);
	}
	/**
	 * Sets the bits from the specified {@code fromIndex} (inclusive) to the
	 * specified {@code toIndex} (exclusive) to {@code false}.
	 *
	 * @param  fromIndex index of the first bit to be cleared.
	 * @param  toIndex index after the last bit to be cleared.
	 * @throws IndexOutOfBoundsException if
	 *          {@code (fromIndex < 0) | (toIndex < fromIndex)}
	 */
	public void clear(int fromIndex, int toIndex) {
		if(fromIndex < 0 || toIndex < fromIndex)
			throw new IndexOutOfBoundsException();
		final int i = fromIndex >>> 6;
		if(i >= _length)
			return; // Ensures that i < _length
		final int j = toIndex >>> 6;
		if(i == j) {
			_bits[i] &= (1L << fromIndex) - 1 | -1L << toIndex;
			return;
		}
		_bits[i] &= (1L << fromIndex) - 1;
		if(j < _length) {
			_bits[j] &= -1L << toIndex;
		}
		for(int k = i + 1; k < j && k < _length; k++) {
			_bits[k] = 0;
		}
	}
	/**
	 * Sets the bit at the index to the opposite value.
	 *
	 * @param bitIndex the index of the bit.
	 * @throws IndexOutOfBoundsException if {@code bitIndex < 0}
	 */
	public void flip(int bitIndex) {
		final int i = bitIndex >> 6;
		setLength(i + 1);
		_bits[i] ^= 1L << bitIndex;
	}
	/**
	 * Sets a range of bits to the opposite value.
	 *
	 * @param fromIndex the low index (inclusive).
	 * @param toIndex the high index (exclusive).
	 * @throws IndexOutOfBoundsException if
	 *          {@code (fromIndex < 0) | (toIndex < fromIndex)}
	 */
	public void flip(int fromIndex, int toIndex) {
		if(fromIndex < 0 || toIndex < fromIndex)
			throw new IndexOutOfBoundsException();
		final int i = fromIndex >>> 6;
		final int j = toIndex >>> 6;
		setLength(j + 1);
		if(i == j) {
			_bits[i] ^= -1L << fromIndex & (1L << toIndex) - 1;
			return;
		}
		_bits[i] ^= -1L << fromIndex;
		_bits[j] ^= (1L << toIndex) - 1;
		for(int k = i + 1; k < j; ++k) {
			_bits[k] ^= -1;
		}
	}
	/**
	 * Returns {@code true}> if the specified integer is in
	 * this bit set; {@code false } otherwise.
	 *
	 * @param bitIndex a non-negative integer.
	 * @return the value of the bit at the specified index.
	 * @throws IndexOutOfBoundsException if {@code bitIndex < 0}
	 */
	public boolean get(int bitIndex) {
		final int i = bitIndex >> 6;
		return i >= _length ? false : (_bits[i] & 1L << bitIndex) != 0;
	}
	/**
	 * Returns a new bit set composed of a range of bits from this one. The
	 * selected bits retain their original indices; this legacy method does not
	 * shift {@code fromIndex} to zero as {@code java.util.BitSet#get(int, int)}
	 * does.
	 *
	 * @param fromIndex the low index (inclusive).
	 * @param toIndex the high index (exclusive).
	 * @return a context allocated bit set instance.
	 * @throws IndexOutOfBoundsException if
	 *          {@code (fromIndex < 0) | (toIndex < fromIndex)}
	 */
	public FastBitSet get(int fromIndex, int toIndex) {
		if(fromIndex < 0 || fromIndex > toIndex)
			throw new IndexOutOfBoundsException();
		final FastBitSet bitSet = FastBitSet.newInstance();
		final int length = Math.min(_length, (toIndex >>> 6) + 1);
		bitSet.setLength(length);
		System.arraycopy(_bits, 0, bitSet._bits, 0, length);
		bitSet.clear(0, fromIndex);
		bitSet.clear(toIndex, length << 6);
		return bitSet;
	}
	/**
	 * Returns {@code true} if this bit set shares at least one
	 * common bit with the specified bit set.
	 *
	 * @param that the bit set to check for intersection
	 * @return {@code true} if the sets intersect; {@code false} otherwise.
	 */
	public boolean intersects(FastBitSet that) {
		for(int i = Math.min(_length, that._length); --i >= 0;) {
			if((_bits[i] & that._bits[i]) != 0)
				return true;
		}
		return false;
	}
	/**
	 * Returns the logical number of bits actually used by this bit
	 * set. It returns the index of the highest set bit plus one.
	 *
	 * <p> Note: This method does not return the number of set bits
	 *           which is returned by {@link #size} </p>
	 *
	 * @return the index of the highest set bit plus one.
	 */
	public int length() {
		for(int i = _length; --i >= 0;) {
			final long l = _bits[i];
			if(l != 0)
				return (i << 6) + 64 - MathLib.numberOfLeadingZeros(l);
		}
		return 0;
	}
	/**
	 * Returns the index of the next {@code false} bit, from the specified bit
	 * (inclusive).
	 *
	 * @param fromIndex the start location.
	 * @return the first {@code false} bit.
	 * @throws IndexOutOfBoundsException if {@code fromIndex < 0}
	 */
	public int nextClearBit(int fromIndex) {
		if(fromIndex < 0)
			throw new IndexOutOfBoundsException();
		long mask = 1L << fromIndex;
		for(int offset = fromIndex >> 6; offset < _length; ++offset) {
			final long h = _bits[offset];
			do {
				if((h & mask) == 0)
					return fromIndex;
				mask <<= 1;
				++fromIndex;
			}
			while(mask != 0);
			mask = 1;
		}
		return fromIndex;
	}
	/**
	 * Returns the index of the next {@code true} bit, from the specified bit
	 * (inclusive). If there is none, {@code -1} is returned.
	 * The following code will iterates through the bit set:[code]
	 * for(int i = nextSetBit(0); i >= 0;) {
	 *     ...
	 *     if(i == Integer.MAX_VALUE) break;
	 *     i = nextSetBit(i + 1);
	 * }[/code]
	 *
	 * @param fromIndex the start location.
	 * @return the first {@code true} bit, or {@code -1} if none exists.
	 * @throws IndexOutOfBoundsException if {@code fromIndex < 0}
	 */
	public int nextSetBit(int fromIndex) {
		if(fromIndex < 0)
			throw new IndexOutOfBoundsException();
		long mask = 1L << fromIndex;
		for(int offset = fromIndex >> 6; offset < _length; ++offset) {
			final long h = _bits[offset];
			do {
				if((h & mask) != 0)
					return fromIndex;
				mask <<= 1;
				++fromIndex;
			}
			while(mask != 0);
			mask = 1;
		}
		return -1;
	}
	/**
	 * Performs the logical OR operation on this bit set and the one specified.
	 * In other words, builds the union of the two sets.
	 * The result is stored into this bit set.
	 *
	 * @param that the second bit set.
	 */
	public void or(FastBitSet that) {
		if(that._length > _length) {
			setLength(that._length);
		}
		for(int i = that._length; --i >= 0;) {
			_bits[i] |= that._bits[i];
		}
	}
	/**
	 * Adds the specified integer to this set (corresponding bit is set to
	 * {@code true}.
	 *
	 * @param bitIndex a non-negative integer.
	 * @throws IndexOutOfBoundsException if {@code bitIndex < 0}
	 */
	public void set(int bitIndex) {
		final int i = bitIndex >> 6;
		if(i >= _length) {
			setLength(i + 1);
		}
		_bits[i] |= 1L << bitIndex;
	}
	/**
	 * Sets the bit at the given index to the specified value.
	 *
	 * @param bitIndex the position to set.
	 * @param value the value to set it to.
	 * @throws IndexOutOfBoundsException if {@code bitIndex < 0}
	 */
	public void set(int bitIndex, boolean value) {
		if(value) {
			set(bitIndex);
		}
		else {
			clear(bitIndex);
		}
	}
	/**
	 * Sets the bits from the specified {@code fromIndex} (inclusive) to the
	 * specified {@code toIndex} (exclusive) to {@code true}.
	 *
	 * @param  fromIndex index of the first bit to be set.
	 * @param  toIndex index after the last bit to be set.
	 * @throws IndexOutOfBoundsException if
	 *          {@code (fromIndex < 0) | (toIndex < fromIndex)}
	 */
	public void set(int fromIndex, int toIndex) {
		if(fromIndex < 0 || toIndex < fromIndex)
			throw new IndexOutOfBoundsException();
		final int i = fromIndex >>> 6;
		final int j = toIndex >>> 6;
		setLength(j + 1);
		if(i == j) {
			_bits[i] |= -1L << fromIndex & (1L << toIndex) - 1;
			return;
		}
		_bits[i] |= -1L << fromIndex;
		_bits[j] |= (1L << toIndex) - 1;
		for(int k = i + 1; k < j; ++k) {
			_bits[k] = -1;
		}
	}
	/**
	 * Sets the bits between from (inclusive) and to (exclusive) to the
	 * specified value.
	 *
	 * @param fromIndex the start range (inclusive).
	 * @param toIndex the end range (exclusive).
	 * @param value the value to set it to.
	 * @throws IndexOutOfBoundsException if {@code bitIndex < 0}
	 */
	public void set(int fromIndex, int toIndex, boolean value) {
		if(value) {
			set(fromIndex, toIndex);
		}
		else {
			clear(fromIndex, toIndex);
		}
	}
	/**
	 * Returns the cardinality of this bit set (number of bits set).
	 *
	 * <P>Note: Unlike {@code java.util.BitSet} this method does not
	 *          returns an approximation of the number of bits of space
	 *          actually in use. This method is compliant with
	 *          java.util.Collection meaning for size().</p>
	 *
	 * @return the cardinality of this bit set.
	 */
	public int size() {
		return cardinality();
	}
	/**
	 * Performs the logical XOR operation on this bit set and the one specified.
	 * In other words, builds the symmetric remainder of the two sets
	 * (the elements that are in one set, but not in the other).
	 * The result is stored into this bit set.
	 *
	 * @param that the second bit set.
	 */
	public void xor(FastBitSet that) {
		if(that._length > _length) {
			setLength(that._length);
		}
		for(int i = that._length; --i >= 0;) {
			_bits[i] ^= that._bits[i];
		}
	}
	// Optimization.
	public boolean equals(Object obj) {
		if(!(obj instanceof FastBitSet))
			return super.equals(obj);
		final FastBitSet that = (FastBitSet) obj;
		final int n = Math.min(_length, that._length);
		for(int i = 0; i < n; ++i) {
			if(_bits[i] != that._bits[i])
				return false;
		}
		for(int i = n; i < _length; ++i) {
			if(_bits[i] != 0)
				return false;
		}
		final int len = that._length;
		for(int i = n; i < len; ++i) {
			if(that._bits[i] != 0)
				return false;
		}
		return true;
	}
	// Optimization.
	public int hashCode() {
		int h = 0;
		for(int i = nextSetBit(0); i >= 0;) {
			h += i;
			if(i == Integer.MAX_VALUE) {
				break;
			}
			i = nextSetBit(i + 1);
		}
		return h;
	}
	// Implements Reusable.
	public void reset() {
		_length = 0;
	}
	// Implements abstract methods.
	// Records holds the ordering position of the bit sets.
	// (e.g. first bit set has a position of zero).
	public Record head() {
		return Index.valueOf(-1);
	}
	public Record tail() {
		return Index.valueOf(cardinality());
	}
	public Object/*{Index}*/ valueOf(Record record) {
		final int i = ((Index) record).intValue();
		int count = 0;
		for(int j = 0; j < _length;) {
			long l = _bits[j++];
			count += MathLib.bitCount(l);
			if(count > i) { // Found word for record.
				int bitIndex = j << 6;
				for(; count != i; --count) {
					final int shiftRight = MathLib.numberOfLeadingZeros(l) + 1;
					l <<= shiftRight;
					bitIndex -= shiftRight;
				}
				return Index.valueOf(bitIndex);
			}
		}
		return null;
	}
	public void delete(Record record) {
		final Index bitIndex = (Index) valueOf(record);
		if(bitIndex != null)
			throw new UnsupportedOperationException("Not supported yet.");
	}
	/**
	 * Sets the new length of the table (all new bits are <code>false</code>).
	 *
	 * @param newLength the new length of the table.
	 */
	private final void setLength(final int newLength) {
		if(_bits.length < newLength) { // Resizes array.
			MemoryArea.getMemoryArea(this).executeInArea(new Runnable() {
				public final void run() {
					int arrayLength = _bits.length == 0 ? 1 : _bits.length;
					while(arrayLength < newLength) {
						arrayLength <<= 1;
					}
					final long[] tmp = new long[arrayLength];
					System.arraycopy(_bits, 0, tmp, 0, _length);
					_bits = tmp;
				}
			});
		}
		for(int i = _length; i < newLength; ++i) {
			_bits[i] = 0;
		}
		_length = newLength;
	}
}