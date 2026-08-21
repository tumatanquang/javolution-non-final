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
 * <p> A collection designed for holding elements prior to processing.
 *     Besides basic {@link Collection} operations, queues provide additional
 *     insertion, extraction, and inspection operations.</p>
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 6.1.0, August 22, 2026
 */
public interface Queue extends Collection {
	/**
	 * Inserts the specified element into this queue if it is possible to do so
	 * immediately without violating capacity restrictions, returning
	 * <code>true</code> upon success and throwing an <code>IllegalStateException</code>
	 * if no space is currently available.
	 *
	 * @param value the element to add.
	 * @return <code>true</code> (as specified by {@link Collection#add}).
	 * @throws IllegalStateException if the element cannot be added at this
	 *         time due to capacity restrictions.
	 * @throws NullPointerException if the specified element is <code>null</code>
	 *         and this queue does not permit <code>null</code> elements.
	 */
	boolean add(Object value);
	/**
	 * Inserts the specified element into this queue if it is possible to do
	 * so immediately without violating capacity restrictions.
	 *
	 * @param value the element to add.
	 * @return <code>true</code> if the element was added to this queue, else
	 *         <code>false</code>.
	 * @throws NullPointerException if the specified element is <code>null</code>
	 *         and this queue does not permit <code>null</code> elements.
	 */
	boolean offer(Object value);
	/**
	 * Retrieves and removes the head of this queue. This method differs
	 * from {@link #poll() poll()} only in that it throws an exception if this
	 * queue is empty.
	 *
	 * @return the head of this queue.
	 * @throws NoSuchElementException if this queue is empty.
	 */
	Object remove();
	/**
	 * Retrieves and removes the head of this queue,
	 * or returns <code>null</code> if this queue is empty.
	 *
	 * @return the head of this queue, or <code>null</code> if this queue is empty.
	 */
	Object poll();
	/**
	 * Retrieves, but does not remove, the head of this queue. This method
	 * differs from {@link #peek() peek()} only in that it throws an exception
	 * if this queue is empty.
	 *
	 * @return the head of this queue.
	 * @throws NoSuchElementException if this queue is empty.
	 */
	Object element();
	/**
	 * Retrieves, but does not remove, the head of this queue,
	 * or returns <code>null</code> if this queue is empty.
	 *
	 * @return the head of this queue, or <code>null</code> if this queue is empty.
	 */
	Object peek();
}