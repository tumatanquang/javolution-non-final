/*
 * File: LayeredSync.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 1Aug1998     dl             Create public version
 */
package _templates.javolution.util.concurrent.locks;
/**
 * A class that can be used to compose Syncs.
 * A LayeredSync object manages two other Sync objects,
 * <em>outer</em> and <em>inner</em>. The acquire operation
 * invokes <em>outer</em>.acquire() followed by <em>inner</em>.acquire(),
 * but backing out of outer (via release) upon an exception in inner.
 * The other methods work similarly.
 * <p>
 * LayeredSyncs can be used to compose arbitrary chains
 * by arranging that either of the managed Syncs be another
 * LayeredSync.
 *
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class LayeredSync implements Sync {
	private final Sync _outer;
	private final Sync _inner;
	/**
	 * Create a LayeredSync managing the given outer and inner Sync
	 * objects
	 */
	public LayeredSync(Sync outer, Sync inner) {
		_outer = outer;
		_inner = inner;
	}
	public void acquire() throws InterruptedException {
		_outer.acquire();
		try {
			_inner.acquire();
		}
		catch(final InterruptedException ex) {
			_outer.release();
			throw ex;
		}
	}
	public boolean attempt(long msecs) throws InterruptedException {
		final long start = msecs <= 0 ? 0 : System.currentTimeMillis();
		long waitTime = msecs;
		if(_outer.attempt(waitTime)) {
			try {
				if(msecs > 0) {
					waitTime = msecs - (System.currentTimeMillis() - start);
				}
				if(_inner.attempt(waitTime))
					return true;
				_outer.release();
				return false;
			}
			catch(final InterruptedException ex) {
				_outer.release();
				throw ex;
			}
		}
		return false;
	}
	public void release() {
		_inner.release();
		_outer.release();
	}
}