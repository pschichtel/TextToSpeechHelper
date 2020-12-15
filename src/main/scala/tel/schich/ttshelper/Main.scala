package tel.schich.ttshelper

import com.google.cloud.texttospeech.v1._
import net.jcazevedo.moultingyaml._

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.StandardOpenOption.{CREATE, DSYNC, SYNC, TRUNCATE_EXISTING, WRITE}
import java.nio.file.{Files, Path}
import java.util.Locale
import java.util.regex.Pattern

object Main {

  def main(args: Array[String]): Unit = {

    Arguments.parse(args) match {
      case Some(arguments) =>
        val configString = new String(Files.readAllBytes(arguments.config), UTF_8)
        val config = configString.parseYaml.convertTo[Config]

        val client = TextToSpeechClient.create()

        val specs =
          if (arguments.specs.nonEmpty) arguments.specs
          else config.texts.keySet

        val unknownSpecs = specs.diff(config.texts.keySet)

        if (unknownSpecs.nonEmpty) {
          System.err.println(s"Cannot render unknown specs: ${unknownSpecs.mkString(", ")}")
          System.exit(1)
        }

        for (specName <- specs) {
          synthesize(client, specName, config.texts(specName), arguments.outputDirectory)
        }

      case None =>
        System.exit(1)
    }
  }

  private def synthesize(client: TextToSpeechClient, name: String, config: SynthesisSpec, outputDirectory: Path): Path = {
    val request = buildRequest(config.input, config.language, config.voice, config.gender, config.encoding, config.variables.getOrElse(Map.empty))
    val response = client.synthesizeSpeech(request)

    val fileName = config.encoding match {
      case AudioEncoding.LINEAR16 => s"$name.wav"
      case AudioEncoding.MP3 => s"$name.mp3"
      case AudioEncoding.OGG_OPUS => s"$name.opus"
      case _ => throw new Exception("unsupported audio encoding!")
    }

    val outputPath = outputDirectory.resolve(fileName)
    println(s"Synthesized spec '$name' to '$outputPath'")

    Files.write(outputPath, response.getAudioContent.toByteArray, WRITE, CREATE, SYNC, DSYNC, TRUNCATE_EXISTING)
  }

  private def buildRequest(input: Input, language: Locale, voice: String, gender: SsmlVoiceGender, encoding: AudioEncoding, variables: Map[String, String]): SynthesizeSpeechRequest = {
    val builder = SynthesizeSpeechRequest.newBuilder()

    builder.setInput(buildInput(input, variables))
    builder.setVoice(buildVoice(language, voice, gender))
    builder.setAudioConfig(buildConfig(encoding))

    builder.build()
  }

  private def buildInput(input: Input, variables: Map[String, String]): SynthesisInput = {
    val builder = SynthesisInput.newBuilder()
    input match {
      case Text(text) => builder.setText(replaceVariables(text, variables))
      case Ssml(ssml) => builder.setSsml(replaceVariables(ssml, variables))
    }

    builder.build()
  }

  private def replaceVariables(s: String, variables: Map[String, String]): String =
    if (variables.isEmpty) s
    else {
      val regex = variables.keys.map(Pattern.quote).mkString("\\{\\{(", "|", ")}}").r
      regex.replaceAllIn(s, m => variables(m.group(1)))
    }

  private def buildVoice(language: Locale, voice: String, gender: SsmlVoiceGender): VoiceSelectionParams = {
    val builder = VoiceSelectionParams.newBuilder()

    builder.setLanguageCode(language.toLanguageTag)
    builder.setName(voice)
    builder.setSsmlGender(gender)

    builder.build()
  }

  private def buildConfig(encoding: AudioEncoding): AudioConfig = {
    val builder = AudioConfig.newBuilder()
    builder.setAudioEncoding(encoding)
    builder.build()
  }

}
