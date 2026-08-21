/*
 * File: CyclicBarrier.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 11Jul1998    dl             Create public version
 * 28Aug1998    dl             minor code simplification
 */
package _templates.javolution.util.concurrent;
/**
 * A cyclic barrier is a reasonable choice for a barrier in contexts
 * involving a fixed sized group of threads that
 * must occasionally wait for each other.
 * (A Rendezvous better handles applications in which
 * any number of threads meet, n-at-a-time.)
 * <p>
 * CyclicBarriers use an all-or-none breakage model
 * for failed synchronization attempts: If threads
 * leave a barrier point prematurely because of timeout
 * or interruption, others will also leave abnormally
 * (via BrokenBarrierException), until
 * the barrier is <code>restart</code>ed. This is usually
 * the simplest and best strategy for sharing knowledge
 * about failures among cooperating threads in the most
 * common usages contexts of Barriers.
 * This implementation  has the property that interruptions
 * among newly arriving threads can cause as-yet-unresumed
 * threads from a previous barrier cycle to return out
 * as broken. This transmits breakage
 * as early as possible, but with the possible byproduct that
 * only some threads returning out of a barrier will realize
 * that it is newly broken. (Others will not realize this until a
 * future cycle.) (The Rendezvous class has a more uniform, but
 * sometimes less desirable policy.)
 * <p>
 * Barriers support an optional Runnable command
 * that is run once per barrier point.
 * <p>
 * <b>Sample usage</b> Here is a code sketch of
 *  a  barrier in a parallel decomposition design.
 * <pre>
 * class Solver {
 *   final int N;
 *   final float[][] data;
 *   final CyclicBarrier barrier;
 *
 *   class Worker implements Runnable {
 *      int myRow;
 *      Worker(int row) { myRow = row; }
 *      public void run() {
 *         while(!done()) {
 *            processRow(myRow);
 *
 *            try {
 *              barrier.barrier();
 *            }
 *            catch(InterruptedException ex) { return; }
 *            catch(BrokenBarrierException ex) { return; }
 *         }
 *      }
 *   }
 *
 *   public Solver(float[][] matrix) {
 *     data = matrix;
 *     N = matrix.length;
 *     barrier = new CyclicBarrier(N);
 *     barrier.setBarrierCommand(new Runnable() {
 *       public final void run() { mergeRows(...); }
 *     });
 *     for(int i = 0; i < N; ++i) {
 *       new Thread(new Worker(i)).start();
 *       waitUntilDone();
 *    }
 * }
 * </pre>
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>

 */
public class CyclicBarrier implements Barrier {
	private final int _parties;
	private boolean _broken = false;
	private Runnable _barrierCommand = null;
	private int _count; // number of parties still waiting
	private int _resets = 0; // incremented on each release
	/**
	 * Create a CyclicBarrier for the indicated number of parties,
	 * and no command to run at each barrier.
	 * @throws IllegalArgumentException if parties less than or equal to zero.
	 */
	public CyclicBarrier(int parties) {
		this(parties, null);
	}
	/**
	 * Create a CyclicBarrier for the indicated number of parties.
	 * and the given command to run at each barrier point.
	 * @throws IllegalArgumentException if parties less than or equal to zero.
	 */
	public CyclicBarrier(int parties, Runnable command) {
		if(parties <= 0)
			throw new IllegalArgumentException();
		_parties = parties;
		_count = parties;
		_barrierCommand = command;
	}
	/**
	 * Set the command to run at the point at which all threads reach the
	 * barrier. This command is run exactly once, by the thread
	 * that trips the barrier. The command is not run if the barrier is
	 * broken.
	 * @param command the command to run. If null, no command is run.
	 * @return the previous command
	 */
	public synchronized Runnable setBarrierCommand(Runnable command) {
		final Runnable old = _barrierCommand;
		_barrierCommand = command;
		return old;
	}
	public synchronized boolean broken() {
		return _broken;
	}
	/**
	 * Reset to initial state. Clears both the broken status
	 * and any record of waiting threads, and releases all
	 * currently waiting threads with indeterminate return status.
	 * This method is intended only for use in recovery actions
	 * in which it is somehow known
	 * that no thread could possibly be relying on the
	 * the synchronization properties of this barrier.
	 */
	public synchronized void restart() {
		_broken = false;
		++_resets;
		_count = _parties;
		notifyAll();
	}
	public int parties() {
		return _parties;
	}
	/**
	 * Enter barrier and wait for the other parties()-1 threads.
	 * @return the arrival index: the number of other parties
	 * that were still waiting
	 * upon entry. This is a unique value from zero to parties()-1.
	 * If it is zero, then the current
	 * thread was the last party to hit barrier point
	 * and so was responsible for releasing the others.
	 * @throws BrokenBarrierException if any other thread
	 * in any previous or current barrier
	 * since either creation or the last <code>restart</code>
	 * operation left the barrier
	 * prematurely due to interruption or time-out. (If so,
	 * the <code>broken</code> status is also set.)
	 * Threads that are notified to have been
	 * interrupted <em>after</em> being released are not considered
	 * to have broken the barrier.
	 * In all cases, the interruption
	 * status of the current thread is preserved, so can be tested
	 * by checking <code>Thread.interrupted</code>.
	 * @throws InterruptedException if this thread was interrupted
	 * during the barrier, and was the one causing breakage.
	 * If so, <code>broken</code> status is also set.
	 */
	public int barrier() throws InterruptedException, BrokenBarrierException {
		return doBarrier(false, 0);
	}
	/**
	 * Enter barrier and wait at most msecs for the other parties()-1 threads.
	 * @return if not timed out, the arrival index: the number of other parties
	 * that were still waiting
	 * upon entry. This is a unique value from zero to parties()-1.
	 * If it is zero, then the current
	 * thread was the last party to hit barrier point
	 * and so was responsible for releasing the others.
	 * @throws BrokenBarrierException
	 * if any other thread
	 * in any previous or current barrier
	 * since either creation or the last <code>restart</code>
	 * operation left the barrier
	 * prematurely due to interruption or time-out. (If so,
	 * the <code>broken</code> status is also set.)
	 * Threads that are noticed to have been
	 * interrupted <em>after</em> being released are not considered
	 * to have broken the barrier.
	 * In all cases, the interruption
	 * status of the current thread is preserved, so can be tested
	 * by checking <code>Thread.interrupted</code>.
	 * @throws InterruptedException if this thread was interrupted
	 * during the barrier. If so, <code>broken</code> status is also set.
	 * @throws TimeoutException if this thread timed out waiting for
	 *  the barrier. If the timeout occured while already in the
	 * barrier, <code>broken</code> status is also set.
	 */
	public int attemptBarrier(long msecs) throws InterruptedException,
			TimeoutException, BrokenBarrierException {
		return doBarrier(true, msecs);
	}
	private synchronized int doBarrier(boolean timed, long msecs)
			throws InterruptedException, TimeoutException,
			BrokenBarrierException {
		final int index = --_count;
		if(_broken)
			throw new BrokenBarrierException(index);
		/*@JVM-1.1+@
		else if(Thread.interrupted()) {
			_broken = true;
			notifyAll();
			throw new InterruptedException();
		}
		/**/
		else if(index == 0) { // tripped
			_count = _parties;
			++_resets;
			notifyAll();
			try {
				if(_barrierCommand != null) {
					_barrierCommand.run();
				}
				return 0;
			}
			catch(final RuntimeException ex) {
				_broken = true;
				return 0;
			}
		}
		else if(timed && msecs <= 0) {
			_broken = true;
			notifyAll();
			throw new TimeoutException(msecs);
		}
		else { // wait until next reset
			final int r = _resets;
			final long startTime = timed ? System.currentTimeMillis() : 0;
			long waitTime = msecs;
			for(;;) {
				try {
					wait(waitTime);
				}
				catch(final InterruptedException ex) {
					// Only claim that broken if interrupted before reset
					if(_resets == r) {
						_broken = true;
						notifyAll();
						throw ex;
					}
					Thread.currentThread().interrupt(); // propagate
				}
				if(_broken)
					throw new BrokenBarrierException(index);
				else if(r != _resets)
					return index;
				else if(timed) {
					waitTime = msecs - (System.currentTimeMillis() - startTime);
					if(waitTime <= 0) {
						_broken = true;
						notifyAll();
						throw new TimeoutException(msecs);
					}
				}
			}
		}
	}
}