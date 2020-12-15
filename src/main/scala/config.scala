import com.google.cloud.texttospeech.v1._
import net.jcazevedo.moultingyaml._

import java.util.Locale

sealed trait Input
final case class Text(text: String) extends Input
final case class Ssml(ssml: String) extends Input

case class Config(texts: Map[String, SynthesisSpec])
case class SynthesisSpec(input: Input, gender: SsmlVoiceGender, language: Locale, voice: String, encoding: AudioEncoding, variables: Option[Map[String, String]])

abstract class StringBasedYamlFormat[T] extends YamlFormat[T] {
  final override def read(yaml: YamlValue): T = yaml match {
    case YamlString(value) => readString(value)
    case _ => deserializationError("input must be a string!")
  }

  def readString(s: String): T

  final override def write(obj: T): YamlValue = YamlString(writeString(obj))

  def writeString(t: T): String
}

object Config extends DefaultYamlProtocol {
  implicit object InputFormat extends StringBasedYamlFormat[Input] {
    override def readString(s: String): Input =
      if (s.startsWith("<speak>")) Ssml(s)
      else Text(s)

    override def writeString(obj: Input): String = obj match {
      case Text(text) => text
      case Ssml(ssml) => ssml
    }
  }

  implicit object LocaleFormat extends StringBasedYamlFormat[Locale] {
    override def readString(s: String): Locale = Locale.forLanguageTag(s)
    override def writeString(t: Locale): String = t.toLanguageTag
  }

  implicit object GenderFormat extends StringBasedYamlFormat[SsmlVoiceGender] {
    override def readString(s: String): SsmlVoiceGender = SsmlVoiceGender.valueOf(s.toUpperCase)
    override def writeString(t: SsmlVoiceGender): String = t.name()
  }

  implicit object AudioEncodingFormat extends StringBasedYamlFormat[AudioEncoding] {
    override def readString(s: String): AudioEncoding = AudioEncoding.valueOf(s.toUpperCase)
    override def writeString(t: AudioEncoding): String = t.name()
  }

  implicit val synthesis: YamlFormat[SynthesisSpec] = yamlFormat6(SynthesisSpec.apply)
  implicit val config: YamlFormat[Config] = yamlFormat1(Config.apply)
}