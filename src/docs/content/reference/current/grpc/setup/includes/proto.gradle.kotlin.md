```gradle
plugins {
  idea
  kotlin("jvm")
  kotlin("plugin.allopen")

  id("com.google.protobuf") version "0.9.4"
}

dependencies {
  gatlingApi("com.google.protobuf:protobuf-kotlin:3.25.2")
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:3.25.2"
  }
  plugins {
    create("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:1.61.0"
    }
  }
  generateProtoTasks {
    ofSourceSet("gatling").forEach { task ->
      tasks.getByName("compileGatlingKotlin").dependsOn(task)
      task.builtins {
        maybeCreate("java") // Used by kotlin and already defined by default
        create("kotlin")
      }
      task.plugins {
        create("grpc")
      }
    }
  }
}
```

You will also need to add the generated files to the idea module and Gatling source sets:

```kotlin
var generatedSources = arrayOf(
  file("${protobuf.generatedFilesBaseDir}/gatling/java"),
  file("${protobuf.generatedFilesBaseDir}/gatling/kotlin"),
  file("${protobuf.generatedFilesBaseDir}/gatling/grpc")
)

idea {
  module {
    generatedSourceDirs.plusAssign(generatedSources)
  }
}

sourceSets.getByName("gatling") {
  java.srcDirs(generatedSources)
  kotlin.srcDirs(generatedSources)
}
```

Add your proto files in the `src/gatling/proto` directory.

Check the demo project for a full example:
[Gatling gRPC Kotlin demo with Gradle](https://github.com/gatling/gatling-grpc-demo/tree/main/kotlin/gradle).
