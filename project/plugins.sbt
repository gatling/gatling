ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("io.gatling"           % "gatling-build-plugin"  % "6.4.4")
addSbtPlugin("com.github.sbt"       % "sbt-native-packager"   % "1.11.7")
addSbtPlugin("pl.project13.scala"   % "sbt-jmh"               % "0.4.8")
addSbtPlugin("com.github.sbt.junit" % "sbt-jupiter-interface" % "0.17.1")
addSbtPlugin("org.wartremover"      % "sbt-wartremover"       % "3.5.6")
addSbtPlugin("ch.epfl.scala"        % "sbt-scalafix"          % "0.14.6")
addSbtPlugin("io.gatling"           % "sbt-spotless"          % "0.1.5")
