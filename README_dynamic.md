Dynamic user guide
-------------

You can read and write your database dynamically use json. Shino provide two
samples reading and writing database use circe. You can
decide which columns should be read or written at runtime.

Here's the [code](https://github.com/scalax/shino/tree/master/src/test/scala/net/scalax/shino/test/samples/rmu).

In order to avoid redundant dependencies, shino can't add it to the implementations.
You can copy it to your source code and use it.