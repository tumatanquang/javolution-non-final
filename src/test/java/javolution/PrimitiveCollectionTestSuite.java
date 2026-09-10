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
import _templates.javolution.util.primitive.FastBooleanIterator;
import _templates.javolution.util.primitive.FastBooleanSet;
import _templates.javolution.util.primitive.FastBooleanTable;
import _templates.javolution.util.primitive.FastByteIterator;
import _templates.javolution.util.primitive.FastByteSet;
import _templates.javolution.util.primitive.FastByteTable;
import _templates.javolution.util.primitive.FastCharIterator;
import _templates.javolution.util.primitive.FastCharSet;
import _templates.javolution.util.primitive.FastCharTable;
import _templates.javolution.util.primitive.FastDoubleIterator;
import _templates.javolution.util.primitive.FastDoubleSet;
import _templates.javolution.util.primitive.FastDoubleTable;
import _templates.javolution.util.primitive.FastFloatIterator;
import _templates.javolution.util.primitive.FastFloatSet;
import _templates.javolution.util.primitive.FastFloatTable;
import _templates.javolution.util.primitive.FastIntIterator;
import _templates.javolution.util.primitive.FastIntSet;
import _templates.javolution.util.primitive.FastIntTable;
import _templates.javolution.util.primitive.FastLongIterator;
import _templates.javolution.util.primitive.FastLongSet;
import _templates.javolution.util.primitive.FastLongTable;
import _templates.javolution.util.primitive.FastShortIterator;
import _templates.javolution.util.primitive.FastShortSet;
import _templates.javolution.util.primitive.FastShortTable;
/**
 * Regression coverage for all primitive table and set families.
 */
public final class PrimitiveCollectionTestSuite extends TestSuite {
	private static final int[] CAPACITIES = { 16, 1024, 1025, 2048, 4096 };
	public PrimitiveCollectionTestSuite() {
		addTest(new TableCapacity());
		addTest(new TableSelfInsertion());
		addTest(new SetTombstoneChurn());
		addTest(new SetEmptyFastPaths());
		addTest(new SharedViews());
	}
	private final class TableCapacity extends TestCase {
		public String getName() {
			return "Primitive tables: segmented capacity and trim";
		}
		public void execute() {}
		public void validate() {
			for(int i = 0; i < CAPACITIES.length; ++i) {
				final int capacity = CAPACITIES[i];
				checkBooleanCapacity(capacity);
				checkByteCapacity(capacity);
				checkCharCapacity(capacity);
				checkDoubleCapacity(capacity);
				checkFloatCapacity(capacity);
				checkIntCapacity(capacity);
				checkLongCapacity(capacity);
				checkShortCapacity(capacity);
			}
		}
	}
	private final class TableSelfInsertion extends TestCase {
		public String getName() {
			return "Primitive tables: self insertion";
		}
		public void execute() {}
		public void validate() {
			final int length = 1030;
			final int middle = length >> 2;
			checkBooleanSelfInsertion(0);
			checkBooleanSelfInsertion(middle);
			checkBooleanSelfInsertion(length);
			checkByteSelfInsertion(0);
			checkByteSelfInsertion(middle);
			checkByteSelfInsertion(length);
			checkCharSelfInsertion(0);
			checkCharSelfInsertion(middle);
			checkCharSelfInsertion(length);
			checkDoubleSelfInsertion(0);
			checkDoubleSelfInsertion(middle);
			checkDoubleSelfInsertion(length);
			checkFloatSelfInsertion(0);
			checkFloatSelfInsertion(middle);
			checkFloatSelfInsertion(length);
			checkIntSelfInsertion(0);
			checkIntSelfInsertion(middle);
			checkIntSelfInsertion(length);
			checkLongSelfInsertion(0);
			checkLongSelfInsertion(middle);
			checkLongSelfInsertion(length);
			checkShortSelfInsertion(0);
			checkShortSelfInsertion(middle);
			checkShortSelfInsertion(length);
		}
	}
	private final class SetTombstoneChurn extends TestCase {
		public String getName() {
			return "Primitive sets: bounded probing after tombstone churn";
		}
		public void execute() {}
		public void validate() {
			checkByteSetChurn();
			checkCharSetChurn();
			checkDoubleSetChurn();
			checkFloatSetChurn();
			checkIntSetChurn();
			checkLongSetChurn();
			checkShortSetChurn();
			checkFloatingPointSetSemantics();
		}
	}
	private final class SetEmptyFastPaths extends TestCase {
		public String getName() {
			return "Primitive sets: empty fast paths";
		}
		public void execute() {}
		public void validate() {
			checkBooleanSetEmptyFastPaths();
			checkByteSetEmptyFastPaths();
			checkCharSetEmptyFastPaths();
			checkDoubleSetEmptyFastPaths();
			checkFloatSetEmptyFastPaths();
			checkIntSetEmptyFastPaths();
			checkLongSetEmptyFastPaths();
			checkShortSetEmptyFastPaths();
		}
	}
	private final class SharedViews extends TestCase {
		public String getName() {
			return "Primitive shared views: iterator locking and concurrent writes";
		}
		public void execute() {}
		public void validate() throws Exception {
			checkBooleanSharedTable();
			checkBooleanSharedSet();
			checkByteSharedTable();
			checkByteSharedSet();
			checkCharSharedTable();
			checkCharSharedSet();
			checkDoubleSharedTable();
			checkDoubleSharedSet();
			checkFloatSharedTable();
			checkFloatSharedSet();
			checkIntSharedTable();
			checkIntSharedSet();
			checkLongSharedTable();
			checkLongSharedSet();
			checkShortSharedTable();
			checkShortSharedSet();
			checkConcurrentIntTableWrites();
		}
	}
	private void checkBooleanCapacity(int capacity) {
		final FastBooleanTable table = new FastBooleanTable(capacity);
		for(int i = 0; i < capacity; ++i) {
			table.add((i & 1) != 0);
		}
		TestContext.assertEquals(capacity, table.size());
		for(int i = 0; i < capacity; ++i) {
			TestContext.assertTrue(table.get(i) == ((i & 1) != 0));
		}
		table.trimToSize();
		TestContext.assertEquals(capacity, table.size());
	}
	private void checkBooleanSelfInsertion(int index) {
		final int originalSize = 1030;
		final boolean[] values = new boolean[originalSize];
		for(int i = 0; i < originalSize; ++i) {
			values[i] = (i & 1) != 0;
		}
		final FastBooleanTable table = new FastBooleanTable(values);
		TestContext.assertTrue(table.addAll(index, table));
		TestContext.assertEquals(originalSize << 1, table.size());
		for(int i = 0; i < table.size(); ++i) {
			final int source = i < index ? i
					: i < index + originalSize ? i - index : i - originalSize;
			TestContext.assertTrue(table.get(i) == values[source]);
		}
		table.trimToSize();
	}
	private void checkBooleanSharedTable() {
		final FastBooleanTable table = new FastBooleanTable();
		table.add(false);
		table.add(true);
		final FastBooleanTable shared = (FastBooleanTable) table.shared();
		final FastBooleanIterator iterator = (FastBooleanIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		TestContext.assertTrue(iterator.next() == false);
		iterator.remove();
		TestContext.assertEquals(1, shared.size());
		TestContext.assertTrue(shared.get(0) == true);
		final FastBooleanTable source = new FastBooleanTable(new boolean[] { false, true });
		final FastBooleanTable unmodifiable = (FastBooleanTable) source.unmodifiable();
		final FastBooleanTable fromView = new FastBooleanTable();
		TestContext.assertTrue(fromView.addAll(unmodifiable));
		TestContext.assertTrue(source.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(source));
		final FastBooleanTable sourceShared = (FastBooleanTable) source.shared();
		source.add(false);
		TestContext.assertEquals(3, unmodifiable.size());
		TestContext.assertEquals(3, sourceShared.size());
		TestContext.assertTrue(unmodifiable.get(2) == false);
		TestContext.assertTrue(sourceShared.get(2) == false);
		source.remove(2);
		TestContext.assertEquals(2, unmodifiable.size());
		TestContext.assertEquals(2, sourceShared.size());
		final FastBooleanTable fromShared = new FastBooleanTable();
		TestContext.assertTrue(fromShared.addAll((FastBooleanTable) source.shared()));
		final FastBooleanTable unmodifiableOwner = new FastBooleanTable(new boolean[] { false, true });
		TestContext.assertTrue(unmodifiableOwner.addAll(1,
				(FastBooleanTable) unmodifiableOwner.unmodifiable()));
		TestContext.assertTrue(unmodifiableOwner.equals(new FastBooleanTable(new boolean[] { false, false, true, true })));
		final FastBooleanTable sharedOwner = new FastBooleanTable(new boolean[] { false, true });
		final FastBooleanTable ownedShared = (FastBooleanTable) sharedOwner.shared();
		TestContext.assertTrue(sharedOwner.addAll(1,
				ownedShared));
		TestContext.assertTrue(sharedOwner.equals(new FastBooleanTable(new boolean[] { false, false, true, true })));
		final FastBooleanTable receiverBacking = new FastBooleanTable(
				new boolean[] { false, true });
		final FastBooleanTable receiverShared = (FastBooleanTable) receiverBacking.shared();
		TestContext.assertTrue(receiverShared.addAll(1, receiverBacking));
		TestContext.assertTrue(receiverShared.equals(new FastBooleanTable(
				new boolean[] { false, false, true, true })));
	}
	private void checkBooleanSharedSet() {
		final FastBooleanSet set = new FastBooleanSet();
		set.add(false);
		set.add(true);
		final FastBooleanSet shared = (FastBooleanSet) set.shared();
		final FastBooleanIterator iterator = (FastBooleanIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final boolean removed = iterator.next();
		iterator.remove();
		TestContext.assertFalse(shared.contains(removed));
		TestContext.assertEquals(1, shared.size());
		final FastBooleanSet source = new FastBooleanSet(new boolean[] { false, true });
		final FastBooleanSet unmodifiable = (FastBooleanSet) source.unmodifiable();
		final FastBooleanSet copied = new FastBooleanSet(unmodifiable);
		TestContext.assertTrue(copied.equals(source));
		final FastBooleanSet sourceShared = (FastBooleanSet) source.shared();
		TestContext.assertTrue(source.remove(false));
		TestContext.assertFalse(unmodifiable.contains(false));
		TestContext.assertFalse(sourceShared.contains(false));
		TestContext.assertTrue(source.add(false));
		TestContext.assertTrue(unmodifiable.contains(false));
		TestContext.assertTrue(sourceShared.contains(false));
		final FastBooleanSet target = (FastBooleanSet) new FastBooleanSet().shared();
		TestContext.assertTrue(target.addAll(unmodifiable));
		TestContext.assertTrue(target.containsAll(unmodifiable));
		TestContext.assertTrue(target.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(target));
		TestContext.assertTrue(target.removeAll((FastBooleanSet) source.shared()));
		TestContext.assertEquals(0, target.size());
		TestContext.assertTrue(target.addAll(unmodifiable));
		final FastBooleanSet retained = new FastBooleanSet(new boolean[] { true });
		TestContext.assertTrue(target.retainAll((FastBooleanSet) retained.shared()));
		TestContext.assertEquals(1, target.size());
		TestContext.assertTrue(target.contains(true));
	}
	private void checkByteCapacity(int capacity) {
		final FastByteTable table = new FastByteTable(capacity);
		for(int i = 0; i < capacity; ++i) {
			table.add((byte) i);
		}
		TestContext.assertEquals(capacity, table.size());
		for(int i = 0; i < capacity; ++i) {
			TestContext.assertTrue(table.get(i) == (byte) i);
		}
		table.trimToSize();
		TestContext.assertEquals(capacity, table.size());
	}
	private void checkByteSelfInsertion(int index) {
		final int originalSize = 1030;
		final byte[] values = new byte[originalSize];
		for(int i = 0; i < originalSize; ++i) {
			values[i] = (byte) i;
		}
		final FastByteTable table = new FastByteTable(values);
		TestContext.assertTrue(table.addAll(index, table));
		TestContext.assertEquals(originalSize << 1, table.size());
		for(int i = 0; i < table.size(); ++i) {
			final int source = i < index ? i
					: i < index + originalSize ? i - index : i - originalSize;
			TestContext.assertTrue(table.get(i) == values[source]);
		}
		table.trimToSize();
	}
	private void checkByteSharedTable() {
		final FastByteTable table = new FastByteTable();
		table.add((byte) 1);
		table.add((byte) 2);
		final FastByteTable shared = (FastByteTable) table.shared();
		final FastByteIterator iterator = (FastByteIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		TestContext.assertTrue(iterator.next() == (byte) 1);
		iterator.remove();
		TestContext.assertEquals(1, shared.size());
		TestContext.assertTrue(shared.get(0) == (byte) 2);
		final FastByteTable source = new FastByteTable(new byte[] { (byte) 1, (byte) 2 });
		final FastByteTable unmodifiable = (FastByteTable) source.unmodifiable();
		final FastByteTable fromView = new FastByteTable();
		TestContext.assertTrue(fromView.addAll(unmodifiable));
		TestContext.assertTrue(source.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(source));
		final FastByteTable sourceShared = (FastByteTable) source.shared();
		source.add((byte) 3);
		TestContext.assertEquals(3, unmodifiable.size());
		TestContext.assertEquals(3, sourceShared.size());
		TestContext.assertTrue(unmodifiable.get(2) == (byte) 3);
		TestContext.assertTrue(sourceShared.get(2) == (byte) 3);
		source.remove(2);
		TestContext.assertEquals(2, unmodifiable.size());
		TestContext.assertEquals(2, sourceShared.size());
		final FastByteTable fromShared = new FastByteTable();
		TestContext.assertTrue(fromShared.addAll((FastByteTable) source.shared()));
		final FastByteTable unmodifiableOwner = new FastByteTable(new byte[] { (byte) 1, (byte) 2 });
		TestContext.assertTrue(unmodifiableOwner.addAll(1,
				(FastByteTable) unmodifiableOwner.unmodifiable()));
		TestContext.assertTrue(unmodifiableOwner.equals(new FastByteTable(new byte[] { (byte) 1, (byte) 1, (byte) 2, (byte) 2 })));
		final FastByteTable sharedOwner = new FastByteTable(new byte[] { (byte) 1, (byte) 2 });
		final FastByteTable ownedShared = (FastByteTable) sharedOwner.shared();
		TestContext.assertTrue(sharedOwner.addAll(1,
				ownedShared));
		TestContext.assertTrue(sharedOwner.equals(new FastByteTable(new byte[] { (byte) 1, (byte) 1, (byte) 2, (byte) 2 })));
		final FastByteTable receiverBacking = new FastByteTable(
				new byte[] { (byte) 1, (byte) 2 });
		final FastByteTable receiverShared = (FastByteTable) receiverBacking.shared();
		TestContext.assertTrue(receiverShared.addAll(1, receiverBacking));
		TestContext.assertTrue(receiverShared.equals(new FastByteTable(
				new byte[] { (byte) 1, (byte) 1, (byte) 2, (byte) 2 })));
	}
	private void checkByteSharedSet() {
		final FastByteSet set = new FastByteSet();
		set.add((byte) 1);
		set.add((byte) 2);
		final FastByteSet shared = (FastByteSet) set.shared();
		final FastByteIterator iterator = (FastByteIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final byte removed = iterator.next();
		iterator.remove();
		TestContext.assertFalse(shared.contains(removed));
		TestContext.assertEquals(1, shared.size());
		final FastByteSet source = new FastByteSet(new byte[] { (byte) 1, (byte) 2 });
		final FastByteSet unmodifiable = (FastByteSet) source.unmodifiable();
		final FastByteSet copied = new FastByteSet(unmodifiable);
		TestContext.assertTrue(copied.equals(source));
		final FastByteSet sourceShared = (FastByteSet) source.shared();
		TestContext.assertTrue(source.remove((byte) 1));
		TestContext.assertFalse(unmodifiable.contains((byte) 1));
		TestContext.assertFalse(sourceShared.contains((byte) 1));
		TestContext.assertTrue(source.add((byte) 1));
		TestContext.assertTrue(unmodifiable.contains((byte) 1));
		TestContext.assertTrue(sourceShared.contains((byte) 1));
		final FastByteSet target = (FastByteSet) new FastByteSet().shared();
		TestContext.assertTrue(target.addAll(unmodifiable));
		TestContext.assertTrue(target.containsAll(unmodifiable));
		TestContext.assertTrue(target.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(target));
		TestContext.assertTrue(target.removeAll((FastByteSet) source.shared()));
		TestContext.assertEquals(0, target.size());
		TestContext.assertTrue(target.addAll(unmodifiable));
		final FastByteSet retained = new FastByteSet(new byte[] { (byte) 2 });
		TestContext.assertTrue(target.retainAll((FastByteSet) retained.shared()));
		TestContext.assertEquals(1, target.size());
		TestContext.assertTrue(target.contains((byte) 2));
	}
	private void checkCharCapacity(int capacity) {
		final FastCharTable table = new FastCharTable(capacity);
		for(int i = 0; i < capacity; ++i) {
			table.add((char) i);
		}
		TestContext.assertEquals(capacity, table.size());
		for(int i = 0; i < capacity; ++i) {
			TestContext.assertTrue(table.get(i) == (char) i);
		}
		table.trimToSize();
		TestContext.assertEquals(capacity, table.size());
	}
	private void checkCharSelfInsertion(int index) {
		final int originalSize = 1030;
		final char[] values = new char[originalSize];
		for(int i = 0; i < originalSize; ++i) {
			values[i] = (char) i;
		}
		final FastCharTable table = new FastCharTable(values);
		TestContext.assertTrue(table.addAll(index, table));
		TestContext.assertEquals(originalSize << 1, table.size());
		for(int i = 0; i < table.size(); ++i) {
			final int source = i < index ? i
					: i < index + originalSize ? i - index : i - originalSize;
			TestContext.assertTrue(table.get(i) == values[source]);
		}
		table.trimToSize();
	}
	private void checkCharSharedTable() {
		final FastCharTable table = new FastCharTable();
		table.add((char) 1);
		table.add((char) 2);
		final FastCharTable shared = (FastCharTable) table.shared();
		final FastCharIterator iterator = (FastCharIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		TestContext.assertTrue(iterator.next() == (char) 1);
		iterator.remove();
		TestContext.assertEquals(1, shared.size());
		TestContext.assertTrue(shared.get(0) == (char) 2);
		final FastCharTable source = new FastCharTable(new char[] { (char) 1, (char) 2 });
		final FastCharTable unmodifiable = (FastCharTable) source.unmodifiable();
		final FastCharTable fromView = new FastCharTable();
		TestContext.assertTrue(fromView.addAll(unmodifiable));
		TestContext.assertTrue(source.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(source));
		final FastCharTable sourceShared = (FastCharTable) source.shared();
		source.add((char) 3);
		TestContext.assertEquals(3, unmodifiable.size());
		TestContext.assertEquals(3, sourceShared.size());
		TestContext.assertTrue(unmodifiable.get(2) == (char) 3);
		TestContext.assertTrue(sourceShared.get(2) == (char) 3);
		source.remove(2);
		TestContext.assertEquals(2, unmodifiable.size());
		TestContext.assertEquals(2, sourceShared.size());
		final FastCharTable fromShared = new FastCharTable();
		TestContext.assertTrue(fromShared.addAll((FastCharTable) source.shared()));
		final FastCharTable unmodifiableOwner = new FastCharTable(new char[] { (char) 1, (char) 2 });
		TestContext.assertTrue(unmodifiableOwner.addAll(1,
				(FastCharTable) unmodifiableOwner.unmodifiable()));
		TestContext.assertTrue(unmodifiableOwner.equals(new FastCharTable(new char[] { (char) 1, (char) 1, (char) 2, (char) 2 })));
		final FastCharTable sharedOwner = new FastCharTable(new char[] { (char) 1, (char) 2 });
		final FastCharTable ownedShared = (FastCharTable) sharedOwner.shared();
		TestContext.assertTrue(sharedOwner.addAll(1,
				ownedShared));
		TestContext.assertTrue(sharedOwner.equals(new FastCharTable(new char[] { (char) 1, (char) 1, (char) 2, (char) 2 })));
		final FastCharTable receiverBacking = new FastCharTable(
				new char[] { (char) 1, (char) 2 });
		final FastCharTable receiverShared = (FastCharTable) receiverBacking.shared();
		TestContext.assertTrue(receiverShared.addAll(1, receiverBacking));
		TestContext.assertTrue(receiverShared.equals(new FastCharTable(
				new char[] { (char) 1, (char) 1, (char) 2, (char) 2 })));
	}
	private void checkCharSharedSet() {
		final FastCharSet set = new FastCharSet();
		set.add((char) 1);
		set.add((char) 2);
		final FastCharSet shared = (FastCharSet) set.shared();
		final FastCharIterator iterator = (FastCharIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final char removed = iterator.next();
		iterator.remove();
		TestContext.assertFalse(shared.contains(removed));
		TestContext.assertEquals(1, shared.size());
		final FastCharSet source = new FastCharSet(new char[] { (char) 1, (char) 2 });
		final FastCharSet unmodifiable = (FastCharSet) source.unmodifiable();
		final FastCharSet copied = new FastCharSet(unmodifiable);
		TestContext.assertTrue(copied.equals(source));
		final FastCharSet sourceShared = (FastCharSet) source.shared();
		TestContext.assertTrue(source.remove((char) 1));
		TestContext.assertFalse(unmodifiable.contains((char) 1));
		TestContext.assertFalse(sourceShared.contains((char) 1));
		TestContext.assertTrue(source.add((char) 1));
		TestContext.assertTrue(unmodifiable.contains((char) 1));
		TestContext.assertTrue(sourceShared.contains((char) 1));
		final FastCharSet target = (FastCharSet) new FastCharSet().shared();
		TestContext.assertTrue(target.addAll(unmodifiable));
		TestContext.assertTrue(target.containsAll(unmodifiable));
		TestContext.assertTrue(target.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(target));
		TestContext.assertTrue(target.removeAll((FastCharSet) source.shared()));
		TestContext.assertEquals(0, target.size());
		TestContext.assertTrue(target.addAll(unmodifiable));
		final FastCharSet retained = new FastCharSet(new char[] { (char) 2 });
		TestContext.assertTrue(target.retainAll((FastCharSet) retained.shared()));
		TestContext.assertEquals(1, target.size());
		TestContext.assertTrue(target.contains((char) 2));
	}
	private void checkDoubleCapacity(int capacity) {
		final FastDoubleTable table = new FastDoubleTable(capacity);
		for(int i = 0; i < capacity; ++i) {
			table.add((double) i);
		}
		TestContext.assertEquals(capacity, table.size());
		for(int i = 0; i < capacity; ++i) {
			TestContext.assertTrue(table.get(i) == (double) i);
		}
		table.trimToSize();
		TestContext.assertEquals(capacity, table.size());
	}
	private void checkDoubleSelfInsertion(int index) {
		final int originalSize = 1030;
		final double[] values = new double[originalSize];
		for(int i = 0; i < originalSize; ++i) {
			values[i] = (double) i;
		}
		final FastDoubleTable table = new FastDoubleTable(values);
		TestContext.assertTrue(table.addAll(index, table));
		TestContext.assertEquals(originalSize << 1, table.size());
		for(int i = 0; i < table.size(); ++i) {
			final int source = i < index ? i
					: i < index + originalSize ? i - index : i - originalSize;
			TestContext.assertTrue(table.get(i) == values[source]);
		}
		table.trimToSize();
	}
	private void checkDoubleSharedTable() {
		final FastDoubleTable table = new FastDoubleTable();
		table.add(1.0);
		table.add(2.0);
		final FastDoubleTable shared = (FastDoubleTable) table.shared();
		final FastDoubleIterator iterator = (FastDoubleIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		TestContext.assertTrue(iterator.next() == 1.0);
		iterator.remove();
		TestContext.assertEquals(1, shared.size());
		TestContext.assertTrue(shared.get(0) == 2.0);
		final FastDoubleTable source = new FastDoubleTable(new double[] { 1.0, 2.0 });
		final FastDoubleTable unmodifiable = (FastDoubleTable) source.unmodifiable();
		final FastDoubleTable fromView = new FastDoubleTable();
		TestContext.assertTrue(fromView.addAll(unmodifiable));
		TestContext.assertTrue(source.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(source));
		final FastDoubleTable sourceShared = (FastDoubleTable) source.shared();
		source.add(3.0);
		TestContext.assertEquals(3, unmodifiable.size());
		TestContext.assertEquals(3, sourceShared.size());
		TestContext.assertTrue(unmodifiable.get(2) == 3.0);
		TestContext.assertTrue(sourceShared.get(2) == 3.0);
		source.remove(2);
		TestContext.assertEquals(2, unmodifiable.size());
		TestContext.assertEquals(2, sourceShared.size());
		final FastDoubleTable fromShared = new FastDoubleTable();
		TestContext.assertTrue(fromShared.addAll((FastDoubleTable) source.shared()));
		final FastDoubleTable unmodifiableOwner = new FastDoubleTable(new double[] { 1.0, 2.0 });
		TestContext.assertTrue(unmodifiableOwner.addAll(1,
				(FastDoubleTable) unmodifiableOwner.unmodifiable()));
		TestContext.assertTrue(unmodifiableOwner.equals(new FastDoubleTable(new double[] { 1.0, 1.0, 2.0, 2.0 })));
		final FastDoubleTable sharedOwner = new FastDoubleTable(new double[] { 1.0, 2.0 });
		final FastDoubleTable ownedShared = (FastDoubleTable) sharedOwner.shared();
		TestContext.assertTrue(sharedOwner.addAll(1,
				ownedShared));
		TestContext.assertTrue(sharedOwner.equals(new FastDoubleTable(new double[] { 1.0, 1.0, 2.0, 2.0 })));
		final FastDoubleTable receiverBacking = new FastDoubleTable(
				new double[] { 1.0, 2.0 });
		final FastDoubleTable receiverShared = (FastDoubleTable) receiverBacking.shared();
		TestContext.assertTrue(receiverShared.addAll(1, receiverBacking));
		TestContext.assertTrue(receiverShared.equals(new FastDoubleTable(
				new double[] { 1.0, 1.0, 2.0, 2.0 })));
	}
	private void checkDoubleSharedSet() {
		final FastDoubleSet set = new FastDoubleSet();
		set.add(1.0);
		set.add(2.0);
		final FastDoubleSet shared = (FastDoubleSet) set.shared();
		final FastDoubleIterator iterator = (FastDoubleIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final double removed = iterator.next();
		iterator.remove();
		TestContext.assertFalse(shared.contains(removed));
		TestContext.assertEquals(1, shared.size());
		final FastDoubleSet source = new FastDoubleSet(new double[] { 1.0, 2.0 });
		final FastDoubleSet unmodifiable = (FastDoubleSet) source.unmodifiable();
		final FastDoubleSet copied = new FastDoubleSet(unmodifiable);
		TestContext.assertTrue(copied.equals(source));
		final FastDoubleSet sourceShared = (FastDoubleSet) source.shared();
		TestContext.assertTrue(source.remove(1.0));
		TestContext.assertFalse(unmodifiable.contains(1.0));
		TestContext.assertFalse(sourceShared.contains(1.0));
		TestContext.assertTrue(source.add(1.0));
		TestContext.assertTrue(unmodifiable.contains(1.0));
		TestContext.assertTrue(sourceShared.contains(1.0));
		final FastDoubleSet target = (FastDoubleSet) new FastDoubleSet().shared();
		TestContext.assertTrue(target.addAll(unmodifiable));
		TestContext.assertTrue(target.containsAll(unmodifiable));
		TestContext.assertTrue(target.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(target));
		TestContext.assertTrue(target.removeAll((FastDoubleSet) source.shared()));
		TestContext.assertEquals(0, target.size());
		TestContext.assertTrue(target.addAll(unmodifiable));
		final FastDoubleSet retained = new FastDoubleSet(new double[] { 2.0 });
		TestContext.assertTrue(target.retainAll((FastDoubleSet) retained.shared()));
		TestContext.assertEquals(1, target.size());
		TestContext.assertTrue(target.contains(2.0));
	}
	private void checkFloatCapacity(int capacity) {
		final FastFloatTable table = new FastFloatTable(capacity);
		for(int i = 0; i < capacity; ++i) {
			table.add((float) i);
		}
		TestContext.assertEquals(capacity, table.size());
		for(int i = 0; i < capacity; ++i) {
			TestContext.assertTrue(table.get(i) == (float) i);
		}
		table.trimToSize();
		TestContext.assertEquals(capacity, table.size());
	}
	private void checkFloatSelfInsertion(int index) {
		final int originalSize = 1030;
		final float[] values = new float[originalSize];
		for(int i = 0; i < originalSize; ++i) {
			values[i] = (float) i;
		}
		final FastFloatTable table = new FastFloatTable(values);
		TestContext.assertTrue(table.addAll(index, table));
		TestContext.assertEquals(originalSize << 1, table.size());
		for(int i = 0; i < table.size(); ++i) {
			final int source = i < index ? i
					: i < index + originalSize ? i - index : i - originalSize;
			TestContext.assertTrue(table.get(i) == values[source]);
		}
		table.trimToSize();
	}
	private void checkFloatSharedTable() {
		final FastFloatTable table = new FastFloatTable();
		table.add(1.0f);
		table.add(2.0f);
		final FastFloatTable shared = (FastFloatTable) table.shared();
		final FastFloatIterator iterator = (FastFloatIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		TestContext.assertTrue(iterator.next() == 1.0f);
		iterator.remove();
		TestContext.assertEquals(1, shared.size());
		TestContext.assertTrue(shared.get(0) == 2.0f);
		final FastFloatTable source = new FastFloatTable(new float[] { 1.0f, 2.0f });
		final FastFloatTable unmodifiable = (FastFloatTable) source.unmodifiable();
		final FastFloatTable fromView = new FastFloatTable();
		TestContext.assertTrue(fromView.addAll(unmodifiable));
		TestContext.assertTrue(source.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(source));
		final FastFloatTable sourceShared = (FastFloatTable) source.shared();
		source.add(3.0f);
		TestContext.assertEquals(3, unmodifiable.size());
		TestContext.assertEquals(3, sourceShared.size());
		TestContext.assertTrue(unmodifiable.get(2) == 3.0f);
		TestContext.assertTrue(sourceShared.get(2) == 3.0f);
		source.remove(2);
		TestContext.assertEquals(2, unmodifiable.size());
		TestContext.assertEquals(2, sourceShared.size());
		final FastFloatTable fromShared = new FastFloatTable();
		TestContext.assertTrue(fromShared.addAll((FastFloatTable) source.shared()));
		final FastFloatTable unmodifiableOwner = new FastFloatTable(new float[] { 1.0f, 2.0f });
		TestContext.assertTrue(unmodifiableOwner.addAll(1,
				(FastFloatTable) unmodifiableOwner.unmodifiable()));
		TestContext.assertTrue(unmodifiableOwner.equals(new FastFloatTable(new float[] { 1.0f, 1.0f, 2.0f, 2.0f })));
		final FastFloatTable sharedOwner = new FastFloatTable(new float[] { 1.0f, 2.0f });
		final FastFloatTable ownedShared = (FastFloatTable) sharedOwner.shared();
		TestContext.assertTrue(sharedOwner.addAll(1,
				ownedShared));
		TestContext.assertTrue(sharedOwner.equals(new FastFloatTable(new float[] { 1.0f, 1.0f, 2.0f, 2.0f })));
		final FastFloatTable receiverBacking = new FastFloatTable(
				new float[] { 1.0f, 2.0f });
		final FastFloatTable receiverShared = (FastFloatTable) receiverBacking.shared();
		TestContext.assertTrue(receiverShared.addAll(1, receiverBacking));
		TestContext.assertTrue(receiverShared.equals(new FastFloatTable(
				new float[] { 1.0f, 1.0f, 2.0f, 2.0f })));
	}
	private void checkFloatSharedSet() {
		final FastFloatSet set = new FastFloatSet();
		set.add(1.0f);
		set.add(2.0f);
		final FastFloatSet shared = (FastFloatSet) set.shared();
		final FastFloatIterator iterator = (FastFloatIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final float removed = iterator.next();
		iterator.remove();
		TestContext.assertFalse(shared.contains(removed));
		TestContext.assertEquals(1, shared.size());
		final FastFloatSet source = new FastFloatSet(new float[] { 1.0f, 2.0f });
		final FastFloatSet unmodifiable = (FastFloatSet) source.unmodifiable();
		final FastFloatSet copied = new FastFloatSet(unmodifiable);
		TestContext.assertTrue(copied.equals(source));
		final FastFloatSet sourceShared = (FastFloatSet) source.shared();
		TestContext.assertTrue(source.remove(1.0f));
		TestContext.assertFalse(unmodifiable.contains(1.0f));
		TestContext.assertFalse(sourceShared.contains(1.0f));
		TestContext.assertTrue(source.add(1.0f));
		TestContext.assertTrue(unmodifiable.contains(1.0f));
		TestContext.assertTrue(sourceShared.contains(1.0f));
		final FastFloatSet target = (FastFloatSet) new FastFloatSet().shared();
		TestContext.assertTrue(target.addAll(unmodifiable));
		TestContext.assertTrue(target.containsAll(unmodifiable));
		TestContext.assertTrue(target.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(target));
		TestContext.assertTrue(target.removeAll((FastFloatSet) source.shared()));
		TestContext.assertEquals(0, target.size());
		TestContext.assertTrue(target.addAll(unmodifiable));
		final FastFloatSet retained = new FastFloatSet(new float[] { 2.0f });
		TestContext.assertTrue(target.retainAll((FastFloatSet) retained.shared()));
		TestContext.assertEquals(1, target.size());
		TestContext.assertTrue(target.contains(2.0f));
	}
	private void checkIntCapacity(int capacity) {
		final FastIntTable table = new FastIntTable(capacity);
		for(int i = 0; i < capacity; ++i) {
			table.add(i);
		}
		TestContext.assertEquals(capacity, table.size());
		for(int i = 0; i < capacity; ++i) {
			TestContext.assertTrue(table.get(i) == i);
		}
		table.trimToSize();
		TestContext.assertEquals(capacity, table.size());
	}
	private void checkIntSelfInsertion(int index) {
		final int originalSize = 1030;
		final int[] values = new int[originalSize];
		for(int i = 0; i < originalSize; ++i) {
			values[i] = i;
		}
		final FastIntTable table = new FastIntTable(values);
		TestContext.assertTrue(table.addAll(index, table));
		TestContext.assertEquals(originalSize << 1, table.size());
		for(int i = 0; i < table.size(); ++i) {
			final int source = i < index ? i
					: i < index + originalSize ? i - index : i - originalSize;
			TestContext.assertTrue(table.get(i) == values[source]);
		}
		table.trimToSize();
	}
	private void checkIntSharedTable() {
		final FastIntTable table = new FastIntTable();
		table.add(1);
		table.add(2);
		final FastIntTable shared = (FastIntTable) table.shared();
		final FastIntIterator iterator = (FastIntIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		TestContext.assertTrue(iterator.next() == 1);
		iterator.remove();
		TestContext.assertEquals(1, shared.size());
		TestContext.assertTrue(shared.get(0) == 2);
		final FastIntTable source = new FastIntTable(new int[] { 1, 2 });
		final FastIntTable unmodifiable = (FastIntTable) source.unmodifiable();
		final FastIntTable fromView = new FastIntTable();
		TestContext.assertTrue(fromView.addAll(unmodifiable));
		TestContext.assertTrue(source.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(source));
		final FastIntTable sourceShared = (FastIntTable) source.shared();
		source.add(3);
		TestContext.assertEquals(3, unmodifiable.size());
		TestContext.assertEquals(3, sourceShared.size());
		TestContext.assertTrue(unmodifiable.get(2) == 3);
		TestContext.assertTrue(sourceShared.get(2) == 3);
		source.remove(2);
		TestContext.assertEquals(2, unmodifiable.size());
		TestContext.assertEquals(2, sourceShared.size());
		final FastIntTable fromShared = new FastIntTable();
		TestContext.assertTrue(fromShared.addAll((FastIntTable) source.shared()));
		final FastIntTable unmodifiableOwner = new FastIntTable(new int[] { 1, 2 });
		TestContext.assertTrue(unmodifiableOwner.addAll(1,
				(FastIntTable) unmodifiableOwner.unmodifiable()));
		TestContext.assertTrue(unmodifiableOwner.equals(new FastIntTable(new int[] { 1, 1, 2, 2 })));
		final FastIntTable sharedOwner = new FastIntTable(new int[] { 1, 2 });
		final FastIntTable ownedShared = (FastIntTable) sharedOwner.shared();
		TestContext.assertTrue(sharedOwner.addAll(1,
				ownedShared));
		TestContext.assertTrue(sharedOwner.equals(new FastIntTable(new int[] { 1, 1, 2, 2 })));
		final FastIntTable receiverBacking = new FastIntTable(
				new int[] { 1, 2 });
		final FastIntTable receiverShared = (FastIntTable) receiverBacking.shared();
		TestContext.assertTrue(receiverShared.addAll(1, receiverBacking));
		TestContext.assertTrue(receiverShared.equals(new FastIntTable(
				new int[] { 1, 1, 2, 2 })));
	}
	private void checkIntSharedSet() {
		final FastIntSet set = new FastIntSet();
		set.add(1);
		set.add(2);
		final FastIntSet shared = (FastIntSet) set.shared();
		final FastIntIterator iterator = (FastIntIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final int removed = iterator.next();
		iterator.remove();
		TestContext.assertFalse(shared.contains(removed));
		TestContext.assertEquals(1, shared.size());
		final FastIntSet source = new FastIntSet(new int[] { 1, 2 });
		final FastIntSet unmodifiable = (FastIntSet) source.unmodifiable();
		final FastIntSet copied = new FastIntSet(unmodifiable);
		TestContext.assertTrue(copied.equals(source));
		final FastIntSet sourceShared = (FastIntSet) source.shared();
		TestContext.assertTrue(source.remove(1));
		TestContext.assertFalse(unmodifiable.contains(1));
		TestContext.assertFalse(sourceShared.contains(1));
		TestContext.assertTrue(source.add(1));
		TestContext.assertTrue(unmodifiable.contains(1));
		TestContext.assertTrue(sourceShared.contains(1));
		final FastIntSet target = (FastIntSet) new FastIntSet().shared();
		TestContext.assertTrue(target.addAll(unmodifiable));
		TestContext.assertTrue(target.containsAll(unmodifiable));
		TestContext.assertTrue(target.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(target));
		TestContext.assertTrue(target.removeAll((FastIntSet) source.shared()));
		TestContext.assertEquals(0, target.size());
		TestContext.assertTrue(target.addAll(unmodifiable));
		final FastIntSet retained = new FastIntSet(new int[] { 2 });
		TestContext.assertTrue(target.retainAll((FastIntSet) retained.shared()));
		TestContext.assertEquals(1, target.size());
		TestContext.assertTrue(target.contains(2));
	}
	private void checkLongCapacity(int capacity) {
		final FastLongTable table = new FastLongTable(capacity);
		for(int i = 0; i < capacity; ++i) {
			table.add((long) i);
		}
		TestContext.assertEquals(capacity, table.size());
		for(int i = 0; i < capacity; ++i) {
			TestContext.assertTrue(table.get(i) == (long) i);
		}
		table.trimToSize();
		TestContext.assertEquals(capacity, table.size());
	}
	private void checkLongSelfInsertion(int index) {
		final int originalSize = 1030;
		final long[] values = new long[originalSize];
		for(int i = 0; i < originalSize; ++i) {
			values[i] = (long) i;
		}
		final FastLongTable table = new FastLongTable(values);
		TestContext.assertTrue(table.addAll(index, table));
		TestContext.assertEquals(originalSize << 1, table.size());
		for(int i = 0; i < table.size(); ++i) {
			final int source = i < index ? i
					: i < index + originalSize ? i - index : i - originalSize;
			TestContext.assertTrue(table.get(i) == values[source]);
		}
		table.trimToSize();
	}
	private void checkLongSharedTable() {
		final FastLongTable table = new FastLongTable();
		table.add(1L);
		table.add(2L);
		final FastLongTable shared = (FastLongTable) table.shared();
		final FastLongIterator iterator = (FastLongIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		TestContext.assertTrue(iterator.next() == 1L);
		iterator.remove();
		TestContext.assertEquals(1, shared.size());
		TestContext.assertTrue(shared.get(0) == 2L);
		final FastLongTable source = new FastLongTable(new long[] { 1L, 2L });
		final FastLongTable unmodifiable = (FastLongTable) source.unmodifiable();
		final FastLongTable fromView = new FastLongTable();
		TestContext.assertTrue(fromView.addAll(unmodifiable));
		TestContext.assertTrue(source.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(source));
		final FastLongTable sourceShared = (FastLongTable) source.shared();
		source.add(3L);
		TestContext.assertEquals(3, unmodifiable.size());
		TestContext.assertEquals(3, sourceShared.size());
		TestContext.assertTrue(unmodifiable.get(2) == 3L);
		TestContext.assertTrue(sourceShared.get(2) == 3L);
		source.remove(2);
		TestContext.assertEquals(2, unmodifiable.size());
		TestContext.assertEquals(2, sourceShared.size());
		final FastLongTable fromShared = new FastLongTable();
		TestContext.assertTrue(fromShared.addAll((FastLongTable) source.shared()));
		final FastLongTable unmodifiableOwner = new FastLongTable(new long[] { 1L, 2L });
		TestContext.assertTrue(unmodifiableOwner.addAll(1,
				(FastLongTable) unmodifiableOwner.unmodifiable()));
		TestContext.assertTrue(unmodifiableOwner.equals(new FastLongTable(new long[] { 1L, 1L, 2L, 2L })));
		final FastLongTable sharedOwner = new FastLongTable(new long[] { 1L, 2L });
		final FastLongTable ownedShared = (FastLongTable) sharedOwner.shared();
		TestContext.assertTrue(sharedOwner.addAll(1,
				ownedShared));
		TestContext.assertTrue(sharedOwner.equals(new FastLongTable(new long[] { 1L, 1L, 2L, 2L })));
		final FastLongTable receiverBacking = new FastLongTable(
				new long[] { 1L, 2L });
		final FastLongTable receiverShared = (FastLongTable) receiverBacking.shared();
		TestContext.assertTrue(receiverShared.addAll(1, receiverBacking));
		TestContext.assertTrue(receiverShared.equals(new FastLongTable(
				new long[] { 1L, 1L, 2L, 2L })));
	}
	private void checkLongSharedSet() {
		final FastLongSet set = new FastLongSet();
		set.add(1L);
		set.add(2L);
		final FastLongSet shared = (FastLongSet) set.shared();
		final FastLongIterator iterator = (FastLongIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final long removed = iterator.next();
		iterator.remove();
		TestContext.assertFalse(shared.contains(removed));
		TestContext.assertEquals(1, shared.size());
		final FastLongSet source = new FastLongSet(new long[] { 1L, 2L });
		final FastLongSet unmodifiable = (FastLongSet) source.unmodifiable();
		final FastLongSet copied = new FastLongSet(unmodifiable);
		TestContext.assertTrue(copied.equals(source));
		final FastLongSet sourceShared = (FastLongSet) source.shared();
		TestContext.assertTrue(source.remove(1L));
		TestContext.assertFalse(unmodifiable.contains(1L));
		TestContext.assertFalse(sourceShared.contains(1L));
		TestContext.assertTrue(source.add(1L));
		TestContext.assertTrue(unmodifiable.contains(1L));
		TestContext.assertTrue(sourceShared.contains(1L));
		final FastLongSet target = (FastLongSet) new FastLongSet().shared();
		TestContext.assertTrue(target.addAll(unmodifiable));
		TestContext.assertTrue(target.containsAll(unmodifiable));
		TestContext.assertTrue(target.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(target));
		TestContext.assertTrue(target.removeAll((FastLongSet) source.shared()));
		TestContext.assertEquals(0, target.size());
		TestContext.assertTrue(target.addAll(unmodifiable));
		final FastLongSet retained = new FastLongSet(new long[] { 2L });
		TestContext.assertTrue(target.retainAll((FastLongSet) retained.shared()));
		TestContext.assertEquals(1, target.size());
		TestContext.assertTrue(target.contains(2L));
	}
	private void checkShortCapacity(int capacity) {
		final FastShortTable table = new FastShortTable(capacity);
		for(int i = 0; i < capacity; ++i) {
			table.add((short) i);
		}
		TestContext.assertEquals(capacity, table.size());
		for(int i = 0; i < capacity; ++i) {
			TestContext.assertTrue(table.get(i) == (short) i);
		}
		table.trimToSize();
		TestContext.assertEquals(capacity, table.size());
	}
	private void checkShortSelfInsertion(int index) {
		final int originalSize = 1030;
		final short[] values = new short[originalSize];
		for(int i = 0; i < originalSize; ++i) {
			values[i] = (short) i;
		}
		final FastShortTable table = new FastShortTable(values);
		TestContext.assertTrue(table.addAll(index, table));
		TestContext.assertEquals(originalSize << 1, table.size());
		for(int i = 0; i < table.size(); ++i) {
			final int source = i < index ? i
					: i < index + originalSize ? i - index : i - originalSize;
			TestContext.assertTrue(table.get(i) == values[source]);
		}
		table.trimToSize();
	}
	private void checkShortSharedTable() {
		final FastShortTable table = new FastShortTable();
		table.add((short) 1);
		table.add((short) 2);
		final FastShortTable shared = (FastShortTable) table.shared();
		final FastShortIterator iterator = (FastShortIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		TestContext.assertTrue(iterator.next() == (short) 1);
		iterator.remove();
		TestContext.assertEquals(1, shared.size());
		TestContext.assertTrue(shared.get(0) == (short) 2);
		final FastShortTable source = new FastShortTable(new short[] { (short) 1, (short) 2 });
		final FastShortTable unmodifiable = (FastShortTable) source.unmodifiable();
		final FastShortTable fromView = new FastShortTable();
		TestContext.assertTrue(fromView.addAll(unmodifiable));
		TestContext.assertTrue(source.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(source));
		final FastShortTable sourceShared = (FastShortTable) source.shared();
		source.add((short) 3);
		TestContext.assertEquals(3, unmodifiable.size());
		TestContext.assertEquals(3, sourceShared.size());
		TestContext.assertTrue(unmodifiable.get(2) == (short) 3);
		TestContext.assertTrue(sourceShared.get(2) == (short) 3);
		source.remove(2);
		TestContext.assertEquals(2, unmodifiable.size());
		TestContext.assertEquals(2, sourceShared.size());
		final FastShortTable fromShared = new FastShortTable();
		TestContext.assertTrue(fromShared.addAll((FastShortTable) source.shared()));
		final FastShortTable unmodifiableOwner = new FastShortTable(new short[] { (short) 1, (short) 2 });
		TestContext.assertTrue(unmodifiableOwner.addAll(1,
				(FastShortTable) unmodifiableOwner.unmodifiable()));
		TestContext.assertTrue(unmodifiableOwner.equals(new FastShortTable(new short[] { (short) 1, (short) 1, (short) 2, (short) 2 })));
		final FastShortTable sharedOwner = new FastShortTable(new short[] { (short) 1, (short) 2 });
		final FastShortTable ownedShared = (FastShortTable) sharedOwner.shared();
		TestContext.assertTrue(sharedOwner.addAll(1,
				ownedShared));
		TestContext.assertTrue(sharedOwner.equals(new FastShortTable(new short[] { (short) 1, (short) 1, (short) 2, (short) 2 })));
		final FastShortTable receiverBacking = new FastShortTable(
				new short[] { (short) 1, (short) 2 });
		final FastShortTable receiverShared = (FastShortTable) receiverBacking.shared();
		TestContext.assertTrue(receiverShared.addAll(1, receiverBacking));
		TestContext.assertTrue(receiverShared.equals(new FastShortTable(
				new short[] { (short) 1, (short) 1, (short) 2, (short) 2 })));
	}
	private void checkShortSharedSet() {
		final FastShortSet set = new FastShortSet();
		set.add((short) 1);
		set.add((short) 2);
		final FastShortSet shared = (FastShortSet) set.shared();
		final FastShortIterator iterator = (FastShortIterator) shared.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final short removed = iterator.next();
		iterator.remove();
		TestContext.assertFalse(shared.contains(removed));
		TestContext.assertEquals(1, shared.size());
		final FastShortSet source = new FastShortSet(new short[] { (short) 1, (short) 2 });
		final FastShortSet unmodifiable = (FastShortSet) source.unmodifiable();
		final FastShortSet copied = new FastShortSet(unmodifiable);
		TestContext.assertTrue(copied.equals(source));
		final FastShortSet sourceShared = (FastShortSet) source.shared();
		TestContext.assertTrue(source.remove((short) 1));
		TestContext.assertFalse(unmodifiable.contains((short) 1));
		TestContext.assertFalse(sourceShared.contains((short) 1));
		TestContext.assertTrue(source.add((short) 1));
		TestContext.assertTrue(unmodifiable.contains((short) 1));
		TestContext.assertTrue(sourceShared.contains((short) 1));
		final FastShortSet target = (FastShortSet) new FastShortSet().shared();
		TestContext.assertTrue(target.addAll(unmodifiable));
		TestContext.assertTrue(target.containsAll(unmodifiable));
		TestContext.assertTrue(target.equals(unmodifiable));
		TestContext.assertTrue(unmodifiable.equals(target));
		TestContext.assertTrue(target.removeAll((FastShortSet) source.shared()));
		TestContext.assertEquals(0, target.size());
		TestContext.assertTrue(target.addAll(unmodifiable));
		final FastShortSet retained = new FastShortSet(new short[] { (short) 2 });
		TestContext.assertTrue(target.retainAll((FastShortSet) retained.shared()));
		TestContext.assertEquals(1, target.size());
		TestContext.assertTrue(target.contains((short) 2));
	}
	private void checkBooleanSetEmptyFastPaths() {
		final boolean[] emptyValues = new boolean[0];
		final boolean[] presentValues = new boolean[] { true };
		final FastBooleanSet emptySource = new FastBooleanSet();
		final FastBooleanSet source = new FastBooleanSet(presentValues);
		final FastBooleanSet set = new FastBooleanSet();
		TestContext.assertTrue(set.containsAll((boolean[]) null));
		TestContext.assertTrue(set.containsAll(emptyValues));
		TestContext.assertTrue(set.containsAll((FastBooleanSet) null));
		TestContext.assertTrue(set.containsAll(emptySource));
		TestContext.assertFalse(set.containsAll(presentValues));
		TestContext.assertFalse(set.containsAll(source));
		TestContext.assertFalse(set.contains(true));
		TestContext.assertFalse(set.remove(true));
		set.clear();
		final boolean[] firstEmptyArray = set.toArray();
		TestContext.assertEquals(0, firstEmptyArray.length);
		TestContext.assertFalse(firstEmptyArray == set.toArray());
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		final FastBooleanSet shared = (FastBooleanSet) set.shared();
		final FastBooleanSet unmodifiable = (FastBooleanSet) set.unmodifiable();
		final FastBooleanSet emptyView = (FastBooleanSet) emptySource.shared();
		final FastBooleanSet sourceView = (FastBooleanSet) source.unmodifiable();
		TestContext.assertTrue(shared.containsAll((boolean[]) null));
		TestContext.assertTrue(unmodifiable.containsAll((FastBooleanSet) null));
		TestContext.assertTrue(shared.containsAll(emptyValues));
		TestContext.assertTrue(unmodifiable.containsAll(emptyView));
		TestContext.assertFalse(shared.containsAll(presentValues));
		TestContext.assertFalse(unmodifiable.containsAll(sourceView));
		TestContext.assertFalse(shared.contains(true));
		TestContext.assertFalse(shared.remove(true));
		TestContext.assertFalse(unmodifiable.contains(true));
		TestContext.assertEquals(0, shared.toArray().length);
		TestContext.assertEquals(0, unmodifiable.toArray().length);
		TestContext.assertEquals(0, shared.hashCode());
		TestContext.assertEquals(0, unmodifiable.hashCode());
		TestContext.assertFalse(shared.iterator().hasNext());
		TestContext.assertFalse(unmodifiable.iterator().hasNext());
		shared.clear();
		TestContext.assertTrue(shared.add(true));
		TestContext.assertTrue(set.containsAll(presentValues));
		TestContext.assertTrue(shared.containsAll(sourceView));
		TestContext.assertTrue(unmodifiable.containsAll(sourceView));
	}
	private void checkByteSetEmptyFastPaths() {
		final byte[] emptyValues = new byte[0];
		final byte[] presentValues = new byte[] { (byte) 7 };
		final FastByteSet emptySource = new FastByteSet();
		final FastByteSet source = new FastByteSet(presentValues);
		final FastByteSet set = new FastByteSet(16);
		TestContext.assertTrue(set.containsAll((byte[]) null));
		TestContext.assertTrue(set.containsAll(emptyValues));
		TestContext.assertTrue(set.containsAll((FastByteSet) null));
		TestContext.assertTrue(set.containsAll(emptySource));
		TestContext.assertFalse(set.containsAll(presentValues));
		TestContext.assertFalse(set.containsAll(source));
		TestContext.assertFalse(set.contains((byte) 7));
		TestContext.assertFalse(set.remove((byte) 7));
		set.clear();
		final byte[] firstEmptyArray = set.toArray();
		TestContext.assertEquals(0, firstEmptyArray.length);
		TestContext.assertFalse(firstEmptyArray == set.toArray());
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		TestContext.assertTrue(set.add((byte) 1));
		TestContext.assertTrue(set.add((byte) 2));
		TestContext.assertTrue(set.remove((byte) 1));
		TestContext.assertTrue(set.remove((byte) 2));
		TestContext.assertEquals(0, set.size());
		TestContext.assertFalse(set.contains((byte) 7));
		TestContext.assertFalse(set.remove((byte) 7));
		TestContext.assertEquals(0, set.toArray().length);
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		final FastByteSet shared = (FastByteSet) set.shared();
		final FastByteSet unmodifiable = (FastByteSet) set.unmodifiable();
		final FastByteSet emptyView = (FastByteSet) emptySource.shared();
		final FastByteSet sourceView = (FastByteSet) source.unmodifiable();
		TestContext.assertTrue(shared.containsAll((byte[]) null));
		TestContext.assertTrue(unmodifiable.containsAll((FastByteSet) null));
		TestContext.assertTrue(shared.containsAll(emptyValues));
		TestContext.assertTrue(unmodifiable.containsAll(emptyView));
		TestContext.assertFalse(shared.containsAll(presentValues));
		TestContext.assertFalse(unmodifiable.containsAll(sourceView));
		TestContext.assertFalse(shared.contains((byte) 7));
		TestContext.assertFalse(shared.remove((byte) 7));
		TestContext.assertFalse(unmodifiable.contains((byte) 7));
		TestContext.assertEquals(0, shared.toArray().length);
		TestContext.assertEquals(0, unmodifiable.toArray().length);
		TestContext.assertEquals(0, shared.hashCode());
		TestContext.assertEquals(0, unmodifiable.hashCode());
		TestContext.assertFalse(shared.iterator().hasNext());
		TestContext.assertFalse(unmodifiable.iterator().hasNext());
		shared.clear();
		TestContext.assertTrue(shared.add((byte) 7));
		TestContext.assertTrue(set.containsAll(presentValues));
		TestContext.assertTrue(shared.containsAll(sourceView));
		TestContext.assertTrue(unmodifiable.containsAll(sourceView));
	}
	private void checkCharSetEmptyFastPaths() {
		final char[] emptyValues = new char[0];
		final char[] presentValues = new char[] { (char) 7 };
		final FastCharSet emptySource = new FastCharSet();
		final FastCharSet source = new FastCharSet(presentValues);
		final FastCharSet set = new FastCharSet(16);
		TestContext.assertTrue(set.containsAll((char[]) null));
		TestContext.assertTrue(set.containsAll(emptyValues));
		TestContext.assertTrue(set.containsAll((FastCharSet) null));
		TestContext.assertTrue(set.containsAll(emptySource));
		TestContext.assertFalse(set.containsAll(presentValues));
		TestContext.assertFalse(set.containsAll(source));
		TestContext.assertFalse(set.contains((char) 7));
		TestContext.assertFalse(set.remove((char) 7));
		set.clear();
		final char[] firstEmptyArray = set.toArray();
		TestContext.assertEquals(0, firstEmptyArray.length);
		TestContext.assertFalse(firstEmptyArray == set.toArray());
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		TestContext.assertTrue(set.add((char) 1));
		TestContext.assertTrue(set.add((char) 2));
		TestContext.assertTrue(set.remove((char) 1));
		TestContext.assertTrue(set.remove((char) 2));
		TestContext.assertEquals(0, set.size());
		TestContext.assertFalse(set.contains((char) 7));
		TestContext.assertFalse(set.remove((char) 7));
		TestContext.assertEquals(0, set.toArray().length);
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		final FastCharSet shared = (FastCharSet) set.shared();
		final FastCharSet unmodifiable = (FastCharSet) set.unmodifiable();
		final FastCharSet emptyView = (FastCharSet) emptySource.shared();
		final FastCharSet sourceView = (FastCharSet) source.unmodifiable();
		TestContext.assertTrue(shared.containsAll((char[]) null));
		TestContext.assertTrue(unmodifiable.containsAll((FastCharSet) null));
		TestContext.assertTrue(shared.containsAll(emptyValues));
		TestContext.assertTrue(unmodifiable.containsAll(emptyView));
		TestContext.assertFalse(shared.containsAll(presentValues));
		TestContext.assertFalse(unmodifiable.containsAll(sourceView));
		TestContext.assertFalse(shared.contains((char) 7));
		TestContext.assertFalse(shared.remove((char) 7));
		TestContext.assertFalse(unmodifiable.contains((char) 7));
		TestContext.assertEquals(0, shared.toArray().length);
		TestContext.assertEquals(0, unmodifiable.toArray().length);
		TestContext.assertEquals(0, shared.hashCode());
		TestContext.assertEquals(0, unmodifiable.hashCode());
		TestContext.assertFalse(shared.iterator().hasNext());
		TestContext.assertFalse(unmodifiable.iterator().hasNext());
		shared.clear();
		TestContext.assertTrue(shared.add((char) 7));
		TestContext.assertTrue(set.containsAll(presentValues));
		TestContext.assertTrue(shared.containsAll(sourceView));
		TestContext.assertTrue(unmodifiable.containsAll(sourceView));
	}
	private void checkDoubleSetEmptyFastPaths() {
		final double[] emptyValues = new double[0];
		final double[] presentValues = new double[] { 7.25 };
		final FastDoubleSet emptySource = new FastDoubleSet();
		final FastDoubleSet source = new FastDoubleSet(presentValues);
		final FastDoubleSet set = new FastDoubleSet(16);
		TestContext.assertTrue(set.containsAll((double[]) null));
		TestContext.assertTrue(set.containsAll(emptyValues));
		TestContext.assertTrue(set.containsAll((FastDoubleSet) null));
		TestContext.assertTrue(set.containsAll(emptySource));
		TestContext.assertFalse(set.containsAll(presentValues));
		TestContext.assertFalse(set.containsAll(source));
		TestContext.assertFalse(set.contains(7.25));
		TestContext.assertFalse(set.remove(7.25));
		set.clear();
		final double[] firstEmptyArray = set.toArray();
		TestContext.assertEquals(0, firstEmptyArray.length);
		TestContext.assertFalse(firstEmptyArray == set.toArray());
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		TestContext.assertTrue(set.add(1.25));
		TestContext.assertTrue(set.add(2.25));
		TestContext.assertTrue(set.remove(1.25));
		TestContext.assertTrue(set.remove(2.25));
		TestContext.assertEquals(0, set.size());
		TestContext.assertFalse(set.contains(7.25));
		TestContext.assertFalse(set.remove(7.25));
		TestContext.assertEquals(0, set.toArray().length);
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		final FastDoubleSet shared = (FastDoubleSet) set.shared();
		final FastDoubleSet unmodifiable = (FastDoubleSet) set.unmodifiable();
		final FastDoubleSet emptyView = (FastDoubleSet) emptySource.shared();
		final FastDoubleSet sourceView = (FastDoubleSet) source.unmodifiable();
		TestContext.assertTrue(shared.containsAll((double[]) null));
		TestContext.assertTrue(unmodifiable.containsAll((FastDoubleSet) null));
		TestContext.assertTrue(shared.containsAll(emptyValues));
		TestContext.assertTrue(unmodifiable.containsAll(emptyView));
		TestContext.assertFalse(shared.containsAll(presentValues));
		TestContext.assertFalse(unmodifiable.containsAll(sourceView));
		TestContext.assertFalse(shared.contains(7.25));
		TestContext.assertFalse(shared.remove(7.25));
		TestContext.assertFalse(unmodifiable.contains(7.25));
		TestContext.assertEquals(0, shared.toArray().length);
		TestContext.assertEquals(0, unmodifiable.toArray().length);
		TestContext.assertEquals(0, shared.hashCode());
		TestContext.assertEquals(0, unmodifiable.hashCode());
		TestContext.assertFalse(shared.iterator().hasNext());
		TestContext.assertFalse(unmodifiable.iterator().hasNext());
		shared.clear();
		TestContext.assertTrue(shared.add(7.25));
		TestContext.assertTrue(set.containsAll(presentValues));
		TestContext.assertTrue(shared.containsAll(sourceView));
		TestContext.assertTrue(unmodifiable.containsAll(sourceView));
	}
	private void checkFloatSetEmptyFastPaths() {
		final float[] emptyValues = new float[0];
		final float[] presentValues = new float[] { 7.25f };
		final FastFloatSet emptySource = new FastFloatSet();
		final FastFloatSet source = new FastFloatSet(presentValues);
		final FastFloatSet set = new FastFloatSet(16);
		TestContext.assertTrue(set.containsAll((float[]) null));
		TestContext.assertTrue(set.containsAll(emptyValues));
		TestContext.assertTrue(set.containsAll((FastFloatSet) null));
		TestContext.assertTrue(set.containsAll(emptySource));
		TestContext.assertFalse(set.containsAll(presentValues));
		TestContext.assertFalse(set.containsAll(source));
		TestContext.assertFalse(set.contains(7.25f));
		TestContext.assertFalse(set.remove(7.25f));
		set.clear();
		final float[] firstEmptyArray = set.toArray();
		TestContext.assertEquals(0, firstEmptyArray.length);
		TestContext.assertFalse(firstEmptyArray == set.toArray());
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		TestContext.assertTrue(set.add(1.25f));
		TestContext.assertTrue(set.add(2.25f));
		TestContext.assertTrue(set.remove(1.25f));
		TestContext.assertTrue(set.remove(2.25f));
		TestContext.assertEquals(0, set.size());
		TestContext.assertFalse(set.contains(7.25f));
		TestContext.assertFalse(set.remove(7.25f));
		TestContext.assertEquals(0, set.toArray().length);
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		final FastFloatSet shared = (FastFloatSet) set.shared();
		final FastFloatSet unmodifiable = (FastFloatSet) set.unmodifiable();
		final FastFloatSet emptyView = (FastFloatSet) emptySource.shared();
		final FastFloatSet sourceView = (FastFloatSet) source.unmodifiable();
		TestContext.assertTrue(shared.containsAll((float[]) null));
		TestContext.assertTrue(unmodifiable.containsAll((FastFloatSet) null));
		TestContext.assertTrue(shared.containsAll(emptyValues));
		TestContext.assertTrue(unmodifiable.containsAll(emptyView));
		TestContext.assertFalse(shared.containsAll(presentValues));
		TestContext.assertFalse(unmodifiable.containsAll(sourceView));
		TestContext.assertFalse(shared.contains(7.25f));
		TestContext.assertFalse(shared.remove(7.25f));
		TestContext.assertFalse(unmodifiable.contains(7.25f));
		TestContext.assertEquals(0, shared.toArray().length);
		TestContext.assertEquals(0, unmodifiable.toArray().length);
		TestContext.assertEquals(0, shared.hashCode());
		TestContext.assertEquals(0, unmodifiable.hashCode());
		TestContext.assertFalse(shared.iterator().hasNext());
		TestContext.assertFalse(unmodifiable.iterator().hasNext());
		shared.clear();
		TestContext.assertTrue(shared.add(7.25f));
		TestContext.assertTrue(set.containsAll(presentValues));
		TestContext.assertTrue(shared.containsAll(sourceView));
		TestContext.assertTrue(unmodifiable.containsAll(sourceView));
	}
	private void checkIntSetEmptyFastPaths() {
		final int[] emptyValues = new int[0];
		final int[] presentValues = new int[] { 7 };
		final FastIntSet emptySource = new FastIntSet();
		final FastIntSet source = new FastIntSet(presentValues);
		final FastIntSet set = new FastIntSet(16);
		TestContext.assertTrue(set.containsAll((int[]) null));
		TestContext.assertTrue(set.containsAll(emptyValues));
		TestContext.assertTrue(set.containsAll((FastIntSet) null));
		TestContext.assertTrue(set.containsAll(emptySource));
		TestContext.assertFalse(set.containsAll(presentValues));
		TestContext.assertFalse(set.containsAll(source));
		TestContext.assertFalse(set.contains(7));
		TestContext.assertFalse(set.remove(7));
		set.clear();
		final int[] firstEmptyArray = set.toArray();
		TestContext.assertEquals(0, firstEmptyArray.length);
		TestContext.assertFalse(firstEmptyArray == set.toArray());
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		TestContext.assertTrue(set.add(1));
		TestContext.assertTrue(set.add(2));
		TestContext.assertTrue(set.remove(1));
		TestContext.assertTrue(set.remove(2));
		TestContext.assertEquals(0, set.size());
		TestContext.assertFalse(set.contains(7));
		TestContext.assertFalse(set.remove(7));
		TestContext.assertEquals(0, set.toArray().length);
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		final FastIntSet shared = (FastIntSet) set.shared();
		final FastIntSet unmodifiable = (FastIntSet) set.unmodifiable();
		final FastIntSet emptyView = (FastIntSet) emptySource.shared();
		final FastIntSet sourceView = (FastIntSet) source.unmodifiable();
		TestContext.assertTrue(shared.containsAll((int[]) null));
		TestContext.assertTrue(unmodifiable.containsAll((FastIntSet) null));
		TestContext.assertTrue(shared.containsAll(emptyValues));
		TestContext.assertTrue(unmodifiable.containsAll(emptyView));
		TestContext.assertFalse(shared.containsAll(presentValues));
		TestContext.assertFalse(unmodifiable.containsAll(sourceView));
		TestContext.assertFalse(shared.contains(7));
		TestContext.assertFalse(shared.remove(7));
		TestContext.assertFalse(unmodifiable.contains(7));
		TestContext.assertEquals(0, shared.toArray().length);
		TestContext.assertEquals(0, unmodifiable.toArray().length);
		TestContext.assertEquals(0, shared.hashCode());
		TestContext.assertEquals(0, unmodifiable.hashCode());
		TestContext.assertFalse(shared.iterator().hasNext());
		TestContext.assertFalse(unmodifiable.iterator().hasNext());
		shared.clear();
		TestContext.assertTrue(shared.add(7));
		TestContext.assertTrue(set.containsAll(presentValues));
		TestContext.assertTrue(shared.containsAll(sourceView));
		TestContext.assertTrue(unmodifiable.containsAll(sourceView));
	}
	private void checkLongSetEmptyFastPaths() {
		final long[] emptyValues = new long[0];
		final long[] presentValues = new long[] { 7L };
		final FastLongSet emptySource = new FastLongSet();
		final FastLongSet source = new FastLongSet(presentValues);
		final FastLongSet set = new FastLongSet(16);
		TestContext.assertTrue(set.containsAll((long[]) null));
		TestContext.assertTrue(set.containsAll(emptyValues));
		TestContext.assertTrue(set.containsAll((FastLongSet) null));
		TestContext.assertTrue(set.containsAll(emptySource));
		TestContext.assertFalse(set.containsAll(presentValues));
		TestContext.assertFalse(set.containsAll(source));
		TestContext.assertFalse(set.contains(7L));
		TestContext.assertFalse(set.remove(7L));
		set.clear();
		final long[] firstEmptyArray = set.toArray();
		TestContext.assertEquals(0, firstEmptyArray.length);
		TestContext.assertFalse(firstEmptyArray == set.toArray());
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		TestContext.assertTrue(set.add(1L));
		TestContext.assertTrue(set.add(2L));
		TestContext.assertTrue(set.remove(1L));
		TestContext.assertTrue(set.remove(2L));
		TestContext.assertEquals(0, set.size());
		TestContext.assertFalse(set.contains(7L));
		TestContext.assertFalse(set.remove(7L));
		TestContext.assertEquals(0, set.toArray().length);
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		final FastLongSet shared = (FastLongSet) set.shared();
		final FastLongSet unmodifiable = (FastLongSet) set.unmodifiable();
		final FastLongSet emptyView = (FastLongSet) emptySource.shared();
		final FastLongSet sourceView = (FastLongSet) source.unmodifiable();
		TestContext.assertTrue(shared.containsAll((long[]) null));
		TestContext.assertTrue(unmodifiable.containsAll((FastLongSet) null));
		TestContext.assertTrue(shared.containsAll(emptyValues));
		TestContext.assertTrue(unmodifiable.containsAll(emptyView));
		TestContext.assertFalse(shared.containsAll(presentValues));
		TestContext.assertFalse(unmodifiable.containsAll(sourceView));
		TestContext.assertFalse(shared.contains(7L));
		TestContext.assertFalse(shared.remove(7L));
		TestContext.assertFalse(unmodifiable.contains(7L));
		TestContext.assertEquals(0, shared.toArray().length);
		TestContext.assertEquals(0, unmodifiable.toArray().length);
		TestContext.assertEquals(0, shared.hashCode());
		TestContext.assertEquals(0, unmodifiable.hashCode());
		TestContext.assertFalse(shared.iterator().hasNext());
		TestContext.assertFalse(unmodifiable.iterator().hasNext());
		shared.clear();
		TestContext.assertTrue(shared.add(7L));
		TestContext.assertTrue(set.containsAll(presentValues));
		TestContext.assertTrue(shared.containsAll(sourceView));
		TestContext.assertTrue(unmodifiable.containsAll(sourceView));
	}
	private void checkShortSetEmptyFastPaths() {
		final short[] emptyValues = new short[0];
		final short[] presentValues = new short[] { (short) 7 };
		final FastShortSet emptySource = new FastShortSet();
		final FastShortSet source = new FastShortSet(presentValues);
		final FastShortSet set = new FastShortSet(16);
		TestContext.assertTrue(set.containsAll((short[]) null));
		TestContext.assertTrue(set.containsAll(emptyValues));
		TestContext.assertTrue(set.containsAll((FastShortSet) null));
		TestContext.assertTrue(set.containsAll(emptySource));
		TestContext.assertFalse(set.containsAll(presentValues));
		TestContext.assertFalse(set.containsAll(source));
		TestContext.assertFalse(set.contains((short) 7));
		TestContext.assertFalse(set.remove((short) 7));
		set.clear();
		final short[] firstEmptyArray = set.toArray();
		TestContext.assertEquals(0, firstEmptyArray.length);
		TestContext.assertFalse(firstEmptyArray == set.toArray());
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		TestContext.assertTrue(set.add((short) 1));
		TestContext.assertTrue(set.add((short) 2));
		TestContext.assertTrue(set.remove((short) 1));
		TestContext.assertTrue(set.remove((short) 2));
		TestContext.assertEquals(0, set.size());
		TestContext.assertFalse(set.contains((short) 7));
		TestContext.assertFalse(set.remove((short) 7));
		TestContext.assertEquals(0, set.toArray().length);
		TestContext.assertEquals(0, set.hashCode());
		TestContext.assertFalse(set.iterator().hasNext());
		final FastShortSet shared = (FastShortSet) set.shared();
		final FastShortSet unmodifiable = (FastShortSet) set.unmodifiable();
		final FastShortSet emptyView = (FastShortSet) emptySource.shared();
		final FastShortSet sourceView = (FastShortSet) source.unmodifiable();
		TestContext.assertTrue(shared.containsAll((short[]) null));
		TestContext.assertTrue(unmodifiable.containsAll((FastShortSet) null));
		TestContext.assertTrue(shared.containsAll(emptyValues));
		TestContext.assertTrue(unmodifiable.containsAll(emptyView));
		TestContext.assertFalse(shared.containsAll(presentValues));
		TestContext.assertFalse(unmodifiable.containsAll(sourceView));
		TestContext.assertFalse(shared.contains((short) 7));
		TestContext.assertFalse(shared.remove((short) 7));
		TestContext.assertFalse(unmodifiable.contains((short) 7));
		TestContext.assertEquals(0, shared.toArray().length);
		TestContext.assertEquals(0, unmodifiable.toArray().length);
		TestContext.assertEquals(0, shared.hashCode());
		TestContext.assertEquals(0, unmodifiable.hashCode());
		TestContext.assertFalse(shared.iterator().hasNext());
		TestContext.assertFalse(unmodifiable.iterator().hasNext());
		shared.clear();
		TestContext.assertTrue(shared.add((short) 7));
		TestContext.assertTrue(set.containsAll(presentValues));
		TestContext.assertTrue(shared.containsAll(sourceView));
		TestContext.assertTrue(unmodifiable.containsAll(sourceView));
	}

	private void checkByteSetChurn() {
		final FastByteSet set = new FastByteSet(16);
		for(int round = 0; round < 256; ++round) {
			for(int i = 0; i < 8; ++i) {
				final byte value = (byte) (round * 13 + i);
				TestContext.assertTrue(set.add(value));
			}
			for(int i = 0; i < 8; ++i) {
				final byte value = (byte) (round * 13 + i);
				TestContext.assertTrue(set.remove(value));
			}
			TestContext.assertFalse(set.contains((byte) (round * 13 + 31)));
		}
		TestContext.assertEquals(0, set.size());
		TestContext.assertTrue(set.add((byte) 7));
		TestContext.assertTrue(set.add((byte) 9));
		final FastByteIterator iterator = (FastByteIterator) set.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final byte returned = iterator.next();
		set.ensureCapacity(128);
		final int sizeBeforeRemove = set.size();
		iterator.remove();
		TestContext.assertFalse(set.contains(returned));
		TestContext.assertEquals(sizeBeforeRemove - 1, set.size());
	}
	private void checkCharSetChurn() {
		final FastCharSet set = new FastCharSet(16);
		for(int round = 0; round < 256; ++round) {
			for(int i = 0; i < 8; ++i) {
				final char value = (char) (round * 13 + i);
				TestContext.assertTrue(set.add(value));
			}
			for(int i = 0; i < 8; ++i) {
				final char value = (char) (round * 13 + i);
				TestContext.assertTrue(set.remove(value));
			}
			TestContext.assertFalse(set.contains((char) (round * 13 + 31)));
		}
		TestContext.assertEquals(0, set.size());
		TestContext.assertTrue(set.add((char) 7));
		TestContext.assertTrue(set.add((char) 9));
		final FastCharIterator iterator = (FastCharIterator) set.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final char returned = iterator.next();
		set.ensureCapacity(128);
		final int sizeBeforeRemove = set.size();
		iterator.remove();
		TestContext.assertFalse(set.contains(returned));
		TestContext.assertEquals(sizeBeforeRemove - 1, set.size());
	}
	private void checkDoubleSetChurn() {
		final FastDoubleSet set = new FastDoubleSet(16);
		for(int round = 0; round < 256; ++round) {
			for(int i = 0; i < 8; ++i) {
				final double value = round * 13.0 + i + 0.25;
				TestContext.assertTrue(set.add(value));
			}
			for(int i = 0; i < 8; ++i) {
				final double value = round * 13.0 + i + 0.25;
				TestContext.assertTrue(set.remove(value));
			}
			TestContext.assertFalse(set.contains(round * 13.0 + 31.25));
		}
		TestContext.assertEquals(0, set.size());
		TestContext.assertTrue(set.add(7.25));
		TestContext.assertTrue(set.add(9.25));
		final FastDoubleIterator iterator = (FastDoubleIterator) set.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final double returned = iterator.next();
		set.ensureCapacity(128);
		final int sizeBeforeRemove = set.size();
		iterator.remove();
		TestContext.assertFalse(set.contains(returned));
		TestContext.assertEquals(sizeBeforeRemove - 1, set.size());
	}
	private void checkFloatSetChurn() {
		final FastFloatSet set = new FastFloatSet(16);
		for(int round = 0; round < 256; ++round) {
			for(int i = 0; i < 8; ++i) {
				final float value = round * 13.0f + i + 0.25f;
				TestContext.assertTrue(set.add(value));
			}
			for(int i = 0; i < 8; ++i) {
				final float value = round * 13.0f + i + 0.25f;
				TestContext.assertTrue(set.remove(value));
			}
			TestContext.assertFalse(set.contains(round * 13.0f + 31.25f));
		}
		TestContext.assertEquals(0, set.size());
		TestContext.assertTrue(set.add(7.25f));
		TestContext.assertTrue(set.add(9.25f));
		final FastFloatIterator iterator = (FastFloatIterator) set.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final float returned = iterator.next();
		set.ensureCapacity(128);
		final int sizeBeforeRemove = set.size();
		iterator.remove();
		TestContext.assertFalse(set.contains(returned));
		TestContext.assertEquals(sizeBeforeRemove - 1, set.size());
	}
	private void checkIntSetChurn() {
		final FastIntSet set = new FastIntSet(16);
		for(int round = 0; round < 256; ++round) {
			for(int i = 0; i < 8; ++i) {
				final int value = round * 13 + i;
				TestContext.assertTrue(set.add(value));
			}
			for(int i = 0; i < 8; ++i) {
				final int value = round * 13 + i;
				TestContext.assertTrue(set.remove(value));
			}
			TestContext.assertFalse(set.contains(round * 13 + 31));
		}
		TestContext.assertEquals(0, set.size());
		TestContext.assertTrue(set.add(7));
		TestContext.assertTrue(set.add(9));
		final FastIntIterator iterator = (FastIntIterator) set.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final int returned = iterator.next();
		set.ensureCapacity(128);
		final int sizeBeforeRemove = set.size();
		iterator.remove();
		TestContext.assertFalse(set.contains(returned));
		TestContext.assertEquals(sizeBeforeRemove - 1, set.size());
	}
	private void checkLongSetChurn() {
		final FastLongSet set = new FastLongSet(16);
		for(int round = 0; round < 256; ++round) {
			for(int i = 0; i < 8; ++i) {
				final long value = round * 13L + i;
				TestContext.assertTrue(set.add(value));
			}
			for(int i = 0; i < 8; ++i) {
				final long value = round * 13L + i;
				TestContext.assertTrue(set.remove(value));
			}
			TestContext.assertFalse(set.contains(round * 13L + 31L));
		}
		TestContext.assertEquals(0, set.size());
		TestContext.assertTrue(set.add(7L));
		TestContext.assertTrue(set.add(9L));
		final FastLongIterator iterator = (FastLongIterator) set.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final long returned = iterator.next();
		set.ensureCapacity(128);
		final int sizeBeforeRemove = set.size();
		iterator.remove();
		TestContext.assertFalse(set.contains(returned));
		TestContext.assertEquals(sizeBeforeRemove - 1, set.size());
	}
	private void checkShortSetChurn() {
		final FastShortSet set = new FastShortSet(16);
		for(int round = 0; round < 256; ++round) {
			for(int i = 0; i < 8; ++i) {
				final short value = (short) (round * 13 + i);
				TestContext.assertTrue(set.add(value));
			}
			for(int i = 0; i < 8; ++i) {
				final short value = (short) (round * 13 + i);
				TestContext.assertTrue(set.remove(value));
			}
			TestContext.assertFalse(set.contains((short) (round * 13 + 31)));
		}
		TestContext.assertEquals(0, set.size());
		TestContext.assertTrue(set.add((short) 7));
		TestContext.assertTrue(set.add((short) 9));
		final FastShortIterator iterator = (FastShortIterator) set.iterator();
		TestContext.assertTrue(iterator.hasNext());
		final short returned = iterator.next();
		set.ensureCapacity(128);
		final int sizeBeforeRemove = set.size();
		iterator.remove();
		TestContext.assertFalse(set.contains(returned));
		TestContext.assertEquals(sizeBeforeRemove - 1, set.size());
	}
	private void checkFloatingPointSetSemantics() {
		final FastFloatSet floats = new FastFloatSet();
		TestContext.assertTrue(floats.add(Float.NaN));
		TestContext.assertFalse(floats.add(Float.intBitsToFloat(0x7f800001)));
		TestContext.assertTrue(floats.add(0.0f));
		TestContext.assertTrue(floats.add(-0.0f));
		TestContext.assertEquals(3, floats.size());
		final FastDoubleSet doubles = new FastDoubleSet();
		TestContext.assertTrue(doubles.add(Double.NaN));
		TestContext.assertFalse(doubles.add(Double.longBitsToDouble(0x7ff0000000000001L)));
		TestContext.assertTrue(doubles.add(0.0));
		TestContext.assertTrue(doubles.add(-0.0));
		TestContext.assertEquals(3, doubles.size());
	}
	private void checkConcurrentIntTableWrites() throws InterruptedException {
		final FastIntTable shared = (FastIntTable) new FastIntTable().shared();
		final Thread first = new Thread(new Runnable() {
			public void run() {
				for(int i = 0; i < 1000; ++i) {
					shared.add(i);
				}
			}
		});
		final Thread second = new Thread(new Runnable() {
			public void run() {
				for(int i = 1000; i < 2000; ++i) {
					shared.add(i);
				}
			}
		});
		first.start();
		second.start();
		first.join();
		second.join();
		TestContext.assertEquals(2000, shared.size());
	}
}