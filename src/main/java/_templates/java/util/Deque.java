/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2005 - Javolution (http://javolution.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package _templates.java.util;
/**
 * <p> A linear collection that supports element insertion and removal at
 *     both ends. The name <i>deque</i> is short for "double ended queue"
 *     and is usually pronounced "deck".</p>
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 6.1.0, August 22, 2026
 */
public interface Deque extends Queue {
	/**
	 * Inserts the specified element at the front of this deque.
	 *
	 * @param value the element to add.
	 * @throws NullPointerException if the specified element is <code>null</code>
	 *         and this deque does not permit <code>null</code> elements.
	 */
	void addFirst(Object value);
	/**
	 * Inserts the specified element at the end of this deque.
	 *
	 * @param value the element to add.
	 * @throws NullPointerException if the specified element is <code>null</code>
	 *         and this deque does not permit <code>null</code> elements.
	 */
	void addLast(Object value);
	/**
	 * Inserts the specified element at the front of this deque.
	 *
	 * @param value the element to add.
	 * @return <code>true</code> if the element was added to this deque, else <code>false</code>.
	 * @throws NullPointerException if the specified element is <code>null</code>
	 *         and this deque does not permit <code>null</code> elements.
	 */
	boolean offerFirst(Object value);
	/**
	 * Inserts the specified element at the end of this deque.
	 *
	 * @param value the element to add.
	 * @return <code>true</code> if the element was added to this deque, else <code>false</code>.
	 * @throws NullPointerException if the specified element is <code>null</code>
	 *         and this deque does not permit <code>null</code> elements.
	 */
	boolean offerLast(Object value);
	/**
	 * Retrieves and removes the first element of this deque.
	 *
	 * @return the head of this deque.
	 * @throws NoSuchElementException if this deque is empty.
	 */
	Object removeFirst();
	/**
	 * Retrieves and removes the last element of this deque.
	 *
	 * @return the tail of this deque.
	 * @throws NoSuchElementException if this deque is empty.
	 */
	Object removeLast();
	/**
	 * Retrieves and removes the first element of this deque,
	 * or returns <code>null</code> if this deque is empty.
	 *
	 * @return the head of this deque, or <code>null</code> if this deque is empty.
	 */
	Object pollFirst();
	/**
	 * Retrieves and removes the last element of this deque,
	 * or returns <code>null</code> if this deque is empty.
	 *
	 * @return the tail of this deque, or <code>null</code> if this deque is empty.
	 */
	Object pollLast();
	/**
	 * Retrieves, but does not remove, the first element of this deque.
	 *
	 * @return the head of this deque.
	 * @throws NoSuchElementException if this deque is empty.
	 */
	Object getFirst();
	/**
	 * Retrieves, but does not remove, the last element of this deque.
	 *
	 * @return the tail of this deque.
	 * @throws NoSuchElementException if this deque is empty.
	 */
	Object getLast();
	/**
	 * Retrieves, but does not remove, the first element of this deque,
	 * or returns <code>null</code> if this deque is empty.
	 *
	 * @return the head of this deque, or <code>null</code> if this deque is empty.
	 */
	Object peekFirst();
	/**
	 * Retrieves, but does not remove, the last element of this deque,
	 * or returns <code>null</code> if this deque is empty.
	 *
	 * @return the tail of this deque, or <code>null</code> if this deque is empty.
	 */
	Object peekLast();
	/**
	 * Removes the first occurrence of the specified element from this deque.
	 *
	 * @param value element to be removed from this deque, if present.
	 * @return <code>true</code> if an element was removed as a result of this call.
	 * @throws NullPointerException if the specified element is <code>null</code>
	 *         and this deque does not permit <code>null</code> elements.
	 */
	boolean removeFirstOccurrence(Object value);
	/**
	 * Removes the last occurrence of the specified element from this deque.
	 *
	 * @param value element to be removed from this deque, if present.
	 * @return <code>true</code> if an element was removed as a result of this call.
	 * @throws NullPointerException if the specified element is <code>null</code>
	 *         and this deque does not permit <code>null</code> elements.
	 */
	boolean removeLastOccurrence(Object value);
	/**
	 * Pushes an element onto the stack represented by this deque (in other words,
	 * at the head of this deque).
	 *
	 * @param value the element to push.
	 * @throws NullPointerException if the specified element is <code>null</code>
	 *         and this deque does not permit <code>null</code> elements.
	 */
	void push(Object value);
	/**
	 * Pops an element from the stack represented by this deque. In other words,
	 * removes and returns the first element of this deque.
	 *
	 * @return the element at the front of this deque (which is the top of the stack represented by this deque).
	 * @throws NoSuchElementException if this deque is empty.
	 */
	Object pop();
	/**
	 * Returns an iterator over the elements in this deque in reverse sequential order.
	 * The elements will be returned in order from last (tail) to first (head).
	 *
	 * @return an iterator over the elements in this deque in reverse sequence.
	 */
	Iterator descendingIterator();
}