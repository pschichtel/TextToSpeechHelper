name := "TextToSpeechHelper"

version := "0.1"

scalaVersion := "2.13.4"

libraryDependencies ++= Seq(
  "com.google.cloud" % "google-cloud-texttospeech" % "1.2.5",
  "net.jcazevedo" %% "moultingyaml" % "0.4.2",
  "com.github.scopt" %% "scopt" % "4.0.0-RC2",
)