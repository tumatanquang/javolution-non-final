/*
 * File: ObservableSync.java
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
import _templates.java.util.Iterator;
import _templates.javolution.util.concurrent.CopyOnWriteArraySet;
/**
 * The ObservableSync class performs no synchronization
 * itself, but invokes event-style messages to other
 * observer objects upon invocation of Sync methods.
 * These observers typically perform monitoring, logging,
 * or other bookkeeping operations surrounding the object
 * being managed by this Sync object.
 * <p>
 * Because ObservableSync does not itself perform any synchronization
 * control, the attempt operation always succeeds.
 * This class is typically used (via LayeredSync) as a wrapper
 * around those that do perform synchronization control.
 * <p>
 * This class is based around a standard Observer design pattern.
 * It is not hard to convert this to instead use a Listener
 * design (as seen in AWT and JavaBeans), by defining associated
 * EventObjects and forwarding them.
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 * @see LayeredSync
 */
public class ObservableSync implements Sync {
	/**
	 *  Interface for objects that observe ObservableSyncs.
	 */
	public interface SyncObserver {
		/**
		 * Method called upon acquire or successful attempt of Sync
		 */
		public void onAcquire(Object arg);
		/**
		 * Method called upon release of Sync.
		 */
		public void onRelease(Object arg);
	}
	private final CopyOnWriteArraySet _observers = new CopyOnWriteArraySet();
	private Object _arg;
	/**
	 * Create an ObservableSync that uses the supplied argument
	 * for all notifications. The argument is typically an
	 * object that is being managed by this Sync object.
	 */
	public ObservableSync(Object notificationArgument) {
		_arg = notificationArgument;
	}
	/**
	 * Return the argument used for notifications
	 */
	public synchronized Object getNotificationArgument() {
		return _arg;
	}
	/**
	 * Set the argument used for notifications.
	 * @return the previous value of this argument
	 */
	public synchronized Object setNotificationArgument(Object notificationArg) {
		final Object old = _arg;
		_arg = notificationArg;
		return old;
	}
	public void acquire() {
		final Object arg = getNotificationArgument();
		for(final Iterator it = _observers.iterator(); it.hasNext();) {
			((SyncObserver) it.next()).onAcquire(arg);
		}
	}
	public boolean attempt(long msecs) {
		acquire();
		return true;
	}
	public void release() {
		final Object arg = getNotificationArgument();
		for(final Iterator it = _observers.iterator(); it.hasNext();) {
			((SyncObserver) it.next()).onRelease(arg);
		}
	}
	/**
	 * Add obs to the set of observers
	 */
	public void attach(SyncObserver obs) {
		_observers.add(obs);
	}
	/**
	 * Remove obs from the set of observers. No effect if not in set
	 */
	public void detach(SyncObserver obs) {
		_observers.remove(obs);
	}
	/** Return an iterator that can be used to traverse through
	 * current set of observers
	 */
	public Iterator observers() {
		return _observers.iterator();
	}
}