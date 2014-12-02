import com.typesafe.sbt.SbtStartScript 

//import com.earldouglas.xsbtwebplugin.WebPlugin

name := "SPIRIT-News"

version := "1.3.7"

organization := "SPIRIT"

scalaVersion := "2.10.4"

resolvers ++= Seq("snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
                "releases" at "http://oss.sonatype.org/content/repositories/releases"
                )

resolvers += Resolver.sonatypeRepo("public")

seq(webSettings :_*)

seq(SbtStartScript.startScriptForClassesSettings: _*)

// net.virtualvoid.sbt.graph.Plugin.graphSettings // for sbt dependency-graph plugin

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature","-language:implicitConversions","-language:postfixOps")

javacOptions ++= Seq("-Djsse.enableSNIExtension=false")

libraryDependencies ++= {
  val liftVersion = "2.5"
  val dispatchVersion="0.11.2"
  Seq(
   "net.tanesha.recaptcha4j" % "recaptcha4j" % "0.0.7",
    "com.eed3si9n" %% "repatch-twitter-core" % "dispatch0.11.1_0.1.0" withSources,
    "net.databinder.dispatch" %% "dispatch-core" % dispatchVersion withSources,
    "net.liftmodules" %% "textile_2.5" % "1.3" % "compile->default" withSources,
    "net.liftweb" % "lift-markdown_2.10" % "2.6-RC1" % "compile->default" withSources,
    "net.liftmodules" %% "widgets_2.5" % "1.3" % "compile->default" withSources,
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default" withSources,
    "net.liftweb" %% "lift-mapper" % liftVersion % "compile->default" withSources,
    "net.liftweb" %% "lift-mongodb" % liftVersion % "compile->default" withSources,
    "net.liftweb" %% "lift-mongodb-record" % liftVersion % "compile->default" withSources,
    "net.liftweb" %% "lift-record" % liftVersion % "compile->default" withSources,
    "net.liftweb" %% "lift-json" % liftVersion % "compile->default" withSources,
    "junit" % "junit" % "4.5" % "test->default",
    "org.scala-tools.testing" %% "specs" % "1.6.9" % "test->default",
    "org.mockito" % "mockito-all" % "1.8.0" % "test",
    "org.mortbay.jetty" % "jetty" % "6.1.26" % "test->default",
    "ch.qos.logback" % "logback-classic" % "1.0.11",
    "org.eclipse.jetty" % "jetty-webapp" % "8.1.10.v20130312" % "compile,container,test",
    "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container,test" artifacts Artifact("javax.servlet", "jar", "jar"),
    "org.eclipse.jetty" % "jetty-server" % "7.6.14.v20131031",
    "javax.mail" % "mail" % "1.4.7",
    "javax.servlet" % "servlet-api" % "2.5" % "provided",
    "org.apache.commons" % "commons-lang3" % "3.3.2",
    "org.jsoup" % "jsoup" % "1.8.1",
    "org.scala-stm" %% "scala-stm" % "0.7"
  )
}

    //lazy val root = (project in file(".")).addPlugins(SbtWeb)

    buildInfoSettings

    sourceGenerators in Compile <+= buildInfo

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)

    buildInfoPackage := "org.unsane.spirit.news.model"
