
semantic_repository
====================

SEE: [teamdigitale/daf](https://github.com/teamdigitale/daf) 

## instructions

1. local publish of dependencies

	1.2. virtuoso JDBC / RDF4J jar

	The dependencies for virtuoso integration are currently not yet published on the maven central, so they are linked using the convetional `lib` folder in the sbt project:

	```
	[semantic_repository]
	├───/lib
	│   ├───virtjdbc4_2.jar
	│   └───virt_rdf4j.jar
	```

	We could avoid this if/when the libraries will be published, or by publishing them on a private nexus.

2. compile / package

	```bash
	$ sbt clean package
	```

3. run

	```bash
	$ sbt run 
	```

4. (local) deploy

	```
	$ sbt clean dist
	$ unzip -o -d  target/universal/ target/universal/semantic_repository-0.1.0.zip
	$ target/universal/semantic_repository-0.1.0/bin/semantic-repository  -Dconfig.file=./conf/application.conf
	```

	**NOTE**: if the application crashed, the pid file whould be deleted before attempting re-run 
	
	```bash
	$ rm target/universal/semantic_repository-0.1.0/RUNNING_PID 
	```

5. release

	working draft: [0.0.1](https://github.com/seralf/semantic_repository/releases/tag/0.1.0)


* * *

### adapters for RDF4J collections

The object `it.almawave.kb.utils.RDF4JAdapters` contains some adapters which may be useful for working with RDF4J collections in a simpler way.

+ `StringContextAdapter` can be used for writing conversions like:
`val context:Resource = "http://graph".toIRI`
+ `StringContextListAdapter` it's the same on collections, and can be used for writing conversions like:
`val contexts:Array[Resource] = Array("http://graph").toIRIList`
+ `RepositoryResultIterator` and `TupleResultIterator` are useful for handling results from query as Scala collections, without having to write `while(...)` code.
+ `BindingSetMapAdapter` adds a `toMap` conversion method, useful for writings things like:
```
val bs: BindingSet = ...
val map: Map[String, Object] = bs.toMap()
```

### Try + transaction handling + error messages

It's possible to simplify the code used for interacting with the underling RDF4J Repository instance, focusing on the actual code, using the `RepositoryAction` construct like in the following example:

```scala
import it.almawave.kb.utils.TryHandlers._

...

implicit val logger = LoggerFactory.getLogger(this.getClass)

val repo:Repository = ...

// a method clearing all the triples!
def clear_all() {

	RepositoryAction(repo) { conn =>

	// some code using connection object, like for example:
	conn.clear()

	}(s"cannot clear all the triples for some reason!}")

}

```

**NOTE**:
+ the open/close connection actions are handled by the `RepositoryAction` itself 
+ the provided error message will be used internally both for logging on the chosen logger, and for handling Exception in the usual `Try/Failure` way.


* * *

## TODO

- [ ] move TryLog to FutureWithLog
- [ ] switch to new name conventions: `semantic_*`, merge into main daf.
	NOTE: consider using `git subtree` for the local fork
- [ ] publish `kb-core` (changing name conventions) on github / bitbucket or as sub-module
- [ ] add `kb-core` dependency on sbt - move core to external library
- [x] add `RDF4J` dependencies on sbt
- [x] add `virtoso` dependencies on sbt
- [x] add `virtoso` jar on sbt/lib
- [x] refactoring JUnit tests for engine part: memory
- [x] refactoring JUnit tests for engine part: virtuoso wrapper
- [ ] more test coverage for simple example HTTP requests (specs2)
- [x] ~~datapackage or similar? at the moment~~ a `.metadata` file is used for contexts
- [x] ~~creating a simple construct for dealing with transactions~~. Done: see `TryHandlers.RepositoryAction[X]`
- [ ] review repository wrapper code base
- [ ] review ontonethub wrapper code base
- [ ] review local filestorage code base
- [ ] review / refactor the response from services, using more meaningful data structures



