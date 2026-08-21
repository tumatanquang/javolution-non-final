# Javolution Extended

Based on the [Javolution 5.5.1](https://mvnrepository.com/artifact/javolution/javolution/5.5.1) source code, combined with [rawnet/javolution](https://github.com/rawnet/javolution) and [Doug Lea's `util.concurrent` package in Release 1.3.4](https://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html).

Since version **5.4**, upstream Javolution has marked most `public` classes and methods as `final`, preventing overriding. This project removes those restrictions and continues to develop and extend the library.

## Changelog:

<details open>
	<summary>Since v6.0.0: Major Changes</summary>

- **Major Primitive Collections Architecture Upgrade (`Fast<Type>Table` & `Fast<Type>Set`)**:
	+ Replaced the 1D-array implementation with a **two-tier segmented array architecture** (`_low` + `_high[block][1024]`), mirroring the design of `FastTable`.
	+ Capacity increases smoothly in fixed 1024-element blocks without copying existing memory, eliminating large object heap fragmentation (Zero-GC Fragmentation) and providing deterministic $O(1)$ append time.
	+ Renamed all primitive list classes to `Fast<Type>Table` (`FastBooleanTable`, `FastByteTable`, `FastCharTable`, `FastDoubleTable`, `FastFloatTable`, `FastIntTable`, `FastLongTable`, `FastShortTable`) for complete 1:1 symmetry with `FastTable`.
	+ Renamed base class `FastPrimitiveList` to `FastPrimitiveCollection` as the unified abstract foundation for all primitive collection types.
	+ This is an intentional breaking migration: legacy `Fast<Type>ArrayList` and `FastPrimitiveList` classes are removed without deprecated aliases or adapters. Consumers must update imports and recompile.
	+ Added in-place `sort()` (Quicksort) across all primitive tables with zero object allocation.
	+ Instant memory reclamation: `trimToSize()` immediately dereferences unused 1024-element blocks for GC.
	+ **Introduced Primitive Sets (`Fast<Type>Set`)**: Added 8 high-performance, zero-boxing primitive set classes (`FastBooleanSet`, `FastByteSet`, `FastCharSet`, `FastDoubleSet`, `FastFloatSet`, `FastIntSet`, `FastLongSet`, `FastShortSet`). `FastBooleanSet` stores the two possible values directly; the other seven use flat primitive arrays, byte state tracking, and linear probing for expected $O(1)$ lookup. They avoid per-value boxing and entry allocation, typically achieving 80–90% retained-memory savings over standard boxed collections (depending on the primitive type, set size, load factor, JVM, and comparison baseline).
	+ Full IEEE 754 floating-point bitwise semantics (`Double.doubleToLongBits`, `Float.floatToIntBits`) supported across `FastDoubleSet`, `FastFloatSet`, `FastDoubleTable`, and `FastFloatTable`.
	+ Dedicated primitive iterators, read-only (`unmodifiable()`), and thread-safe (`shared()`) views supported across all 8 primitive tables and sets.
- **Queue, Deque & Stack Architecture Support (`FastList` & `FastTable`)**:
	+ `FastList` and `FastTable` now directly implement `Deque` and `Queue` (`java.util.Deque` on Java 6+, `java.util.Queue` on Java 5+, and `j2me.util.Deque` / `j2me.util.Queue` on J2ME via newly added compatibility interface).
	+ `FastList` provides constant-time $O(1)$ operations at both ends; `FastTable` provides constant-time operations at the tail while front insertions/removals remain $O(n)$.
	+ Added complete Deque and Stack operations (`push`, `pop`, `peek`, `offer`, `poll`, `element`, `addFirst`, `addLast`, `removeFirst`, `removeLast`, `getFirst`, `getLast`, `peekFirst`, `peekLast`, `offerFirst`, `offerLast`, `pollFirst`, `pollLast`, `removeFirstOccurrence`, `removeLastOccurrence`, `descendingIterator`) across `FastList`, `FastTable`, and their `Unmodifiable` / `Shared` views.
	+ Added primitive stack and queue helper methods (`push`, `pop`, `peek`, `offer`, `poll`, `element`, `addFirst`, `addLast`, `removeFirst`, `removeLast`, `getFirst`, `getLast`) across all 8 `Fast<Type>Table` classes.
- **Core Utility, Math & BitSet Improvements**:
	+ **`FastBitSet`**: Fixed operator precedence in `length()` calculation (`numberOfLeadingZeros`), added negative index checks on `nextSetBit`/`nextClearBit`, and optimized `hashCode()`.
	+ **`Index`**: Enhanced `rangeOf` and `valuesOf` factory methods to use pre-allocated `FastTable` instances.
	+ **`MathLib`**: Optimized and standardized `final` modifiers across mathematical primitives, hyperbolic functions (`sinh`, `cosh`, `tanh`), logarithms, and exponentials.
- **Unified Standalone Test Suite**:
	+ Consolidated all test suites under single aggregate `AllTestSuite` and standalone runner `AllTestRunner` (executable via `ant -Dtest.runtime=j2me|1.4|1.5|1.6 test-all`).
	+ Added dedicated test suites: `PrimitiveCollectionTestSuite`, `QueueDequeTestSuite`, and `FastBitSetTestSuite`.
- **Packaging, Tooling & Documentation Update**:
	+ Overhauled Javadoc documentation across `javolution.util.primitive`, concurrency, and core packages.
	+ Added Linux/Unix interactive launcher script [`compile.sh`](compile.sh) matching [`compile.bat`](compile.bat).
	+ Updated artifact coordinate to `javolution:javolution-extended:6.0.0` across `pom.xml`, `build.xml`, and CI/CD release workflows.
</details>

<details>
	<summary>Since v5.7.8:</summary>

- Updated `README.md` and Javadoc: Fixed several Javadoc layout issues and code block formatting across multiple packages (`context`, `lang`, `io`, `xml`, etc.).
- Fixed the issue where `FastList.newInstance()` was missing a generic type parameter when compiling on Java 5+.
- **Added `FastPrimitiveList` and primitive iterators**:
	+ Introduced the abstract base class `FastPrimitiveList` and the `FastPrimitiveIterator` interface under `javolution.util.primitive`.
	+ Added dedicated primitive iterator implementations (`FastBooleanIterator`, `FastByteIterator`, `FastCharIterator`, `FastDoubleIterator`, `FastFloatIterator`, `FastIntIterator`, `FastLongIterator`, `FastShortIterator`).
- **Enhanced primitive array lists (`Fast<Type>ArrayList`)**:
	+ Implemented `iterator()`, `reset()`, and `trimToSize()` methods across all primitive array list classes.
	+ Added support for `unmodifiable()` and `shared()` views along with their respective `Unmodifiable` and `Shared` inner classes.
- **Optimized `Unmodifiable` and `Shared` collection wrappers**:
	+ Added `final` modifiers to proxy methods in `Unmodifiable` and `Shared` inner classes for improved performance and lock consistency.
	+ Standardized parameter names.
</details>

<details>
	<summary>Since v5.7.7:</summary>

- Updated `README.md`.
- **Removed `AbstractList`**: Now, `FastList` and `FastTable` will directly `extends FastCollection`.
- **Removed `javolution.util.ReentrantLock`**: Use `javolution.util.concurrent.locks.ReentrantLock` instead.
- **Removed the `javolution.util.internal.collection` package and its inner classes**.
- Reimplemented `Unmodifiable` and `Shared` for `FastCollection`, `FastList`, and `FastTable`.
- The source code will now use the Java standard line width, reduced from **120** to **80** characters.
</details>

<details>
	<summary>Since v5.7.6:</summary>

- Updated `README.md`.
- **Renamed `FastAbstractList` to `AbstractList`**.
- Fixed an incorrect `javac` attribute name in the [`build.xml`](https://github.com/tumatanquang/javolution-extended/blob/main/build.xml) file.
- Fixed several Javadoc layout issues.
- Reimplemented the `unmodifiable()` and `shared()` methods for `FastList` and `FastTable`.
- For `SharedFastList` and `SharedFastTable`:
	+ Fixed the issue where `subList()` uses the wrong lock type, changing it from `writeLock()` to `readLock()`.
	+ Added thread-safe implementations for `iterator()`, `listIterator()`, and `listIterator(int)`.
- For `Fast<Type>ArrayList` in the `javolution.util.primitive` package:
	+ Optimized parameter names for some methods.
	+ If compiled in Java 5+, the `toString()` method will use the internal `StringBuilder` to improve performance.
	+ Optimized the `indexOf()` and `lastIndexOf()` methods: They will return `-1` if the list is empty.
- Added `PropertyChangeMulticaster.java` and `VetoableChangeMulticaster.java` from [Doug Lea's `util.concurrent` package in Release 1.3.4](https://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html): However, due to the limitation of not having JavaBeans on J2ME, these two classes will have empty bodies by default (not implemented).
- Changed the use of `@exception` to `@throws` in Javadoc.
- The file `jdk-1_5_0_22-linux-amd64-direct.bin` has been renamed to `jdk-5u22-linux-x64.bin` in the `lib/` directory.
</details>

<details>
	<summary>Since v5.7.5:</summary>

- Revert to using `colapi.jar` when building Javadoc.
- [Doug Lea's `util.concurrent` package in Release 1.3.4](https://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html) has been implemented in `javolution.util.concurrent`, except for `PropertyChangeMulticaster.java` and `VetoableChangeMulticaster.java` for maintenance and backport reasons.
- `javolution.util.concurrent` will be split into sub-packages similar to the JDK:
	+ `java.util.concurrent` can be replaced with `javolution.util.concurrent`.
	+ `java.util.concurrent.atomic` can be replaced with `javolution.util.concurrent.atomic`.
	+ `java.util.concurrent.locks` can be replaced with `javolution.util.concurrent.locks`.
- Added Javadoc for packages: `javolution.util.concurrent`, `javolution.util.concurrent.atomic`, `javolution.util.concurrent.locks`, `javolution.util.internal.collection`, and `javolution.util.primitive`.
- Added an override for `FastSet.shared()`.
- Added an iterator method for `FastSet`.
- Optimized `javolution.util.concurrent.ConcurrentHashMap` for high concurrency and low GC pressure:
	+ Converted the internal chain links to a `volatile Entry _next` pointer to ensure safe lock-free reads.
	+ Redesigned the removal logic to perform in-place unlinking (`prev._next = e._next`) under segment locks, completely eliminating the original Copy-on-Write chain replication overhead and achieving zero-allocation removals.
	+ Added static `newKeySet()` and `newKeySet(int)` helper methods mimicking JDK 8+ concurrent set support.
	+ Implemented the nested static class `KeySetView` to support concurrent, reusable Set views of map keys, fully backported for J2ME and legacy JDK (1.4+) compatibility.
</details>

<details>
	<summary>Since v5.7.4:</summary>

- Updated property names in [build.xml](https://github.com/tumatanquang/javolution-extended/blob/main/build.xml).
- Javadoc build will now use `colapi-2.0.jar`.
- With the `javolution.util.concurrent.locks` package:
	+ Changed the naming convention of classes in the package to be consistent with classes in other packages.
	+ Added the `ReaderPreferenceReadWriteLock` class.
	+ Renamed the `ReentrantReadWriteLock` class to `ReentrantWriterPreferenceReadWriteLock` to be consistent with Doug Lea's implementation.
	+ Reformatted the Javadocs.
- Reformatted the `FastTable` code.
- Reformatted the `FastList` code.
</details>

<details>
	<summary>Since v5.7.3:</summary>

- Unified build output set to `target/classes`.
- With `FastTable`:
	+ The ~~`requireNotNull()`~~ method has been removed.
	+ The ~~`isNullDisallowed()`~~ method has been removed.
	+ If you want the table to not allow `null` values, use constructors that specify `boolean rejectNulls` such as: `FastTable(boolean rejectNulls)` or `FastTable(int capacity, boolean rejectNulls)` or `FastTable(Collection values, boolean rejectNulls)`.
	+ Updated Javadoc.
- Updated `README.md`.
- The new [Javadoc](https://tumatanquang.github.io/javolution-extended/index.html) page has been updated.
</details>

<details>
	<summary>Since v5.7.2:</summary>

- Optimized the `FastTable` source code.
- For classes in the `javolution.util.primitive` package:
	+ Classes have been renamed to the format `Fast<Type>ArrayList`.
	+ The source code has been optimized.
	+ It will now throw an `ArrayIndexOutOfBoundsException` instead of `IndexOutOfBoundsException` when attempting to access an invalid index position.
	+ Renamed the `delete()` method to `removeElement()`.
	+ The ~~`removeRange()`~~ method has been removed.
</details>

<details>
	<summary>Since v5.7.1:</summary>

- Added the `javolution.util.primitive` package: A collection of implementations for `FastList` and `FastMap` for primitive data types.
- Reimplemented for J2ME; when compiled with `ant j2me`, it will default to using `midp_2.0.jar` (MIDP 2.0) and `cldc_1.1.jar` (CLDC 1.1) for the classpath.
</details>

<details>
	<summary>Since v5.7.0:</summary>

- Added the packages `javolution.util.concurrent` and `javolution.util.concurrent.locks` based on [Doug Lea's implementation](https://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html).
- The following classes have been renamed:
	+ `MutableList` → `FastAbstractList`.
	+ `SharedCollectionImpl` → `FastSharedCollection`.
	+ `UnmodifiableCollectionImpl` → `FastUnmodifiableCollection`.

> From this version onwards, there will no longer be separate branches for each version. If you need to download the source code, please download it from the [Releases](https://github.com/tumatanquang/javolution-extended/releases) page.
</details>

<details>
	<summary>Since v5.6.9:</summary>

- Undo class name `FastSequence` to `FastList`.
- The abstract class `FastList` has been renamed to `MutableList`.
</details>

<details>
	<summary>Since v5.6.8:</summary>

- The original `FastChain` has been replaced with `FastSequence`.
</details>

<details>
	<summary>Since v5.6.6:</summary>

- The original `FastList` has been replaced with `FastChain`.
</details>


## Suggestions for use:

* `ArrayList` can be replaced with `FastTable`.
* `LinkedList` can be replaced with `FastList`.
* To initialize a `FastTable` / `FastList`:

```java
FastTable table = new FastTable();
FastList list = new FastList();
// or:
FastCollection table = new FastTable();
FastCollection list = new FastList();
```

## How to build:

This project supports building with [Apache Ant™](https://ant.apache.org/bindownload.cgi) and [Apache Maven](https://maven.apache.org/download.cgi); please refer to the official websites for system requirements. However, to compile the source code and build the Javadoc, you will need Java 5 or Java 6, depending on your needs; see the [`build.xml`](https://github.com/tumatanquang/javolution-extended/blob/main/build.xml) file for details.
