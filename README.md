# shino

An auto mapper for scala [slick](https://github.com/slick/slick) base on [asuna](https://github.com/scalax/asuna). Type safe, type driven, no runtime reflection.

How to get it
-------------

Add dependency

```scala
resolvers += Resolver.bintrayRepo("djx314", "maven")
libraryDependencies += "net.scalax" %% "shino" % "0.0.2-SNAP20181008.1"
```

Can I use it in production?
-------------
Nope. Since the mapping rules in asuna is not stable.

Documentation
-------------

[slick formatter](./README_formatter.md)  
[slick reader](./README_reader.md)  
[slick writer](./README_writer.md)  
[slick sortBy](./README_sortby.md)  
[slick dynamic](./README_dynamic.md)  