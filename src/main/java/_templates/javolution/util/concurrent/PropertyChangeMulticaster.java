/*
 * File: PropertyChangeMulticaster.java
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
import java.beans.PropertyChangeListener;
import java.io.IOException;
import _templates.java.util.HashMap;
import _templates.java.io.ObjectInputStream;
import _templates.java.io.ObjectOutputStream;
/**/
import _templates.java.io.Serializable;
/**
 * This class is interoperable with java.beans.PropertyChangeSupport,
 * but relies on a streamlined copy-on-write scheme similar to
 * that used in CopyOnWriteArrayList. This leads to much better
 * performance in most event-intensive programs. It also adheres to clarified
 * semantics of add and remove  operations.
 * <p>
 * <b>Sample usage.</b>
 *
 * <pre>
 * class Thing {
 *   protected Color myColor = Color.red; // an example property
 *
 *   protected PropertyChangeMulticaster listeners =
 *     new PropertyChangeMulticaster(this);
 *
 *   // registration methods, including:
 *   void addListener(PropertyChangeListener l) {
 *     // Use the `ifAbsent' version to avoid duplicate notifications
 *     listeners.addPropertyChangeListenerIfAbsent(l);
 *   }
 *
 *  public synchronized Color getColor() { // accessor
 *    return myColor;
 *  }
 *
 *   // internal synchronized state change method; returns old value
 *   protected synchronized Color assignColor(Color newColor) {
 *     Color oldColor = myColor;
 *     myColor = newColor;
 *     return oldColor;
 *   }
 *
 *   public void setColor(Color newColor) {
 *     // atomically change state
 *     Color oldColor = assignColor(newColor);
 *     // broadcast change notification without holding synch lock
 *     listeners.firePropertyChange("color", oldColor, newColor);
 *   }
 * }
 * </pre>
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class PropertyChangeMulticaster implements Serializable {
	// In order to allow this class to be lifted out without using
	// the whole package, the basic mechanics of CopyOnWriteArrayList
	// are used here, but not the class itself.
	// This also makes it barely faster.
	/**
	 * The array of listeners. Copied on each update
	 */
	/*@JVM-1.1+@
	private transient PropertyChangeListener[] listeners = new PropertyChangeListener[0];
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
	 * Maps property names to PropertyChangeMulticaster objects.
	 * @serial
	 */
	/*@JVM-1.1+@
	private HashMap children;
	/**/
	/**
	 * Constructs a <code>PropertyChangeMulticaster</code> object.
	 *
	 * @param sourceBean  The bean to be given as the source for any events.
	 * @throws NullPointerException if sourceBean is null
	 */
	/*@JVM-1.1+@
	public PropertyChangeMulticaster(Object sourceBean) {
		if(sourceBean == null)
			throw new NullPointerException();
		source = sourceBean;
	}
	/**/
	/**
	 * Return the child associated with property, or null if no such
	 */
	/*@JVM-1.1+@
	private synchronized PropertyChangeMulticaster getChild(String propertyName) {
		return children == null ? null : (PropertyChangeMulticaster) children.get(propertyName);
	}
	/**/
	/**
	 * Add a VetoableChangeListener to the listener list.
	 * The listener is registered for all properties.
	 * If the listener is added multiple times, it will
	 * receive multiple change notifications upon any firePropertyChange
	 *
	 * @param listener  The PropertyChangeListener to be added
	 * @throws NullPointerException If listener is null
	 */
	/*@JVM-1.1+@
	public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
		if(listener == null)
			throw new NullPointerException();
		final int length = listeners.length;
		final PropertyChangeListener[] newArray = new PropertyChangeListener[length + 1];
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
	public synchronized void addPropertyChangeListenerIfAbsent(PropertyChangeListener listener) {
		if(listener == null)
			throw new NullPointerException();
		// Copy while checking if already present.
		final int length = listeners.length;
		final PropertyChangeListener[] newArray = new PropertyChangeListener[length + 1];
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
	 * Remove a PropertyChangeListener from the listener list.
	 * It removes at most one occurrence of the given listener.
	 * If the listener was added multiple times it must be removed
	 * mulitple times.
	 * This removes a PropertyChangeListener that was registered
	 * for all properties, and has no effect if registered for only
	 * one or more specified properties.
	 *
	 * @param listener  The PropertyChangeListener to be removed
	 */
	/*@JVM-1.1+@
	public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
		final int newlen = listeners.length - 1;
		if(newlen < 0 || listener == null)
			return;
		// Copy while searching for element to remove
		final PropertyChangeListener[] newArray = new PropertyChangeListener[newlen];
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
	 * Add a PropertyChangeListener for a specific property.  The listener
	 * will be invoked only when a call on firePropertyChange names that
	 * specific property. However, if a listener is registered both for all
	 * properties and a specific property, it will receive multiple
	 * notifications upon changes to that property.
	 *
	 * @param propertyName  The name of the property to listen on.
	 * @param listener  The PropertyChangeListener to be added
	 * @throws NullPointerException If listener is null
	 */
	/*@JVM-1.1+@
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		if(listener == null)
			throw new NullPointerException();
		PropertyChangeMulticaster child = null;
		synchronized(this) {
			if(children == null) {
				children = new HashMap();
			}
			else {
				child = (PropertyChangeMulticaster) children.get(propertyName);
			}
			if(child == null) {
				child = new PropertyChangeMulticaster(source);
				children.put(propertyName, child);
			}
		}
		child.addPropertyChangeListener(listener);
	}
	/**/
	/**
	 * Add a PropertyChangeListener for a specific property, if it is not
	 * already registered.  The listener
	 * will be invoked only when a call on firePropertyChange names that
	 * specific property.
	 *
	 * @param propertyName  The name of the property to listen on.
	 * @param listener  The PropertyChangeListener to be added
	 * @throws NullPointerException If listener is null
	 */
	/*@JVM-1.1+@
	public void addPropertyChangeListenerIfAbsent(String propertyName, PropertyChangeListener listener) {
		if(listener == null)
			throw new NullPointerException();
		PropertyChangeMulticaster child = null;
		synchronized(this) {
			if(children == null) {
				children = new HashMap();
			}
			else {
				child = (PropertyChangeMulticaster) children.get(propertyName);
			}
			if(child == null) {
				child = new PropertyChangeMulticaster(source);
				children.put(propertyName, child);
			}
		}
		child.addPropertyChangeListenerIfAbsent(listener);
	}
	/**/
	/**
	 * Remove a PropertyChangeListener for a specific property.
	 * Affects only the given property.
	 * If the listener is also registered for all properties,
	 * then it will continue to be registered for them.
	 *
	 * @param propertyName  The name of the property that was listened on.
	 * @param listener  The PropertyChangeListener to be removed
	 */
	/*@JVM-1.1+@
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		final PropertyChangeMulticaster child = getChild(propertyName);
		if(child != null) {
			child.removePropertyChangeListener(listener);
		}
	}
	/**/
	/**
	 * Helper method to relay evt to all listeners.
	 * Called by all public firePropertyChange methods.
	 */
	/*@JVM-1.1+@
	private void multicast(PropertyChangeEvent evt) {
		PropertyChangeListener[] array; // bind in synch block below
		PropertyChangeMulticaster child = null;
		synchronized(this) {
			array = listeners;
			if(children != null && evt.getPropertyName() != null) {
				child = (PropertyChangeMulticaster) children.get(evt.getPropertyName());
			}
		}
		final int length = array.length;
		for(int i = 0; i < length; ++i) {
			array[i].propertyChange(evt);
		}
		if(child != null) {
			child.multicast(evt);
		}
	}
	/**/
	/**
	 * Report a bound property update to any registered listeners.
	 * No event is fired if old and new are equal and non-null.
	 *
	 * @param propertyName  The programmatic name of the property
	 *		that was changed.
	 * @param oldValue  The old value of the property.
	 * @param newValue  The new value of the property.
	 */
	/*@JVM-1.1+@
	public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		if(oldValue == null || newValue == null || !oldValue.equals(newValue)) {
			multicast(new PropertyChangeEvent(source, propertyName, oldValue, newValue));
		}
	}
	/**/
	/**
	 * Report an int bound property update to any registered listeners.
	 * No event is fired if old and new are equal and non-null.
	 * <p>
	 * This is merely a convenience wrapper around the more general
	 * firePropertyChange method that takes Object values.
	 *
	 * @param propertyName  The programmatic name of the property
	 *		that was changed.
	 * @param oldValue  The old value of the property.
	 * @param newValue  The new value of the property.
	 */
	/*@JVM-1.1+@
	public void firePropertyChange(String propertyName, int oldValue, int newValue) {
		if(oldValue != newValue) {
			multicast(new PropertyChangeEvent(source, propertyName, new Integer(oldValue), new Integer(newValue)));
		}
	}
	/**/
	/**
	 * Report a boolean bound property update to any registered listeners.
	 * No event is fired if old and new are equal and non-null.
	 * <p>
	 * This is merely a convenience wrapper around the more general
	 * firePropertyChange method that takes Object values.
	 *
	 * @param propertyName  The programmatic name of the property
	 *		that was changed.
	 * @param oldValue  The old value of the property.
	 * @param newValue  The new value of the property.
	 */
	/*@JVM-1.1+@
	public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
		if(oldValue != newValue) {
			multicast(new PropertyChangeEvent(source, propertyName, new Boolean(oldValue), new Boolean(newValue)));
		}
	}
	/**/
	/**
	 * Fire an existing PropertyChangeEvent to any registered listeners.
	 * No event is fired if the given event's old and new values are
	 * equal and non-null.
	 * @param evt  The PropertyChangeEvent object.
	 */
	/*@JVM-1.1+@
	public void firePropertyChange(PropertyChangeEvent evt) {
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
		PropertyChangeMulticaster child;
		synchronized(this) {
			if(listeners.length != 0)
				return true;
			else if(propertyName == null || children == null)
				return false;
			else {
				child = (PropertyChangeMulticaster) children.get(propertyName);
				if(child == null)
					return false;
			}
		}
		return child.hasListeners(null);
	}
	/**/
	/**
	 * @serialData Null terminated list of <code>PropertyChangeListeners</code>.
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
			final PropertyChangeListener l = listeners[i];
			if(listeners[i] instanceof Serializable) {
				s.writeObject(listeners[i]);
			}
		}
		s.writeObject(null);
	}
	private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
		listeners = new PropertyChangeListener[0]; // paranoically reset
		s.defaultReadObject();
		for(Object listenerOrNull; (listenerOrNull = s.readObject()) != null;) {
			addPropertyChangeListener((PropertyChangeListener) listenerOrNull);
		}
	}
	/**/
}