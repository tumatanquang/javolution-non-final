/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2026 - Javolution
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package javolution;
import _templates.javolution.testing.TestCase;
import _templates.javolution.testing.TestContext;
import _templates.javolution.testing.TestSuite;
import _templates.javolution.util.FastBitSet;
/**
 * Regression coverage for FastBitSet boundary behavior.
 */
public final class FastBitSetTestSuite extends TestSuite {
	public FastBitSetTestSuite() {
		addTest(new LengthAndTraversal());
		addTest(new ZeroCapacityAndHash());
		addTest(new AbsoluteRange());
	}
	private static final class LengthAndTraversal extends TestCase {
		private static final int[] INDICES = { 0, 1, 62, 63, 64, 65, 127 };
		public String getName() {
			return "FastBitSet: length and traversal boundaries";
		}
		public void execute() {}
		public void validate() {
			final FastBitSet bits = new FastBitSet();
			for(int i = 0; i < INDICES.length; ++i) {
				bits.clear();
				bits.set(INDICES[i]);
				TestContext.assertEquals(INDICES[i] + 1, bits.length());
			}
			bits.clear();
			bits.set(63);
			bits.set(64);
			bits.set(127);
			TestContext.assertEquals(63, bits.nextSetBit(0));
			TestContext.assertEquals(64, bits.nextSetBit(64));
			TestContext.assertEquals(127, bits.nextSetBit(65));
			TestContext.assertEquals(-1, bits.nextSetBit(128));
			TestContext.assertEquals(0, bits.nextClearBit(0));
			TestContext.assertEquals(65, bits.nextClearBit(64));
			TestContext.assertException(IndexOutOfBoundsException.class,
					new Runnable() {
						public void run() {
							bits.nextSetBit(-1);
						}
					});
			TestContext.assertException(IndexOutOfBoundsException.class,
					new Runnable() {
						public void run() {
							bits.nextClearBit(-1);
						}
					});
			bits.clear(127);
			bits.clear(64);
			bits.clear(63);
			TestContext.assertEquals(0, bits.length());
		}
	}
	private static final class ZeroCapacityAndHash extends TestCase {
		public String getName() {
			return "FastBitSet: zero capacity growth and hash termination";
		}
		public void execute() {}
		public void validate() {
			final FastBitSet zero = new FastBitSet(0);
			zero.set(0);
			TestContext.assertTrue(zero.get(0));
			TestContext.assertEquals(1, zero.length());
			final FastBitSet left = new FastBitSet();
			left.set(1);
			left.set(65);
			final FastBitSet right = new FastBitSet(256);
			right.set(1);
			right.set(65);
			TestContext.assertTrue(left.equals(right));
			TestContext.assertEquals(left.hashCode(), right.hashCode());
		}
	}
	private static final class AbsoluteRange extends TestCase {
		public String getName() {
			return "FastBitSet: range retains absolute indices";
		}
		public void execute() {}
		public void validate() {
			final FastBitSet bits = new FastBitSet();
			bits.set(65);
			final FastBitSet range = bits.get(64, 66);
			try {
				TestContext.assertTrue(range.get(65));
				TestContext.assertFalse(range.get(1));
			}
			finally {
				FastBitSet.recycle(range);
			}
		}
	}
}