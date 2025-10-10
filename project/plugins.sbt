ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("io.gatling"           % "gatling-build-plugin"  % "6.4.2")
addSbtPlugin("com.github.sbt"       % "sbt-native-packager"   % "1.11.4")
addSbtPlugin("pl.project13.scala"   % "sbt-jmh"               % "0.4.8")
addSbtPlugin("com.github.sbt.junit" % "sbt-jupiter-interface" % "0.15.1")
addSbtPlugin("org.wartremover"      % "sbt-wartremover"       % "3.4.1")
addSbtPlugin("ch.epfl.scala"        % "sbt-scalafix"          % "0.14.4")
addSbtPlugin("net.moznion.sbt"      % "sbt-spotless"          % "0.1.3")
