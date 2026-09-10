# Javolution Extended

Based on the [Javolution 5.5.1](https://mvnrepository.com/artifact/javolution/javolution/5.5.1) source code, combined with [rawnet/javolution](https://github.com/rawnet/javolution) and [Doug Lea's `util.concurrent` package in Release 1.3.4](https://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html).

Since version **5.4**, upstream Javolution has marked most `public` classes and methods as `final`, preventing overriding. This project removes those restrictions and continues to develop and extend the library.

## Changelog:

See [CHANGELOG.md](CHANGELOG.md).

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