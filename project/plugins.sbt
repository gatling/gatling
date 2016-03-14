resolvers += Resolver.url("gatling", url("http://dl.bintray.com/content/gatling/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("io.gatling" % "gatling-build-plugin" % "1.8.11")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.6.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.2")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.3")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.4")

addMavenResolverPlugin
