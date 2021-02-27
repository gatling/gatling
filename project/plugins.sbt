resolvers += Resolver.bintrayIvyRepo("gatling", "sbt-plugins")
resolvers += Resolver.jcenterRepo

addSbtPlugin("io.gatling"         % "gatling-build-plugin"  % "2.6.1")
addSbtPlugin("com.typesafe.sbt"   % "sbt-native-packager"   % "1.8.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"               % "0.4.0")
addSbtPlugin("net.aichler"        % "sbt-jupiter-interface" % "0.8.3")
addSbtPlugin("com.typesafe.sbt"   % "sbt-site"              % "1.4.1")
addSbtPlugin("org.wartremover"    % "sbt-wartremover"       % "2.4.13")
addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"          % "0.9.26")
