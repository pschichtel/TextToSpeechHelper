package tel.schich.ttshelper

import com.google.api.gax.rpc.ApiException
import com.google.cloud.texttospeech.v1.*

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.StandardOpenOption.{CREATE, DSYNC, SYNC, TRUNCATE_EXISTING, WRITE}
import java.nio.file.{Files, Path}
import java.util.Locale
import java.util.regex.Pattern
import io.circe.yaml.parser

object Main {

  def main(args: Array[String]): Unit = {
    Arguments.parse(args) match {
      case Some(arguments) =>
        val configString = new String(Files.readAllBytes(arguments.config), UTF_8)
        val config = parser.parse(configString)
          .flatMap(json => json.as[Config])
          .toTry
          .get

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

  private def synthesize(client: TextToSpeechClient, name: String, config: SynthesisSpec, outputDirectory: Path): Either[Throwable, Path] = {
    try {
      val request = buildRequest(config.input, config.language, config.voice, config.gender, config.encoding, config.speakingRate, config.variables.getOrElse(Map.empty))
      val response = client.synthesizeSpeech(request)

      val fileName = config.encoding match {
        case AudioEncoding.LINEAR16 => s"$name.wav"
        case AudioEncoding.MP3 => s"$name.mp3"
        case AudioEncoding.OGG_OPUS => s"$name.opus"
        case _ => throw new Exception("unsupported audio encoding!")
      }

      val outputPath = outputDirectory.resolve(fileName)
      println(s"Synthesized spec '$name' to '$outputPath'")

      Right(Files.write(outputPath, response.getAudioContent.toByteArray, WRITE, CREATE, SYNC, DSYNC, TRUNCATE_EXISTING))
    } catch {
      case e: ApiException =>
        System.err.println("Synthesis failed!")
        e.printStackTrace(System.err)
        Left(e)
    }
  }

  private def buildRequest(input: Input, language: Locale, voice: String, gender: SsmlVoiceGender, encoding: AudioEncoding, speakingRate: Double, variables: Map[String, String]): SynthesizeSpeechRequest = {
    val builder = SynthesizeSpeechRequest.newBuilder()

    builder.setInput(buildInput(input, variables))
    builder.setVoice(buildVoice(language, voice, gender))
    builder.setAudioConfig(buildConfig(encoding, speakingRate))

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

  private def buildConfig(encoding: AudioEncoding, speakingRate: Double): AudioConfig = {
    val builder = AudioConfig.newBuilder()
    builder.setAudioEncoding(encoding)
    builder.setSpeakingRate(speakingRate)
    builder.build()
  }

}
