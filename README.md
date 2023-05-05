
[ ![Codeship Status for pschaus/minicp](https://app.codeship.com/projects/c5b42a30-bb10-0134-c1e5-0a15df6d3688/status?branch=master)](https://app.codeship.com/projects/195547)

[![codecov](https://codecov.io/bb/pschaus/minicp/branch/master/graph/badge.svg?token=zAUOtKaB64)](https://codecov.io/bb/pschaus/minicp)


# README #

* This repository contains the source code of SEQUOIA, an algorithm providing initial solutions to a TSPTW instance.
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
java -jar target/minicp-1.0.jar 
```

The exact path to the executable might change depending on the naming / versioning. It is printed as the end of the compilation step

### Example

```
java -jar target/minicp-1.0.jar -f data/TSPTW/instances/Dumas/n200w40.002.txt
```

Gives an output similar to

```
Dumas/n200w40.002.txt |   sequence |       open |    1084.00 |       0.42 | 77 173 83 85 43 65 56 38 1 25 128 174 91 15 148 68 40 198 34 184 102 151 140 61 195 108 161 4 82 67 20 35 110 145 167 119 120 196 171 39 129 199 53 21 158 12 50 44 147 54 93 92 124 31 14 118 7 64 66 186 104 10 111 178 28 100 146 5 99 130 48 9 70 123 79 23 69 125 86 176 159 138 115 42 60 187 17 116 36 190 194 131 109 76 26 166 114 160 139 94 71 164 106 29 122 175 90 153 81 117 137 89 84 63 2 52 80 72 57 150 19 143 180 32 113 192 152 6 74 97 132 127 142 182 18 126 169 78 105 95 157 96 41 75 3 58 49 88 133 73 55 87 46 11 16 135 189 134 181 168 33 197 37 30 155 103 45 165 172 62 156 8 162 101 59 112 188 179 22 107 24 154 170 185 121 136 13 144 193 191 163 183 98 51 47 27 177 200 149 141
```

showing 
- the instance
- the name of the underlying technology
- a string indicating if the traveled distance is optimal (close) or not proven optimal (open). SEQUOIA only produces open solutions with respect to the traveled distance.
- the traveled distance
- the run time in seconds for solving the instance
- the ordering of customers in the solution. 

### Options

Several options can be given as input:

```
-f filename     path to the instance to solve
-v verbosity    controls the verbosity level expressed as an int: the higher, the more information printed. 
                Use 1 to get more information about the iterations of the underlying algorithm.
-r seed         provides a seed for random number generation (default uses a fixed constant seed)
-t timeout      constrols the timeout, in seconds
-m method       where method is either "greedy", providing a path that may not visit all nodes, 
                or "satisfy", providing a path visiting all the nodes 
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
./src/main/java/                                        # the implementation of mini-cp
./src/main/java/minicp/engine/constraints/sequence/     # constraints on sequences
./src/main/java/minicp/engine/core/                     # variables implementations (including SequenceVar)
./src/main/java/minicp/examples/tsptw/                  # TSPTW model
./src/test/java/                                        # the test suite
./data/                                                 # input instances
```

