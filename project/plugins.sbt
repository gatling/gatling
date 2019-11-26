resolvers += Resolver.bintrayIvyRepo("gatling", "sbt-plugins")
resolvers += Resolver.jcenterRepo

addSbtPlugin("io.gatling"         % "gatling-build-plugin"  % "2.4.3")
addSbtPlugin("com.typesafe.sbt"   % "sbt-native-packager"   % "1.3.11")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"               % "0.3.3")
addSbtPlugin("net.aichler"        % "sbt-jupiter-interface" % "0.7.0")
addSbtPlugin("com.typesafe.sbt"   % "sbt-site"              % "1.3.2")
addSbtPlugin("org.wartremover"    % "sbt-wartremover"       % "2.4.3")
