# ChessKt
A chess game implementation made using KotlinJS and Ktor

### Build instructions
To build and run the server use:
```shell
./gradlew run
```

To generate a .jar file use:
```shell
./gradlew shadowJar
```

By default, the server will run on 127.0.0.1:8080, you can change that by adding a host:port argument while running 
the server (i.e. `java -jar build.jar example.com:1337`)

