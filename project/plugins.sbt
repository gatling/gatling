resolvers += Resolver.url("scoverage",url("http://dl.bintray.com/sksamuel/sbt-plugins"))(Resolver.ivyStylePatterns)

resolvers += Resolver.url("gatling", url("http://dl.bintray.com/content/gatling/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("io.gatling" % "gatling-build-plugin" % "1.8.5")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.6.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.2")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.3")

addMavenResolverPlugin
