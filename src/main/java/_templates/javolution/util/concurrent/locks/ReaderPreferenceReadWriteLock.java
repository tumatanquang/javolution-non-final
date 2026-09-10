/*
 * File: ReaderPreferenceReadWriteLock.java
 *
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 *
 * History:
 * Date         Who            What
 * 11Jun1998    dl             Create public version
 */
package _templates.javolution.util.concurrent.locks;
/**
 * A ReadWriteLock that prefers waiting readers over
 * waiting writers when there is contention. The range of applicability
 * of this class is very limited. In the majority of situations,
 * writer preference locks provide more reasonable semantics.
 *
 * <p>[ <a href="//gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package.</a> ]</p>
 */
public class ReaderPreferenceReadWriteLock
		extends WriterPreferenceReadWriteLock {
	protected boolean allowReader() {
		return _activeWriter == null;
	}
}