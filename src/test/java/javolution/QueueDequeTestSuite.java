/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2026 - Javolution
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package javolution;
import _templates.java.lang.UnsupportedOperationException;
import _templates.java.util.Iterator;
import _templates.javolution.testing.TestCase;
import _templates.javolution.testing.TestContext;
import _templates.javolution.testing.TestSuite;
import _templates.javolution.util.FastList;
import _templates.javolution.util.FastTable;
/**
 * Queue, deque, and stack regression coverage for FastList and FastTable.
 */
public final class QueueDequeTestSuite extends TestSuite {
	public QueueDequeTestSuite() {
		addTest(new FastListQueue());
		addTest(new FastTableQueue());
	}
	private static final class FastListQueue extends TestCase {
		public String getName() {
			return "FastList: queue/deque/stack behavior";
		}
		public void execute() {}
		public void validate() {
			final FastList list = new FastList();
			list.offerLast("b");
			list.offerFirst("a");
			list.push("z");
			TestContext.assertEquals("z", list.pop());
			TestContext.assertEquals("a", list.pollFirst());
			TestContext.assertEquals("b", list.poll());
			TestContext.assertNull(list.poll());

			list.add("a");
			list.add("b");
			final Iterator descending = list.descendingIterator();
			TestContext.assertEquals("b", descending.next());
			TestContext.assertEquals("a", descending.next());
			final FastList unmodifiable = (FastList) list.unmodifiable();
			TestContext.assertException(UnsupportedOperationException.class,
					new Runnable() {
						public void run() {
							unmodifiable.addFirst("x");
						}
					});
		}
	}
	private static final class FastTableQueue extends TestCase {
		public String getName() {
			return "FastTable: queue/deque/stack behavior";
		}
		public void execute() {}
		public void validate() {
			final FastTable table = new FastTable();
			table.offerLast("b");
			table.offerFirst("a");
			table.push("z");
			TestContext.assertEquals("z", table.pop());
			TestContext.assertEquals("a", table.pollFirst());
			TestContext.assertEquals("b", table.poll());
			TestContext.assertNull(table.poll());

			table.add("a");
			table.add("b");
			final Iterator descending = table.descendingIterator();
			TestContext.assertEquals("b", descending.next());
			TestContext.assertEquals("a", descending.next());
			final FastTable shared = (FastTable) table.shared();
			TestContext.assertEquals("a", shared.peekFirst());
			TestContext.assertEquals("b", shared.peekLast());
		}
	}
}