resolvers += Resolver.bintrayIvyRepo("gatling", "sbt-plugins")
resolvers += Resolver.jcenterRepo

addSbtPlugin("io.gatling"         % "gatling-build-plugin"  % "2.5.3")
addSbtPlugin("com.typesafe.sbt"   % "sbt-native-packager"   % "1.7.3")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"               % "0.3.7")
addSbtPlugin("net.aichler"        % "sbt-jupiter-interface" % "0.7.0")
addSbtPlugin("com.typesafe.sbt"   % "sbt-site"              % "1.3.3")
addSbtPlugin("org.wartremover"    % "sbt-wartremover"       % "2.4.9")
addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"          % "0.9.16")
