```xml
<build>
  <extensions>
    <extension>
      <groupId>kr.motd.maven</groupId>
      <artifactId>os-maven-plugin</artifactId>
      <version>${os-maven-plugin.version}</version>
    </extension>
  </extensions>
  <plugins>
    <plugin>
      <groupId>org.xolstice.maven.plugins</groupId>
      <artifactId>protobuf-maven-plugin</artifactId>
      <version>${protobuf-maven-plugin.version}</version>
      <executions>
        <execution>
          <id>compile</id>
          <goals>
            <goal>test-compile</goal>
            <goal>test-compile-custom</goal>
          </goals>
          <configuration>
            <pluginId>grpc-java</pluginId>
            <pluginArtifact>io.grpc:protoc-gen-grpc-java:${protoc-gen-grpc-java.version}:exe:${os.detected.classifier}</pluginArtifact>
            <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

With the following properties:

```xml
<properties>
  <kotlin.version>1.9.10</kotlin.version>
  <protobuf.version>3.25.2</protobuf.version>
  <protobuf-maven-plugin.version>0.6.1</protobuf-maven-plugin.version>
  <protoc-gen-grpc-java.version>1.61.0</protoc-gen-grpc-java.version>
  <os-maven-plugin.version>1.7.1</os-maven-plugin.version>
</properties>
```

Add your proto files in the `src/test/proto` directory.

Check the demo project for a full example:
[Gatling gRPC Kotlin demo with Maven](https://github.com/gatling/gatling-grpc-demo/tree/main/kotlin/maven).
