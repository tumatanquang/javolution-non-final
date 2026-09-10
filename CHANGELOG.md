<details open>
	<summary>Since v7.0.0: Breaking Changes & Enhancements</summary>

- **With `FastTable`**:
	+ Removed constructor-based `rejectNulls` configuration; `FastTable` now always accepts `null` elements.
	+ Added `ensureCapacity(int)` method.
	+ Fixed `ensureCapacity()`, `setSize()`, and `getCapacity()` for `unmodifiable()` and `shared()` views.
	+ Fixed relative iterator indices for sub-tables.
	+ `FastTable.addAll(null)` and `FastList.addAll(null)` now return `false` instead of throwing `NullPointerException`.
- **With `FastList`**:
	+ Fixed size synchronization for nested sub-lists and their iterators.
- **With primitive collections (`javolution.util.primitive`)**:
	+ Added fast paths for empty sets across all 8 `Fast<Type>Set` implementations.
- Migrated release history from `README.md` to `CHANGELOG.md`.
- Updated project coordinate to `7.0.0` in `build.xml`, `pom.xml`, and CI/CD workflows.
</details>

<details>
	<summary>Since v6.1.0: Minor Changes & Enhancements</summary>

- **Math & Bitwise Manipulation Backport (`MathLib`)**:
	+ Added bit-manipulation methods (`highestOneBit`, `lowestOneBit`, `numberOfLeadingZeros`, `numberOfTrailingZeros`, `bitCount`) for J2ME CLDC 1.1 and Java 1.4+.
	+ Automatically transformed to native `Integer.*` / `Long.*` calls on Java 1.5+ during build.
- **Enhanced `trimToSize()`**:
	+ Enabled instant primary sub-block memory reclamation ($\le 1024$) across `FastTable` and all 8 `Fast<Type>Table` classes on all supported runtimes.
- **RTSJ & Scoped Memory Safety**:
	+ Updated `increaseCapacity()` in `Fast<Type>Table` and `rehash()` in `Fast<Type>Set` to execute in `MemoryArea.getMemoryArea(this).executeInArea(...)`.
- **Primitive Collections Fixes**:
	+ Fixed `size()` polymorphic dispatch and `equals(Object)` symmetry for `unmodifiable()` and `shared()` views of primitive tables and sets.
- **Queue / Deque & Utility Refinements**:
	+ Refined Queue and Deque contracts across `FastTable`, `FastList`, and `Fast<Type>Table`.
	+ Standardized primitive `Math.min`/`Math.max` usage across core utilities.
- Updated project coordinate to `6.1.0` in `build.xml`, `pom.xml`, and CI/CD workflows.
</details>

<details>
	<summary>Since v6.0.0: Major Changes</summary>

- **Redesigned Primitive Collections (`Fast<Type>Table` & `Fast<Type>Set`)**:
	+ Replaced 1D-array backing in primitive lists with a 2-tier segmented array architecture (`_low` + `_high[block][1024]`), matching `FastTable`.
	+ Renamed `Fast<Type>ArrayList` to `Fast<Type>Table` and `FastPrimitiveList` to `FastPrimitiveCollection` (legacy classes removed).
	+ Introduced 8 high-performance primitive sets (`Fast<Type>Set`) with linear probing and zero-boxing overhead.
	+ Added in-place primitive `sort()`, instant memory reclamation via `trimToSize()`, and `unmodifiable()` / `shared()` views.
- **Queue, Deque & Stack Support**:
	+ `FastList` and `FastTable` now implement `Deque` and `Queue` across all target runtimes.
	+ Added primitive stack and queue operations (`push`, `pop`, `peek`, `offer`, `poll`, etc.) to all 8 `Fast<Type>Table` classes.
- **Core Utilities & Math Improvements**:
	+ Fixed operator precedence in `FastBitSet.length()` and optimized bit search methods.
	+ Optimized hyperbolic functions, logarithms, and exponentials in `MathLib`.
- **Test Suite & Tooling**:
	+ Consolidated test suites under `AllTestSuite` (run via `ant -Dtest.runtime=<runtime> test-all`).
	+ Added Linux/Unix interactive launcher [`compile.sh`](compile.sh).
	+ Updated project coordinate to `6.0.0`.
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
	+ Constructor-based `rejectNulls` configuration was introduced in this version and later removed in v7.0.0.
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