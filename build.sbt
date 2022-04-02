name := "TextToSpeechHelper"

version := "0.1"

scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "com.google.cloud" % "google-cloud-texttospeech" % "2.2.0",
  "net.jcazevedo" %% "moultingyaml" % "0.4.2",
  "com.github.scopt" %% "scopt" % "4.0.1",
)

mainClass := Some("tel.schich.ttshelper.Main")