```gradle
plugins {
  id("idea")
  id("java")
  id("com.google.protobuf") version "0.9.4"
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:3.25.2"
  }
  plugins {
    grpc {
      artifact = "io.grpc:protoc-gen-grpc-java:1.61.0"
    }
  }
  generateProtoTasks {
    ofSourceSet("gatling").forEach { task ->
      compileGatlingJava.dependsOn(task)
      task.plugins {
        grpc {}
      }
    }
  }
}
```

You will also need to add the generated files to the idea module and Gatling source sets:

```gradle
var generatedSources = [
  file("${protobuf.generatedFilesBaseDir}/gatling/java"),
  file("${protobuf.generatedFilesBaseDir}/gatling/grpc")
]

idea {
  module {
    generatedSources.forEach { generatedSourceDirs += it }
  }
}

sourceSets {
  gatling {
    java {
      generatedSources.forEach { srcDirs += it }
    }
  }
}
```

Add your proto files in the `src/gatling/proto` directory.

Check the demo project for a full example:
[Gatling gRPC Java demo with Gradle](https://github.com/gatling/gatling-grpc-demo/tree/main/java/gradle).
