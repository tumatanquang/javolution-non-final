/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2006 - Javolution (http://javolution.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package _templates.javolution.util;
import _templates.java.io.Serializable;
import _templates.java.lang.IllegalStateException;
import _templates.java.lang.Iterable;
import _templates.java.lang.UnsupportedOperationException;
import _templates.java.util.Collection;
import _templates.java.util.Iterator;
import _templates.java.util.List;
import _templates.java.util.ListIterator;
import _templates.java.util.Set;
import _templates.javolution.lang.Realtime;
import _templates.javolution.text.Text;
import _templates.javolution.xml.XMLSerializable;
/**
 * <p> This class represents collections which can quickly be iterated over
 *     (forward or backward) and which an be made {@link #shared() thread-safe}
 *     and/or {@link #unmodifiable() unmodifiable}.</p>
 *
 * <p> Fast collections can be iterated over  without creating new objects
 *     and without using {@link #iterator iterators}.[code]
 * public boolean search(Object item, FastCollection c) {
 *     for(Record r = c.head(), end = c.tail(); (r = r.getNext()) != end;) {
 *         if(item.equals(c.valueOf(r)))
 *             return true;
 *     }
 *     return false;
 * }[/code]</p>
 *
 * <p> Fast collections are thread-safe when marked {@link #shared shared}
 *     (can be read, iterated over or modified concurrently).[code]
 * public class Foo {
 *     private static final Collection<Foo> INSTANCES = new FastTable().shared();
 *     public Foo() {
 *         INSTANCES.add(this);
 *     }
 *     public static void showInstances() {
 *         for(Foo foo : INSTANCES) { // Iterations are thread-safe even if new Foo instances are added.
 *             System.out.println(foo);
 *         }
 *     }
 * }[/code]</p>
 *
 * <p> Users may provide a read-only view of any {@link FastCollection}
 *     instance using the {@link #unmodifiable()} method (the view is
 *     thread-safe if the collection is {@link #shared shared}).[code]
 * class Foo {
 *     private static final FastTable<Foo> INSTANCES = new FastTable().shared();
 *     Foo() {
 *         INSTANCES.add(this);
 *     }
 *     public static Collection<Foo> getInstances() {
 *         return INSTANCES.unmodifiable(); // Returns a public unmodifiable view over the shared collection.
 *     }
 * }[/code]</p>
 *
 * <p> Finally, {@link FastCollection} may use custom {@link #getValueComparator
 *     comparators} for element equality or ordering if the collection is
 *     ordered (e.g. <code>FastTree</code>).</p>
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 5.7.8, July 27, 2026
 */
public abstract class FastCollection/*<E>*/ implements Collection/*<E>*/,
		Iterable/*<E>*/, XMLSerializable, Realtime {
	private static final Object NULL = new Object();
	/**
	 * Default constructor.
	 */
	protected FastCollection() {}
	/**
	 * No-op constructor for internal view classes ({@link Unmodifiable}, {@link Shared}).
	 * Skips all internal data structure initialization.
	 */
	private FastCollection(Void unused) {
		// No initialization — view delegates everything to backing list.
	}
	/**
	 * Returns the number of values in this collection.
	 *
	 * @return the number of values.
	 */
	public abstract int size();
	/**
	 * Returns the head record of this collection; it is the record such as
	 * <code>head().getNext()</code> holds the first collection value.
	 *
	 * @return the head record.
	 */
	public abstract Record head();
	/**
	 * Returns the tail record of this collection; it is the record such as
	 * <code>tail().getPrevious()</code> holds the last collection value.
	 *
	 * @return the tail record.
	 */
	public abstract Record tail();
	/**
	 * Returns the collection value for the specified record.
	 *
	 * @param record the record whose current value is returned.
	 * @return the current value.
	 */
	public abstract Object/*{E}*/ valueOf(Record record);
	/**
	 * Deletes the specified record from this collection.
	 *
	 * <p> Implementation must ensure that removing a record from the
	 *     collection does not affect in any way the records preceding
	 *     the record being removed (it might affect the next records though,
	 *     e.g. in a list collection, the indices of the subsequent records
	 *     will change).</p>
	 *
	 * @param record the record to be removed.
	 * @throws UnsupportedOperationException if not supported.
	 */
	public abstract void delete(Record record);
	/**
	 * Returns the unmodifiable view associated to this collection.
	 * Attempts to modify the returned collection result in an
	 * {@link UnsupportedOperationException} being thrown.
	 *
	 * @return the unmodifiable view over this collection.
	 */
	public FastCollection/*<E>*/ unmodifiable() {
		return new Unmodifiable(this);
	}
	/**
	 * <p> Returns a thread-safe read-write view of this collection.</p>
	 * <p> The default implementation performs synchronization on read and write.
	 *     Sub-classes may provide more efficient implementation (e.g.
	 *     only synchronizing on writes modifying the internal data structure).</p>
	 * <p> Having a shared collection does not mean that modifications made
	 *     by onethread are automatically viewed by others thread. Which in practice
	 *     is not an issue. In a well-behaved system, threads need to synchronize
	 *     only at predetermined synchronization points (the fewer the better).</p>
	 *
	 * @return a thread-safe collection.
	 */
	public FastCollection/*<E>*/ shared() {
		return new Shared(this);
	}
	/**
	 * Returns an iterator over the elements in this collection
	 * (allocated on the stack when executed in a
	 * {@link _templates.javolution.context.StackContext StackContext}).
	 *
	 * @return an iterator over this collection's elements.
	 */
	public Iterator/*<E>*/ iterator() {
		return FastIterator.valueOf(this);
	}
	/**
	 * Returns the value comparator for this collection (default
	 * {@link FastComparator#DEFAULT}).
	 *
	 * @return the comparator to use for value equality (or ordering if
	 *        the collection is ordered)
	 */
	public FastComparator/*<? super E>*/ getValueComparator() {
		return FastComparator.DEFAULT;
	}
	/**
	 * Appends the specified value to the end of this collection
	 * (optional operation).
	 *
	 * <p>Note: This default implementation always throws
	 *          <code>UnsupportedOperationException</code>.</p>
	 *
	 * @param value the value to be appended to this collection.
	 * @return <code>true</code> (as per the general contract of the
	 *         <code>Collection.add</code> method).
	 * @throws UnsupportedOperationException if not supported.
	 */
	public boolean add(Object/*{E}*/ value) {
		throw new UnsupportedOperationException();
	}
	/**
	 * Removes the first occurrence in this collection of the specified value
	 * (optional operation).
	 *
	 * @param value the value to be removed from this collection.
	 * @return <code>true</code> if this collection contained the specified
	 *         value; <code>false</code> otherwise.
	 * @throws UnsupportedOperationException if not supported.
	 */
	public boolean remove(Object value) {
		final FastComparator valueComp = getValueComparator();
		for(Record r = head(), end = tail(); (r = r.getNext()) != end;) {
			if(valueComp.areEqual(value, valueOf(r))) {
				delete(r);
				return true;
			}
		}
		return false;
	}
	/**
	 * Removes all of the values from this collection (optional operation).
	 *
	 * @throws UnsupportedOperationException if not supported.
	 */
	public void clear() {
		// Removes last record until empty.
		for(Record head = head(), r = tail().getPrevious(); r != head; r = r
				.getPrevious()) {
			delete(r);
		}
	}
	/**
	 * Indicates if this collection is empty.
	 *
	 * @return <code>true</code> if this collection contains no value;
	 *         <code>false</code> otherwise.
	 */
	public boolean isEmpty() {
		return size() == 0;
	}
	/**
	 * Indicates if this collection contains the specified value.
	 *
	 * @param value the value whose presence in this collection
	 *        is to be tested.
	 * @return <code>true</code> if this collection contains the specified
	 *         value;<code>false</code> otherwise.
	 */
	public boolean contains(Object value) {
		final FastComparator valueComp = getValueComparator();
		for(Record r = head(), end = tail(); (r = r.getNext()) != end;) {
			if(valueComp.areEqual(value, valueOf(r)))
				return true;
		}
		return false;
	}
	/**
	 * Appends all of the values in the specified collection to the end of
	 * this collection, in the order that they are returned by the specified
	 * collection's iterator.
	 *
	 * @param values collection whose values are to be added to this collection.
	 * @return <code>true</code> if this collection changed as a result of
	 *         the call; <code>false</code> otherwise.
	 */
	public boolean addAll(Collection/*<? extends E>*/ values) {
		boolean modified = false;
		for(final Iterator /*<? extends E>*/ itr = values.iterator(); itr
				.hasNext();) {
			if(add(itr.next())) {
				modified = true;
			}
		}
		return modified;
	}
	/**
	 * Indicates if this collection contains all of the values of the
	 * specified collection.
	 *
	 * @param  values collection to be checked for containment in this collection.
	 * @return <code>true</code> if this collection contains all of the values
	 *         of the specified collection; <code>false</code> otherwise.
	 */
	public boolean containsAll(Collection/*<?>*/ values) {
		for(final Iterator /*<?>*/ itr = values.iterator(); itr.hasNext();) {
			if(!contains(itr.next()))
				return false;
		}
		return true;
	}
	/**
	 * Removes from this collection all the values that are contained in the
	 * specified collection.
	 *
	 * @param values collection that defines which values will be removed from
	 *          this collection.
	 * @return <code>true</code> if this collection changed as a result of
	 *         the call; <code>false</code> otherwise.
	 */
	public boolean removeAll(Collection/*<?>*/ values) {
		boolean modified = false;
		// Iterates from the tail and remove the record if present in c.
		for(Record head = head(), r = tail()
				.getPrevious(), previous; r != head; r = previous) {
			previous = r.getPrevious(); // Saves previous.
			if(FastCollection.contains(values, valueOf(r),
					getValueComparator())) {
				delete(r);
				modified = true;
			}
		}
		return modified;
	}
	private static final boolean contains(Collection c, Object obj,
			FastComparator cmp) {
		if(c instanceof FastCollection
				&& ((FastCollection) c).getValueComparator().equals(cmp))
			return c.contains(obj); // Direct is ok (same value comparator).
		for(final Iterator /*<?>*/ itr = c.iterator(); itr.hasNext();) {
			if(cmp.areEqual(obj, itr.next()))
				return true;
		}
		return false;
	}
	/**
	 * Retains only the values in this collection that are contained in the
	 * specified collection.
	 *
	 * @param values collection that defines which values this set will retain.
	 * @return <code>true</code> if this collection changed as a result of
	 *         the call; <code>false</code> otherwise.
	 */
	public boolean retainAll(Collection/*<?>*/ values) {
		boolean modified = false;
		// Iterates from the tail and remove the record if not present in c.
		for(Record head = head(), r = tail()
				.getPrevious(), previous; r != head; r = previous) {
			previous = r.getPrevious(); // Saves previous.
			if(!FastCollection.contains(values, valueOf(r),
					getValueComparator())) {
				delete(r);
				modified = true;
			}
		}
		return modified;
	}
	/**
	 * Returns a new array allocated on the heap containing all of the values
	 * in this collection in proper sequence.
	 * <p> Note: To avoid heap allocation {@link #toArray(Object[])} is
	 *           recommended.</p>
	 * @return <code>toArray(new Object[size()])</code>
	 */
	public Object[] toArray() {
		return toArray(new Object[size()]);
	}
	/**
	 * Fills the specified array with the values of this collection in
	 * the proper sequence.
	 *
	 * <p> Note: Unlike standard Collection, this method does not try to resize
	 *           the array using reflection (which might not be supported) if
	 *           the array is too small. UnsupportedOperationException is raised
	 *           if the specified array is too small for this collection.</p>
	 *
	 * @param  array the array into which the values of this collection
	 *         are to be stored.
	 * @return the specified array.
	 * @throws UnsupportedOperationException if <code>array.length < size()</code>
	 */
	public Object/*{<T> T}*/[] toArray(Object/*{T}*/[] array) {
		final int size = size();
		if(array.length < size)
			throw new UnsupportedOperationException(
					"Destination array too small");
		if(array.length > size) {
			array[size] = null; // As per Collection contract.
		}
		int i = 0;
		final Object[] arrayView = array;
		for(Record r = head(), end = tail(); (r = r.getNext()) != end;) {
			arrayView[i++] = valueOf(r);
		}
		return array;
	}
	/**
	 * Returns the textual representation of this collection.
	 *
	 * @return this collection textual representation.
	 */
	public Text toText() {
		// We use Text concatenation instead of TextBuilder to avoid copying
		// the text representation of the record values (unknown length).
		Text text = Text.valueOf("{");
		for(Record r = head(), end = tail(); (r = r.getNext()) != end;) {
			text = text.plus(valueOf(r));
			if(r.getNext() != end) {
				text = text.plus(", ");
			}
		}
		return text.plus("}");
	}
	/**
	 * Returns the <code>String</code> representation of this
	 * {@link FastCollection}.
	 *
	 * @return <code>toText().toString()</code>
	 */
	public String toString() {
		return toText().toString();
	}
	/**
	 * Compares the specified object with this collection for equality.
	 * The default behavior is to consider two collections equal if they
	 * hold the same values and have the same iterative order if any of
	 * the collections is an ordered collection ({@link List} instances).
	 * Equality comparisons are performed using this collection
	 * {@link #getValueComparator value comparator}.
	 *
	 * @param obj the object to be compared for equality with this collection
	 * @return <code>true</code> if the specified object is a collection with
	 *         the same content and iteration order when necessary;
	 *         <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		if(this instanceof List)
			return obj instanceof List ? equalsOrder((List) obj) : false;
		if(obj instanceof List)
			return false; // 'this' is not a list but obj is!
		if(!(obj instanceof Collection))
			return false; // Can only compare collections.
		final Collection that = (Collection) obj;
		return this == that || size() == that.size() && containsAll(that);
	}
	private final boolean equalsOrder(List that) {
		if(that == this)
			return true;
		if(size() != that.size())
			return false;
		final Iterator thatIterator = that.iterator();
		final FastComparator comp = getValueComparator();
		for(Record r = head(), end = tail(); (r = r.getNext()) != end;) {
			final Object o1 = valueOf(r);
			final Object o2 = thatIterator.next();
			if(!comp.areEqual(o1, o2))
				return false;
		}
		return true;
	}
	/**
	 * Returns the hash code for this collection. For non-ordered collection
	 * the hashcode of this collection is the sum of the hashcode of its
	 * values.
	 *
	 * @return the hash code for this collection.
	 */
	public int hashCode() {
		if(this instanceof List)
			return hashCodeList();
		final FastComparator valueComp = getValueComparator();
		int hash = 0;
		for(Record r = head(), end = tail(); (r = r.getNext()) != end;) {
			hash += valueComp.hashCodeOf(valueOf(r));
		}
		return hash;
	}
	private final int hashCodeList() {
		final FastComparator comp = getValueComparator();
		int h = 1;
		for(Record r = head(), end = tail(); (r = r.getNext()) != end;) {
			h = 31 * h + comp.hashCodeOf(valueOf(r));
		}
		return h;
	}
	/**
	 * This interface represents the collection records which can directly be
	 * iterated over.
	 */
	public interface Record {
		/**
		 * Returns the record before this one.
		 *
		 * @return the previous record.
		 */
		public Record getPrevious();
		/**
		 * Returns the record after this one.
		 *
		 * @return the next record.
		 */
		public Record getNext();
	}
	/**
	 * Marker class for the no-op view constructor.
	 * Not instantiated — only {@code null} is passed.
	 */
	static final class Void {}
	/**
	 * This inner class represents an unmodifiable view over the collection.
	 */
	private static final class Unmodifiable/*<E>*/ extends FastCollection
			/*<E>*/ implements List/*<E>*/, Set/*<E>*/ {
		private final FastCollection/*<E>*/ _fc;
		private Unmodifiable(FastCollection/*<E>*/ fc) {
			super((Void) null);
			_fc = fc;
		}
		// Forwards...
		public final FastCollection/*<E>*/ unmodifiable() {
			return this;
		}
		// Disallows...
		public final FastCollection/*<E>*/ shared() {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		// Implements abstract method.
		public final int size() {
			return _fc.size();
		}
		// Implements abstract method.
		public final Record head() {
			return _fc.head();
		}
		// Implements abstract method.
		public final Record tail() {
			return _fc.tail();
		}
		// Implements abstract method.
		public final Object/*{E}*/ valueOf(Record record) {
			return _fc.valueOf(record);
		}
		// Forwards...
		public final boolean contains(Object value) {
			return _fc.contains(value);
		}
		// Forwards...
		public final boolean containsAll(Collection values) {
			return _fc.containsAll(values);
		}
		// Forwards...
		public final FastComparator getValueComparator() {
			return _fc.getValueComparator();
		}
		// Disallows...
		public final boolean add(Object obj) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		// Disallows...
		public final void delete(Record node) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		//////////////////////////////////////////
		// List interface supplementary methods //
		//////////////////////////////////////////
		public final boolean addAll(int index, Collection c) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Object/*{E}*/ get(int index) {
			return (Object/*{E}*/) ((List/*<E>*/) _fc).get(index);
		}
		public final Object/*{E}*/ set(int index, Object/*{E}*/ element) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final void add(int index, Object element) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final Object/*{E}*/ remove(int index) {
			throw new UnsupportedOperationException("Unmodifiable");
		}
		public final int indexOf(Object value) {
			return ((List/*<E>*/) _fc).indexOf(value);
		}
		public final int lastIndexOf(Object value) {
			return ((List/*<E>*/) _fc).lastIndexOf(value);
		}
		public final ListIterator/*<E>*/ listIterator() {
			throw new UnsupportedOperationException(
					"List iterator not supported for unmodifiable collection");
		}
		public final ListIterator/*<E>*/ listIterator(int index) {
			throw new UnsupportedOperationException(
					"List iterator not supported for unmodifiable collection");
		}
		public final List/*<E>*/ subList(int fromIndex, int toIndex) {
			throw new UnsupportedOperationException(
					"Sub-List not supported for unmodifiable collection");
		}
	}
	/**
	 * This inner class represents a thread safe view (read-write) over the
	 * collection.
	 */
	private static final class Shared/*<E>*/ extends FastCollection
			/*<E>*/ implements Collection/*<E>*/, Serializable {
		private final FastCollection/*<E>*/ _fc; // Backing FastCollection
		private final Object _mutex; // Object on which to synchronize
		private Shared(FastCollection/*<E>*/ fc) {
			super((Void) null);
			_fc = fc;
			_mutex = this;
		}
		// Forwards...
		public final FastCollection/*<E>*/ unmodifiable() {
			return _fc.unmodifiable();
		}
		// Forwards...
		public final FastCollection/*<E>*/ shared() {
			return this;
		}
		public final int size() {
			synchronized(_mutex) {
				return _fc.size();
			}
		}
		public final Record head() {
			synchronized(_mutex) {
				return _fc.head();
			}
		}
		public final Record tail() {
			synchronized(_mutex) {
				return _fc.tail();
			}
		}
		public final Object/*{E}*/ valueOf(Record record) {
			synchronized(_mutex) {
				return (Object/*{E}*/) _fc.valueOf(record);
			}
		}
		public final void delete(Record record) {
			synchronized(_mutex) {
				_fc.delete(record);
			}
		}
		public final boolean isEmpty() {
			synchronized(_mutex) {
				return _fc.isEmpty();
			}
		}
		public final boolean contains(Object value) {
			synchronized(_mutex) {
				return _fc.contains(value);
			}
		}
		public final Object[] toArray() {
			synchronized(_mutex) {
				return _fc.toArray();
			}
		}
		public final Object[] toArray(Object[] array) {
			synchronized(_mutex) {
				return _fc.toArray(array);
			}
		}
		public final Iterator/*<E>*/ iterator() {
			synchronized(_mutex) {
				if(_fc instanceof List)
					return new ListArrayIterator(_fc.toArray());
				return new CollectionArrayIterator(_fc.toArray());
			}
		}
		public final boolean add(Object/*{E}*/ value) {
			synchronized(_mutex) {
				return _fc.add(value);
			}
		}
		public final boolean remove(Object value) {
			synchronized(_mutex) {
				return _fc.remove(value);
			}
		}
		public final boolean containsAll(Collection/*<?>*/ values) {
			synchronized(_mutex) {
				return _fc.containsAll(values);
			}
		}
		public final boolean addAll(Collection/*<? extends E>*/ values) {
			synchronized(_mutex) {
				return _fc.addAll(values);
			}
		}
		public final boolean removeAll(Collection/*<?>*/ values) {
			synchronized(_mutex) {
				return _fc.removeAll(values);
			}
		}
		public final boolean retainAll(Collection/*<?>*/ values) {
			synchronized(_mutex) {
				return _fc.retainAll(values);
			}
		}
		public final void clear() {
			synchronized(_mutex) {
				_fc.clear();
			}
		}
		public final String toString() {
			synchronized(_mutex) {
				return _fc.toString();
			}
		}
		private final class ListArrayIterator implements Iterator/*<E>*/ {
			private final Object[] _elements;
			private int _index;
			private int _removed;
			public ListArrayIterator(Object[] elements) {
				_elements = elements;
			}
			public boolean hasNext() {
				return _index < _elements.length;
			}
			public Object/*{E}*/ next() {
				return (Object/*{E}*/) _elements[_index++];
			}
			public void remove() {
				if(_index == 0)
					throw new IllegalStateException();
				final Object/*{E}*/ removed = (Object/*{E}*/) _elements[_index
						- 1];
				if(removed == NULL)
					throw new IllegalStateException();
				_elements[_index - 1] = NULL;
				++_removed;
				synchronized(_mutex) {
					((List/*<E>*/) _fc).remove(_index - _removed);
				}
			}
		}
		private final class CollectionArrayIterator implements Iterator/*<E>*/ {
			private final Object[] _elements;
			private int _index;
			private Object/*{E}*/ _next;
			public CollectionArrayIterator(Object[] elements) {
				_elements = elements;
			}
			public boolean hasNext() {
				return _index < _elements.length;
			}
			public Object/*{E}*/ next() {
				return _next = (Object/*{E}*/) _elements[_index++];
			}
			public void remove() {
				if(_next == null)
					throw new IllegalStateException();
				synchronized(_mutex) {
					_fc.remove(_next);
				}
				_next = null;
			}
		}
	}
}