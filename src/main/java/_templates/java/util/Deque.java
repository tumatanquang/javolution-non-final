/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2005 - Javolution (http://javolution.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package _templates.java.util;
public interface Deque extends Queue {
	void addFirst(Object o);
	void addLast(Object o);
	boolean offerFirst(Object o);
	boolean offerLast(Object o);
	Object removeFirst();
	Object removeLast();
	Object pollFirst();
	Object pollLast();
	Object getFirst();
	Object getLast();
	Object peekFirst();
	Object peekLast();
	boolean removeFirstOccurrence(Object o);
	boolean removeLastOccurrence(Object o);
	void push(Object o);
	Object pop();
	Iterator descendingIterator();
}
