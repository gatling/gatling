ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("io.gatling"         % "gatling-build-plugin"  % "5.2.0")
addSbtPlugin("com.github.sbt"     % "sbt-native-packager"   % "1.9.16")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"               % "0.4.5")
addSbtPlugin("net.aichler"        % "sbt-jupiter-interface" % "0.11.1")
addSbtPlugin("com.github.sbt"     % "sbt-site"              % "1.5.0")
addSbtPlugin("org.wartremover"    % "sbt-wartremover"       % "3.1.3")
addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"          % "0.11.0")
addSbtPlugin("io.gatling"         % "kotlin-plugin"         % "2.0.1")
addSbtPlugin("net.moznion.sbt"    % "sbt-spotless"          % "0.1.3")
