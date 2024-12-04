name := "TextToSpeechHelper"

version := "0.1"

scalaVersion := "3.6.1"

libraryDependencies ++= Seq(
  "com.google.cloud" % "google-cloud-texttospeech" % "2.18.0",
  "io.circe" %% "circe-yaml" % "0.16.0",
  "com.github.scopt" %% "scopt" % "4.1.0",
)

scalacOptions ++= Seq("-unchecked", "-deprecation", "--explain")

mainClass := Some("tel.schich.ttshelper.Main")