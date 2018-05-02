resolvers += Resolver.url("gatling", url("http://dl.bintray.com/content/gatling/sbt-plugins/"))(Resolver.ivyStylePatterns)
resolvers += Resolver.jcenterRepo

addSbtPlugin("io.gatling" % "gatling-build-plugin" % "2.1.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.3.3")
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.7.0")

