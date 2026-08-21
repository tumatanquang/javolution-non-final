/*
 * File: ProperyChangeMulticaster.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * This class is based on Sun JDK java.beans.VetoableChangeSupport,
 * which is copyrighted by Sun. (It shares practically no code, but for
 * consistency, the documentation was lifted and adapted here.)
 *
 * History:
 * Date         Who            What
 * 14Mar1999    dl             first release
 */
package _templates.javolution.util.concurrent;
/*@JVM-1.1+@
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.IOException;
import _templates.java.util.HashMap;
import _templates.java.io.ObjectInputStream;
import _templates.java.io.ObjectOutputStream;
/**/
import _templates.java.io.Serializable;
/**
 * This class is interoperable with java.beans.VetoableChangeSupport,
 * but relies on a streamlined copy-on-write scheme similar to
 * that used in CopyOnWriteArrayList. It also adheres to clarified
 * semantics of add, remove, and fireVetoableChange operations.
 * <p>
 * <b>Sample usage.</b>
 *
 * <pre>
 * class Thing {
 *   protected Color myColor = Color.red; // an example property
 *   protected boolean changePending; // track whether in midst of change
 *
 *   // vetoable listeners:
 *   protected VetoableChangeMulticaster vetoers =
 *     new VetoableChangeMulticaster(this);
 *
 *   // Possibly also some ordinary listeners:
 *   protected PropertyChangeMulticaster listeners =
 *     new PropertyChangeMulticaster(this);
 *
 *   // registration methods, including:
 *   void addVetoer(VetoableChangeListener l) {
 *     // Use the `ifAbsent' version to avoid duplicate notifications
 *     vetoers.addVetoableChangeListenerIfAbsent(l);
 *   }
 *
 *   public synchronized Color getColor() { // accessor
 *     return myColor;
 *   }
 *
 *   // Simple transactional control for vetos
 *
 *   public void setColor(int newColor) throws PropertyVetoException {
 *     Color oldColor = prepareSetColor(newColor);
 *
 *     try {
 *       vetoers.fireVetoableChange("color", oldColor, newColor);
 *       commitColor(newColor);
 *       listeners.firePropertyChange("color", oldColor, newColor);
 *     }
 *     catch(PropertyVetoException ex) {
 *       abortSetColor();
 *       throw ex;
 *     }
 *   }
 *
 *   // Called on entry to proposed vetoable change from setColor.
 *   // Throws exception if there is already another change in progress.
 *   // Returns current color
 *   synchronized int prepareSetColor(Color c) throws PropertyVetoException {
 *     // only support one transaction at a time
 *     if(changePending)
 *       throw new PropertyVetoException("Concurrent modification");
 *       // (Could alternatively wait out other transactions via
 *       // a wait/notify construction based on changePending.)
 *
 *     // perhaps some other screenings, like:
 *     else if(c == null)
 *       throw new PropertyVetoException("Cannot change color to Null");
 *     else {
 *       changePending = true;
 *       return myColor;
 *     }
 *   }
 *
 *   synchronized void commitColor(Color newColor) {
 *     myColor = newColor;
 *     changePending = false;
 *   }
 *
 *   synchronized void abortSetColor() {
 *     changePending = false;
 *   }
 *
 * }
 * </pre>
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class VetoableChangeMulticaster implements Serializable {
	// This code is 90% identical with PropertyChangeMulticaster,
	// but there is no good way to unify the code while maintaining
	// interoperability with beans versions.
	/**
	 * The array of listeners. Copied on each update
	 */
	/*@JVM-1.1+@
	private transient VetoableChangeListener[] listeners = new VetoableChangeListener[0];
	/**/
	/**
	 * The object to be provided as the "source" for any generated events.
	 * @serial
	 */
	/*@JVM-1.1+@
	private final Object source;
	/**/
	/**
	 * HashMap for managing listeners for specific properties.
	 * Maps property names to VetoableChangeMulticaster objects.
	 * @serial
	 */
	/*@JVM-1.1+@
	private HashMap children;
	/**/
	/**
	 * Constructs a <code>VetoableChangeMulticaster</code> object.
	 *
	 * @param sourceBean  The bean to be given as the source for any events.
	 * @throws NullPointerException if sourceBean is null
	 */
	/*@JVM-1.1+@
	public VetoableChangeMulticaster(Object sourceBean) {
		if(sourceBean == null)
			throw new NullPointerException();
		source = sourceBean;
	}
	/**/
	/**
	 * Return the child associated with property, or null if no such
	 */
	/*@JVM-1.1+@
	private synchronized VetoableChangeMulticaster getChild(String propertyName) {
		return children == null ? null : (VetoableChangeMulticaster) children.get(propertyName);
	}
	/**/
	/**
	 * Add a VetoableChangeListener to the listener list.
	 * The listener is registered for all properties.
	 * If the listener is added multiple times, it will
	 * receive multiple change notifications upon any fireVetoableChange.
	 *
	 * @param listener  The VetoableChangeListener to be added
	 */
	/*@JVM-1.1+@
	public synchronized void addVetoableChangeListener(VetoableChangeListener listener) {
		if(listener == null)
			throw new NullPointerException();
		final int length = listeners.length;
		final VetoableChangeListener[] newArray = new VetoableChangeListener[length + 1];
		if(length != 0) {
			System.arraycopy(listeners, 0, newArray, 0, length);
		}
		newArray[length] = listener;
		listeners = newArray;
	}
	/**/
	/**
	 * Add a PropertyChangeListener to the listener list if it is
	 * not already present.
	 * The listener is registered for all properties.
	 * The operation maintains Set semantics: If the listener is already
	 * registered, the operation has no effect.
	 *
	 * @param listener  The PropertyChangeListener to be added
	 * @throws NullPointerException If listener is null
	 */
	/*@JVM-1.1+@
	public synchronized void addVetoableChangeListenerIfAbsent(VetoableChangeListener listener) {
		if(listener == null)
			throw new NullPointerException();
		// Copy while checking if already present.
		final int length = listeners.length;
		final VetoableChangeListener[] newArray = new VetoableChangeListener[length + 1];
		for(int i = 0; i < length; ++i) {
			newArray[i] = listeners[i];
			if(listener.equals(listeners[i]))
				return; // already present -- throw away copy
		}
		newArray[length] = listener;
		listeners = newArray;
	}
	/**/
	/**
	 * Remove an occurrence of a VetoableChangeListener from the listener list.
	 * It removes at most one occurrence of the given listener.
	 * If the listener was added multiple times it must be removed
	 * mulitple times.
	 * This removes a VetoableChangeListener that was registered
	 * for all properties, and has no effect if registered for only
	 * one or more specified properties.
	 *
	 * @param listener  The VetoableChangeListener to be removed
	 */
	/*@JVM-1.1+@
	public synchronized void removeVetoableChangeListener(VetoableChangeListener listener) {
		final int newlen = listeners.length - 1;
		if(newlen < 0 || listener == null)
			return;
		// Copy while searching for element to remove
		final VetoableChangeListener[] newArray = new VetoableChangeListener[newlen];
		for(int i = 0; i < newlen; ++i) {
			if(listener.equals(listeners[i])) {
				//  copy remaining and exit
				for(int k = i + 1; k <= newlen; ++k) {
					newArray[k - 1] = listeners[k];
				}
				listeners = newArray;
				return;
			}
			else {
				newArray[i] = listeners[i];
			}
		}
		// special-case last cell
		if(listener.equals(listeners[newlen])) {
			listeners = newArray;
		}
	}
	/**/
	/**
	 * Add a VetoableChangeListener for a specific property.  The listener
	 * will be invoked only when a call on fireVetoableChange names that
	 * specific property. However, if a listener is registered both for all
	 * properties and a specific property, it will receive multiple
	 * notifications upon changes to that property.
	 *
	 * @param propertyName  The name of the property to listen on.
	 * @param listener  The VetoableChangeListener to be added
	 * @throws NullPointerException If listener is null
	 */
	/*@JVM-1.1+@
	public void addVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
		if(listener == null)
			throw new NullPointerException();
		VetoableChangeMulticaster child = null;
		synchronized(this) {
			if(children == null) {
				children = new HashMap();
			}
			else {
				child = (VetoableChangeMulticaster) children.get(propertyName);
			}
			if(child == null) {
				child = new VetoableChangeMulticaster(source);
				children.put(propertyName, child);
			}
		}
		child.addVetoableChangeListener(listener);
	}
	/**/
	/**
	 * Add a VetoableChangeListener for a specific property, if it is not
	 * already registered.  The listener
	 * will be invoked only when a call on fireVetoableChange names that
	 * specific property.
	 *
	 * @param propertyName  The name of the property to listen on.
	 * @param listener  The VetoableChangeListener to be added
	 * @throws NullPointerException If listener is null
	 */
	/*@JVM-1.1+@
	public void addVetoableChangeListenerIfAbsent(String propertyName, VetoableChangeListener listener) {
		if(listener == null)
			throw new NullPointerException();
		VetoableChangeMulticaster child = null;
		synchronized(this) {
			if(children == null) {
				children = new HashMap();
			}
			else {
				child = (VetoableChangeMulticaster) children.get(propertyName);
			}
			if(child == null) {
				child = new VetoableChangeMulticaster(source);
				children.put(propertyName, child);
			}
		}
		child.addVetoableChangeListenerIfAbsent(listener);
	}
	/**/
	/**
	 * Remove a VetoableChangeListener for a specific property.
	 * Affects only the given property.
	 * If the listener is also registered for all properties,
	 * then it will continue to be registered for them.
	 *
	 * @param propertyName  The name of the property that was listened on.
	 * @param listener  The VetoableChangeListener to be removed
	 */
	/*@JVM-1.1+@
	public void removeVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
		final VetoableChangeMulticaster child = getChild(propertyName);
		if(child != null) {
			child.removeVetoableChangeListener(listener);
		}
	}
	/**/
	/**
	 * Helper method to relay evt to all listeners.
	 * Called by all public fireVetoableChange methods.
	 */
	/*@JVM-1.1+@
	private void multicast(PropertyChangeEvent evt) throws PropertyVetoException {
		VetoableChangeListener[] array; // bind in synch block below
		VetoableChangeMulticaster child = null;
		synchronized(this) {
			array = listeners;
			if(children != null && evt.getPropertyName() != null) {
				child = (VetoableChangeMulticaster) children.get(evt.getPropertyName());
			}
		}
		// Loop through array, and then cascade to child.
		int i = 0; // make visible to catch clause
		try {
			final int length = array.length;
			for(i = 0; i < length; ++i) {
				array[i].vetoableChange(evt);
			}
			if(child != null) {
				child.multicast(evt);
			}
		}
		catch(final PropertyVetoException veto) {
			// Revert all that have been notified
			final PropertyChangeEvent revert = new PropertyChangeEvent(evt.getSource(), evt.getPropertyName(),
					evt.getNewValue(), evt.getOldValue());
			final int lastNotified = i < array.length ? i : array.length - 1;
			for(int k = 0; k <= lastNotified; ++k) {
				try {
					array[k].vetoableChange(revert);
				}
				catch(final PropertyVetoException ignore) {
					// Cannot veto a reversion
				}
			}
			//  Rethrow the PropertyVetoException.
			throw veto;
		}
	}
	/**/
	/**
	 * Report a vetoable property update to any registered listeners.
	 * Notifications are sent serially (although in no particular order)
	 * to the list of listeners,
	 * aborting if one throws PropertyVetoException. Upon this exception,
	 * fire a new event reverting this
	 * change to all listeners that have already been notified
	 * (ignoring any further vetos),
	 * suppress notifications to all other listeners, and
	 * then rethrow the PropertyVetoException.
	 * <p>
	 * No event is fired if old and new are equal non-null.
	 *
	 * @param propertyName  The programmatic name of the property
	 *		that was changed.
	 * @param oldValue  The old value of the property.
	 * @param newValue  The new value of the property.
	 * @throws PropertyVetoException if a recipient wishes the property
	 *              change to be rolled back.
	 */
	/*@JVM-1.1+@
	public void fireVetoableChange(String propertyName, Object oldValue, Object newValue) throws PropertyVetoException {
		if(oldValue == null || newValue == null || !oldValue.equals(newValue)) {
			multicast(new PropertyChangeEvent(source, propertyName, oldValue, newValue));
		}
	}
	/**/
	/**
	 * Report a vetoable property update to any registered listeners.
	 * Notifications are sent serially (although in no particular order)
	 * to the list of listeners,
	 * aborting if one throws PropertyVetoException. Upon this exception,
	 * fire a new event reverting this
	 * change to all listeners that have already been notified
	 * (ignoring any further vetos),
	 * suppress notifications to all other listeners, and
	 * then rethrow the PropertyVetoException.
	 * <p>
	 * No event is fired if old and new are equal.
	 * <p>
	 * This is merely a convenience wrapper around the more general
	 * fireVetoableChange method that takes Object values.
	 *
	 * @param propertyName  The programmatic name of the property
	 *		that was changed.
	 * @param oldValue  The old value of the property.
	 * @param newValue  The new value of the property.
	 * @throws PropertyVetoException if the recipient wishes the property
	 *              change to be rolled back.
	 */
	/*@JVM-1.1+@
	public void fireVetoableChange(String propertyName, int oldValue, int newValue) throws PropertyVetoException {
		if(oldValue != newValue) {
			multicast(new PropertyChangeEvent(source, propertyName, new Integer(oldValue), new Integer(newValue)));
		}
	}
	/**/
	/**
	 * Report a vetoable property update to any registered listeners.
	 * Notifications are sent serially (although in no particular order)
	 * to the list of listeners,
	 * aborting if one throws PropertyVetoException. Upon this exception,
	 * fire a new event reverting this
	 * change to all listeners that have already been notified
	 * (ignoring any further vetos),
	 * suppress notifications to all other listeners, and
	 * then rethrow the PropertyVetoException.
	 * <p>
	 * No event is fired if old and new are equal.
	 * <p>
	 * This is merely a convenience wrapper around the more general
	 * fireVetoableChange method that takes Object values.
	 *
	 * @param propertyName  The programmatic name of the property
	 *		that was changed.
	 * @param oldValue  The old value of the property.
	 * @param newValue  The new value of the property.
	 * @throws PropertyVetoException if the recipient wishes the property
	 *              change to be rolled back.
	 */
	/*@JVM-1.1+@
	public void fireVetoableChange(String propertyName, boolean oldValue, boolean newValue) throws PropertyVetoException {
		if(oldValue != newValue) {
			multicast(new PropertyChangeEvent(source, propertyName, new Boolean(oldValue), new Boolean(newValue)));
		}
	}
	/**/
	/**
	 * Report a vetoable property update to any registered listeners.
	 * Notifications are sent serially (although in no particular order)
	 * to the list of listeners,
	 * aborting if one throws PropertyVetoException. Upon this exception,
	 * fire a new event reverting this
	 * change to all listeners that have already been notified
	 * (ignoring any further vetos),
	 * suppress notifications to all other listeners, and
	 * then rethrow the PropertyVetoException.
	 * <p>
	 * No event is fired if old and new are equal and non-null.
	 *
	 * equal and non-null.
	 * @param evt  The PropertyChangeEvent object.
	 * @throws PropertyVetoException if the recipient wishes the property
	 *              change to be rolled back.
	 */
	/*@JVM-1.1+@
	public void fireVetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
		final Object oldValue = evt.getOldValue();
		final Object newValue = evt.getNewValue();
		if(oldValue == null || newValue == null || !oldValue.equals(newValue)) {
			multicast(evt);
		}
	}
	/**/
	/**
	 * Check if there are any listeners for a specific property.
	 * If propertyName is null, return whether there are any listeners at all.
	 *
	 * @param propertyName  the property name.
	 * @return true if there are one or more listeners for the given property
	 *
	 */
	/*@JVM-1.1+@
	public boolean hasListeners(String propertyName) {
		VetoableChangeMulticaster child;
		synchronized(this) {
			if(listeners.length != 0)
				return true;
			else if(propertyName == null || children == null)
				return false;
			else {
				child = (VetoableChangeMulticaster) children.get(propertyName);
				if(child == null)
					return false;
			}
		}
		return child.hasListeners(null);
	}
	/**/
	/**
	 * @serialData Null terminated list of <code>VetoableChangeListeners</code>.
	 * <p>
	 * At serialization time we skip non-serializable listeners and
	 * only serialize the serializable listeners.
	 *
	 */
	/*@JVM-1.1+@
	private synchronized void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
		final int length = listeners.length;
		for(int i = 0; i < length; ++i) {
			final VetoableChangeListener l = listeners[i];
			if(listeners[i] instanceof Serializable) {
				s.writeObject(listeners[i]);
			}
		}
		s.writeObject(null);
	}
	private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
		listeners = new VetoableChangeListener[0]; // paranoically reset
		s.defaultReadObject();
		for(Object listenerOrNull; (listenerOrNull = s.readObject()) != null;) {
			addVetoableChangeListener((VetoableChangeListener) listenerOrNull);
		}
	}
	/**/
}