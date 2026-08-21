/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2026 - Javolution
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package javolution;
import _templates.java.util.Iterator;
import _templates.javolution.testing.TestCase;
import _templates.javolution.testing.TestSuite;
/**
 * Single aggregate for all manually invoked Javolution tests.
 */
public final class AllTestSuite extends TestSuite {
	public AllTestSuite() {
		addSuite(new TypeFormatTestSuite());
		addSuite(new ContextTestSuite());
		addSuite(new StructTestSuite());
		addSuite(new PrimitiveCollectionTestSuite());
		addSuite(new QueueDequeTestSuite());
		addSuite(new FastBitSetTestSuite());
	}
	private void addSuite(TestSuite suite) {
		final Iterator tests = suite.tests().iterator();
		while(tests.hasNext()) {
			addTest((TestCase) tests.next());
		}
	}
}