import com.amazonaws.regions.RegionUtils

name := "s3mock"

version := "0.2.4"

organization := "io.findify"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.11", "2.12.4")

val akkaVersion = "2.5.6"

licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))

homepage := Some(url("https://github.com/findify/s3mock"))

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  "com.github.pathikrit" %% "better-files" % "2.17.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
  "org.slf4j" % "slf4j-simple" % "1.8.0-beta1", // To send logging to standard out, so it can appear in the container's log
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.224",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test",
  "org.iq80.leveldb" % "leveldb" % "0.10", // See https://github.com/findify/s3mock/issues/83
  "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "0.14" % "test"
)

parallelExecution in Test := false

publishMavenStyle := true

// Fixes duplicate error on building docker image, caused by adding the slf4j-simple dependency above
assemblyMergeStrategy in assembly := {
  case "module-info.class" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
    <scm>
      <url>git@github.com:findify/s3mock.git</url>
      <connection>scm:git:git@github.com:findify/s3mock.git</connection>
    </scm>
    <developers>
      <developer>
        <id>romangrebennikov</id>
        <name>Roman Grebennikov</name>
        <url>http://www.dfdx.me</url>
      </developer>
    </developers>)

enablePlugins(DockerPlugin, EcrPlugin)

assemblyJarName in assembly := "s3mock.jar"
mainClass in assembly := Some("io.findify.s3mock.Main")
test in assembly := {}

dockerfile in docker := new Dockerfile {
  from("openjdk:8-alpine") // see https://github.com/findify/s3mock/issues/83
  expose(8001)
  add(assembly.value, "/app/s3mock.jar")
  entryPoint("sh", "-c", "java -Xmx128m $JAVA_ARGS -jar /app/s3mock.jar")
}

imageNames in docker := Seq(
  ImageName(s"uxforms/s3mock:${version.value.replaceAll("\\+", "_")}"),
  ImageName(s"uxforms/s3mock:latest")
)

region in Ecr := RegionUtils.getRegion("eu-west-2")

repositoryName in Ecr := name.value

localDockerImage in Ecr := s"uxforms/${name.value}" + ":" + version.value

repositoryTags in Ecr := Seq(version.value)

login in Ecr := ((login in Ecr) dependsOn (createRepository in Ecr)).value

push in Ecr := ((push in Ecr) dependsOn (docker in docker, login in Ecr)).value

/*enablePlugins(JavaAppPackaging)

maintainer in Docker := "S3mock"
packageSummary in Docker := "S3Mock"
packageDescription := "Mock Service For S3"
dockerUpdateLatest := true
dockerExposedPorts := Seq(8001)
*/