/*
 * File: WriterPreferenceReadWriteLock.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 11Jun1998    dl             Create public version
 *  5Aug1998    dl             replaced int counters with longs
 * 25aug1998    dl             record writer thread
 *  3May1999    dl             add notifications on interrupt/timeout
 */
package _templates.javolution.util.concurrent.locks;
/**
 * A ReadWriteLock that prefers waiting writers over
 * waiting readers when there is contention. This class
 * is adapted from the versions described in CPJ, improving
 * on the ones there a bit by segregating reader and writer
 * wait queues, which is typically more efficient.
 * <p>
 * The locks are <em>NOT</em> reentrant. In particular,
 * even though it may appear to usually work OK,
 * a thread holding a read lock should not attempt to
 * re-acquire it. Doing so risks lockouts when there are
 * also waiting writers.
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class WriterPreferenceReadWriteLock implements ReadWriteLock {
	final ReaderLock _readerLock = new ReaderLock();
	final WriterLock _writerLock = new WriterLock();
	long _activeReaders;
	Thread _activeWriter;
	long _waitingReaders;
	long _waitingWriters;
	public Sync writeLock() {
		return _writerLock;
	}
	public Sync readLock() {
		return _readerLock;
	}
	/*
	 * A bunch of small synchronized methods are needed
	 * to allow communication from the Lock objects
	 * back to this object, that serves as controller
	 */
	synchronized void cancelledWaitingReader() {
		--_waitingReaders;
	}
	synchronized void cancelledWaitingWriter() {
		--_waitingWriters;
	}
	/**
	 * Override this method to change to reader preference
	 */
	boolean allowReader() {
		return _activeWriter == null && _waitingWriters == 0;
	}
	synchronized boolean startRead() {
		final boolean allowRead = allowReader();
		if(allowRead) {
			++_activeReaders;
		}
		return allowRead;
	}
	synchronized boolean startWrite() {
		// The allowWrite expression cannot be modified without
		// also changing startWrite, so is hard-wired
		final boolean allowWrite = _activeWriter == null && _activeReaders == 0;
		if(allowWrite) {
			_activeWriter = Thread.currentThread();
		}
		return allowWrite;
	}
	/*
	 * Each of these variants is needed to maintain atomicity
	 * of wait counts during wait loops. They could be
	 * made faster by manually inlining each other. We hope that
	 * compilers do this for us though.
	 */
	synchronized boolean startReadFromNewReader() {
		final boolean pass = startRead();
		if(!pass) {
			++_waitingReaders;
		}
		return pass;
	}
	synchronized boolean startWriteFromNewWriter() {
		final boolean pass = startWrite();
		if(!pass) {
			++_waitingWriters;
		}
		return pass;
	}
	synchronized boolean startReadFromWaitingReader() {
		final boolean pass = startRead();
		if(pass) {
			--_waitingReaders;
		}
		return pass;
	}
	synchronized boolean startWriteFromWaitingWriter() {
		final boolean pass = startWrite();
		if(pass) {
			--_waitingWriters;
		}
		return pass;
	}
	/**
	 * Called upon termination of a read.
	 * Returns the object to signal to wake up a waiter, or null if no such
	 */
	synchronized Signaller endRead() {
		if(--_activeReaders == 0 && _waitingWriters > 0)
			return _writerLock;
		return null;
	}
	/**
	 * Called upon termination of a write.
	 * Returns the object to signal to wake up a waiter, or null if no such
	 */
	synchronized Signaller endWrite() {
		_activeWriter = null;
		if(_waitingReaders > 0 && allowReader())
			return _readerLock;
		else if(_waitingWriters > 0)
			return _writerLock;
		else
			return null;
	}
	/**
	 * Reader and Writer requests are maintained in two different
	 * wait sets, by two different objects. These objects do not
	 * know whether the wait sets need notification since they
	 * don't know preference rules. So, each supports a
	 * method that can be selected by main controlling object
	 * to perform the notifications.  This base class simplifies mechanics.
	 */
	abstract class Signaller { // base for ReaderLock and WriterLock
		abstract void signalWaiters();
	}
	public class ReaderLock extends Signaller implements Sync {
		public void acquire() throws InterruptedException {
			/*@JVM-1.1+@
			if(Thread.interrupted())
				throw new InterruptedException();
			/**/
			InterruptedException ie = null;
			synchronized(this) {
				if(!startReadFromNewReader()) {
					for(;;) {
						try {
							ReaderLock.this.wait();
							if(startReadFromWaitingReader())
								return;
						}
						catch(final InterruptedException ex) {
							cancelledWaitingReader();
							ie = ex;
							break;
						}
					}
				}
			}
			if(ie != null) {
				// fall through outside synch on interrupt.
				// This notification is not really needed here,
				// but may be in plausible subclasses
				_writerLock.signalWaiters();
				throw ie;
			}
		}
		public void release() {
			final Signaller s = endRead();
			if(s != null) {
				s.signalWaiters();
			}
		}
		synchronized void signalWaiters() {
			ReaderLock.this.notifyAll();
		}
		public boolean attempt(long msecs) throws InterruptedException {
			/*@JVM-1.1+@
			if(Thread.interrupted())
				throw new InterruptedException();
			/**/
			InterruptedException ie = null;
			synchronized(this) {
				if(msecs <= 0)
					return startRead();
				else if(startReadFromNewReader())
					return true;
				else {
					long waitTime = msecs;
					final long start = System.currentTimeMillis();
					for(;;) {
						try {
							ReaderLock.this.wait(waitTime);
						}
						catch(final InterruptedException ex) {
							cancelledWaitingReader();
							ie = ex;
							break;
						}
						if(startReadFromWaitingReader())
							return true;
						waitTime = msecs - (System.currentTimeMillis() - start);
						if(waitTime <= 0) {
							cancelledWaitingReader();
							break;
						}
					}
				}
			}
			// safeguard on interrupt or timeout:
			_writerLock.signalWaiters();
			if(ie != null)
				throw ie;
			return false; // timed out
		}
	}
	public class WriterLock extends Signaller implements Sync {
		public void acquire() throws InterruptedException {
			/*@JVM-1.1+@
			if(Thread.interrupted())
				throw new InterruptedException();
			/**/
			InterruptedException ie = null;
			synchronized(this) {
				if(!startWriteFromNewWriter()) {
					for(;;) {
						try {
							WriterLock.this.wait();
							if(startWriteFromWaitingWriter())
								return;
						}
						catch(final InterruptedException ex) {
							cancelledWaitingWriter();
							WriterLock.this.notify();
							ie = ex;
							break;
						}
					}
				}
			}
			if(ie != null) {
				// Fall through outside synch on interrupt.
				// On exception, we may need to signal readers.
				// It is not worth checking here whether it is strictly necessary.
				_readerLock.signalWaiters();
				throw ie;
			}
		}
		public void release() {
			final Signaller s = endWrite();
			if(s != null) {
				s.signalWaiters();
			}
		}
		synchronized void signalWaiters() {
			WriterLock.this.notify();
		}
		public boolean attempt(long msecs) throws InterruptedException {
			/*@JVM-1.1+@
			if(Thread.interrupted())
				throw new InterruptedException();
			/**/
			InterruptedException ie = null;
			synchronized(this) {
				if(msecs <= 0)
					return startWrite();
				else if(startWriteFromNewWriter())
					return true;
				else {
					long waitTime = msecs;
					final long start = System.currentTimeMillis();
					for(;;) {
						try {
							WriterLock.this.wait(waitTime);
						}
						catch(final InterruptedException ex) {
							cancelledWaitingWriter();
							WriterLock.this.notify();
							ie = ex;
							break;
						}
						if(startWriteFromWaitingWriter())
							return true;
						waitTime = msecs - (System.currentTimeMillis() - start);
						if(waitTime <= 0) {
							cancelledWaitingWriter();
							WriterLock.this.notify();
							break;
						}
					}
				}
			}
			_readerLock.signalWaiters();
			if(ie != null)
				throw ie;
			return false; // timed out
		}
	}
}