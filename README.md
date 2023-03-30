
[ ![Codeship Status for pschaus/minicp](https://app.codeship.com/projects/c5b42a30-bb10-0134-c1e5-0a15df6d3688/status?branch=master)](https://app.codeship.com/projects/195547)

[![codecov](https://codecov.io/bb/pschaus/minicp/branch/master/graph/badge.svg?token=zAUOtKaB64)](https://codecov.io/bb/pschaus/minicp)


# README #

* This repository contains the source code from Augustin Delecluse, Pierre Schaus, and Pascal Van Hentenryck. Sequence variables for routing problems.
  *In 28th International Conference on Principles and Practice of Constraint Programming (CP
  2022)*, 2022.
* The jar executable can be found in the `out/artifacts` folder
* All jar executables have been built using java 17, but can be rebuilt using java 8.
* The code in this repository has been built from MiniCP
* MiniCP technical documentation, exercises etc 
can be found at www.minicp.org
* MiniCP is a Java project build with maven https://maven.apache.org



System Requirements
-------------------

* JDK:
 1.8 or above (this is to execute Maven - it still allows you to build against 1.3
 and prior JDK's).
* Memory:
 No minimum requirement.
* Disk:
 Approximately 10MB is required for the Maven installation itself. In addition to
 that, additional disk space will be used for your local Maven repository. The size
 of your local repository will vary depending on usage but expect at least 500MB.
* Operating System:
 - Windows: Windows 2000 or above.
 - Unix based systems (Linux, Solaris and Mac OS X) and others: No minimum requirement.

Installing Maven
----------------

1. Unpack the archive where you would like to store the binaries, e.g.:
 - Unix-based operating systems (Linux, Solaris and Mac OS X)
   ```
   tar zxvf apache-maven-3.x.y.tar.gz 
   ```  
 - Windows
   ```
   unzip apache-maven-3.x.y.zip
   ```
2. A directory called `apache-maven-3.x.y` will be created.
3. Add the bin directory to your PATH, e.g.:
 - Unix-based operating systems (Linux, Solaris and Mac OS X)
   ```
   export PATH=/usr/local/apache-maven-3.x.y/bin:$PATH
   ```
 - Windows
   ```
   set PATH="v:\program files\apache-maven-3.x.y\bin";%PATH%
   ```
4. Make sure `JAVA_HOME` is set to the location of your JDK
5. Run `mvn --version` to verify that it is correctly installed.


For complete documentation, see https://maven.apache.org/download.html#Installation

You need maven and java 17+. Once it's installed, run

```
mvn clean compile assembly:single
```

to build the executable. You can run it with

```
javar -jar target/maxicp-0.0.1.jar
```

The exact path to the executable might change depending on the naming / versioning. It is printed as the end of the compilation step

### Example

```
java -jar target/minicp-1.0.jar -f data/TSPTW/instances/Dumas/n200w40.002.txt
```

Gives an output similar to

```
data/PCTSP/size_20/data_10.txt | 49808.000 | 0.279 | suboptimal | 6 -> 14 -> 18 -> 5 -> 19 -> 16 -> 15 -> 13 -> 7 -> 17 -> 9 -> 3 -> 8 -> 0 -> 11 -> 12 -> 10 | 1 2 4 10 | 42 | OPT
```

Using an IntelliJ idea editor
--------------------------------------------------

We recommend IntelliJ idea https://www.jetbrains.com/idea/download

Simply do > `File | Open Project (Alt + F + O)` and specify the path to `pom.xml`
as explained here
https://blog.jetbrains.com/idea/2008/03/opening-maven-projects-is-easy-as-pie/

Content
-------------

```
./src/main/java/                        # the implementation of mini-cp
./src/main/java/minicp/engine/constraints/sequence/   # constraints on sequences
./src/main/java/minicp/engine/core/     # variables implementations (including SequenceVar)
./src/main/java/minicp/examples/darp/   # DARP models
./src/main/java/minicp/examples/ptp/    # PTP model
./src/main/java/minicp/examples/tsptw/  # TSPTW model
./src/test/java/                        # the test suite
./data/                                 # input instances
```

