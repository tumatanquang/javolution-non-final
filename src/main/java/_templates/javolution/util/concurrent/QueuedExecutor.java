/*
 * File: QueuedExecutor.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 21Jun1998    dl             Create public version
 * 28aug1998    dl             rely on ThreadFactoryUser, restart now public
 * 4may1999     dl             removed redundant interrupt detect
 * 7sep2000     dl             new shutdown methods
 * 20may2004    dl             can shutdown even if thread not created yet
 */
package _templates.javolution.util.concurrent;
/**
 *
 * An implementation of Executor that queues incoming
 * requests until they can be processed by a single background
 * thread.
 * <p>
 * The thread is not actually started until the first
 * <code>execute</code> request is encountered. Also, if the
 * thread is stopped for any reason (for example, after hitting
 * an unrecoverable exception in an executing task), one is started
 * upon encountering a new request, or if <code>restart()</code> is
 * invoked.
 * <p>
 * Beware that, especially in situations
 * where command objects themselves invoke execute, queuing can
 * sometimes lead to lockups, since commands that might allow
 * other threads to terminate do not run at all when they are in the queue.
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class QueuedExecutor extends ThreadFactoryUser implements Executor {
	/**
	 * The thread used to process commands
	 */
	private Thread _thread;
	/**
	 * Special queue element to signal termination
	 */
	private static Runnable ENDTASK = new Runnable() {
		public final void run() {}
	};
	/** true if thread should shut down after processing current task */
	private volatile boolean _shutdown; // latches true;
	/**
	 * Return the thread being used to process commands, or
	 * null if there is no such thread. You can use this
	 * to invoke any special methods on the thread, for
	 * example, to interrupt it.
	 */
	public synchronized Thread getThread() {
		return _thread;
	}
	/**
	 * set _thread to null to indicate termination
	 */
	private synchronized void clearThread() {
		_thread = null;
	}
	/**
	 * The queue
	 */
	private final Channel _queue;
	/**
	 * The runloop is isolated in its own Runnable class
	 * just so that the main
	 * class need not implement Runnable,  which would
	 * allow others to directly invoke run, which would
	 * never make sense here.
	 */
	private final class RunLoop implements Runnable {
		public void run() {
			try {
				while(!_shutdown) {
					Runnable task = (Runnable) _queue.take();
					if(task == ENDTASK) {
						_shutdown = true;
						break;
					}
					else if(task != null) {
						task.run();
						task = null;
					}
					else {
						break;
					}
				}
			}
			catch(final InterruptedException ex) {} // fallthrough
			finally {
				clearThread();
			}
		}
	}
	private final RunLoop _runLoop;
	/**
	 * Construct a new QueuedExecutor that uses
	 * the supplied Channel as its queue.
	 * <p>
	 * This class does not support any methods that
	 * reveal this queue. If you need to access it
	 * independently (for example to invoke any
	 * special status monitoring operations), you
	 * should record a reference to it separately.
	 */
	public QueuedExecutor(Channel queue) {
		_queue = queue;
		_runLoop = new RunLoop();
	}
	/**
	 * Construct a new QueuedExecutor that uses
	 * a BoundedLinkedQueue with the current
	 * DefaultChannelCapacity as its queue.
	 */
	public QueuedExecutor() {
		this(new BoundedLinkedQueue());
	}
	/**
	 * Start (or restart) the background thread to process commands. It has
	 * no effect if a thread is already running. This
	 * method can be invoked if the background thread crashed
	 * due to an unrecoverable exception.
	 */
	public synchronized void restart() {
		if(_thread == null && !_shutdown) {
			_thread = _threadFactory.newThread(_runLoop);
			_thread.start();
		}
	}
	/**
	 * Arrange for execution of the command in the
	 * background thread by adding it to the queue.
	 * The method may block if the channel's put
	 * operation blocks.
	 * <p>
	 * If the background thread
	 * does not exist, it is created and started.
	 */
	public void execute(Runnable command) throws InterruptedException {
		restart();
		_queue.put(command);
	}
	/**
	 * Terminate background thread after it processes all
	 * elements currently in queue. Any tasks entered after this point will
	 * not be processed. A shut down thread cannot be restarted.
	 * This method may block if the task queue is finite and full.
	 * Also, this method
	 * does not in general apply (and may lead to comparator-based
	 * exceptions) if the task queue is a priority queue.
	 */
	public synchronized void shutdownAfterProcessingCurrentlyQueuedTasks() {
		if(!_shutdown) {
			try {
				_queue.put(ENDTASK);
			}
			catch(final InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}
	/**
	 * Terminate background thread after it processes the
	 * current task, removing other queued tasks and leaving them unprocessed.
	 * A shut down thread cannot be restarted.
	 */
	public synchronized void shutdownAfterProcessingCurrentTask() {
		_shutdown = true;
		try {
			while(_queue.poll(0) != null) {
				// drain
			}
			_queue.put(ENDTASK);
		}
		catch(final InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
	/**
	 * Terminate background thread even if it is currently processing
	 * a task. This method uses Thread.interrupt, so relies on tasks
	 * themselves responding appropriately to interruption. If the
	 * current tasks does not terminate on interruption, then the
	 * thread will not terminate until processing current task.
	 * A shut down thread cannot be restarted.
	 */
	public synchronized void shutdownNow() {
		_shutdown = true;
		final Thread t = _thread;
		if(t != null) {
			t.interrupt();
		}
		shutdownAfterProcessingCurrentTask();
	}
}