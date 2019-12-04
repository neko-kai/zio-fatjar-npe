Repro for an NPE when running a fat jar produced by `sbt assembly`.

### To reproduce:

1. Clone the repo and run the following:
```
> sbt assembly && scala target/scala-2.13/zio-fatjar-npe-assembly-0.1.jar
```
2. The following NPE will be printed:
```
Fiber:Id(1575462458707,0) was spawned by: <empty trace>
Fiber failed.
An unchecked error was produced.
java.lang.NullPointerException
	at zio.internal.stacktracer.impl.AkkaLineNumbers$.getStreamForClass(AkkaLineNumbers.scala:188)
	at zio.internal.stacktracer.impl.AkkaLineNumbers$.$anonfun$apply$1(AkkaLineNumbers.scala:63)
	at scala.Option.orElse(Option.scala:477)
	at zio.internal.stacktracer.impl.AkkaLineNumbers$.apply(AkkaLineNumbers.scala:63)
	at zio.internal.stacktracer.impl.AkkaLineNumbersTracer.traceLocation(AkkaLineNumbersTracer.scala:30)
	at zio.internal.stacktracer.Tracer$$anon$1.traceLocation(Tracer.scala:35)
	at zio.internal.FiberContext.traceLocation(FiberContext.scala:106)
	at zio.internal.FiberContext.pushContinuation(FiberContext.scala:113)
	at zio.internal.FiberContext.evaluateNow(FiberContext.scala:370)
	at zio.internal.FiberContext.$anonfun$fork$2(FiberContext.scala:655)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
	at java.base/java.lang.Thread.run(Thread.java:834)
  ...
```

This does not happen when ran normally from sbt (`sbt run`) or when executed with `java -jar target/scala-2.13/zio-fatjar-npe-assembly-0.1.jar`

To repro from the command line on my machine, `scala` expands to the following:
```
java -Xmx256M -Xms32M -Xbootclasspath/a:/usr/local/Cellar/scala/2.13.1/libexec/lib/jansi-1.12.jar:/usr/local/Cellar/scala/2.13.1/libexec/lib/jline-2.14.6.jar:/usr/local/Cellar/scala/2.13.1/libexec/lib/scala-compiler.jar:/usr/local/Cellar/scala/2.13.1/libexec/lib/scala-library.jar:/usr/local/Cellar/scala/2.13.1/libexec/lib/scala-reflect.jar:/usr/local/Cellar/scala/2.13.1/libexec/lib/scalap-2.13.1.jar -Dscala.usejavacp=true scala.tools.nsc.MainGenericRunner target/scala-2.13/zio-fatjar-npe-assembly-0.1.jar
```
Running the above command will cause the NPE as well (equivalent to running `scala`).

### Analysis

The NPE is caused when using `andThen` in my original code:
```
getStrLn.map(parse _ andThen (_.toString))
```

The offending line in `AkkaLineNumbers` is [here](https://github.com/zio/zio/blob/4c081ae1835b5c60a7fd14227a6da905d918d517/stacktracer/jvm/src/main/scala/zio/internal/stacktracer/impl/AkkaLineNumbers.scala#L184-L190):
```scala
private[this] def getStreamForClass(c: Class[_]): Option[(InputStream, String, None.type)] = {
  val name     = c.getName
  val resource = name.replace('.', '/') + ".class"
  val cl       = c.getClassLoader                          <-- getClassLoader returns null!
  val r        = cl.getResourceAsStream(resource)
  ...
```
It seems that when running with a fat jar, I get an extra class, `class scala.Function1$$Lambda$137/0x00000008001da040`, which doesn't happen when running normally. Seems that this class does not return a class loader.

If I rewrite my code not to use `andThen` - the NPE does not occur.
