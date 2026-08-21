/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2026 - Javolution
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package javolution;
import _templates.javolution.testing.TestContext;
/**
 * Standalone regression runner used only by the explicit test-all target.
 */
public final class AllTestRunner {
	private AllTestRunner() {}
	public static void main(String[] args) throws Exception {
		TestContext.enter(TestContext.REGRESSION);
		try {
			TestContext.run(new AllTestSuite());
		}
		finally {
			TestContext.exit();
		}
	}
}