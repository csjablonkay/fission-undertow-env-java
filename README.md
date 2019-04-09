# fission-undertow-env-java

Java undertow environment implementation for fission.

Sample
------

###Dependencies:
- [Java core module](https://github.com/csjablonkay/fission-undertow-java-core). This has the Function interface.
- [Java sample module](https://github.com/csjablonkay/fission-undertow-java-sample). This has the helloworld function implementation.

###Steps:
1., Create environment:
```bash
$ fission env create --name jvm --image csjablonkay/fission-undertow-jvm-env --version 2 --keeparchive --mincpu 100 --maxcpu 500 --minmemory 256 --maxmemory 1024
```

2., Create hello function:
```bash
#Checkout the core module.
$ git clone https://github.com/csjablonkay/fission-undertow-java-core.git
#Build and push the core module into your local maven repository.
$ mvn clean install -f fission-undertow-java-core
#Checkout the sample module.
$ git clone https://github.com/csjablonkay/fission-undertow-java-sample.git
#Build the sample module.
$ mvn clean install -f fission-undertow-java-sample
#Create hello function.
$ fission fn create --name hello --deploy fission-undertow-java-sample/target/undertow-java-sample-0.0.1-SNAPSHOT-jar-with-dependencies.jar --env jvm --entrypoint io.fission.undertow.HelloWorld
```

3., Create route for hello function:
```bash
$ fission route create --function hello --name hello --url /hello --method GET
```

4., Test hello function:
```bash
$ curl $FISSION_ROUTER/hello
Hello World...
```

