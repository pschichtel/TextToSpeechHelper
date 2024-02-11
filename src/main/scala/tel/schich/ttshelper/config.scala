package tel.schich.ttshelper

import com.google.cloud.texttospeech.v1.*
import io.circe.Decoder.Result
import io.circe.DecodingFailure.Reason
import io.circe.{Codec, Decoder, DecodingFailure, Encoder, HCursor, Json}
import io.circe.derivation.{Configuration, ConfiguredCodec}
import io.circe.syntax.EncoderOps

import java.util.Locale
import scala.util.{Failure, Success, Try}

sealed trait Input
final case class Text(text: String) extends Input
final case class Ssml(ssml: String) extends Input

given Configuration = Configuration.default.withDefaults

abstract class SimpleStringBackedCodec[T](private val encode: T => String, private val decode: String => T) extends Codec[T] {
  override def apply(obj: T): Json = encode(obj).asJson

  override def apply(c: HCursor): Result[T] = c.as[String].flatMap { s =>
    Try(decode(s)) match
      case Success(value) =>
        Right(value)
      case Failure(exception) =>
        val reason = Reason.CustomReason(s"Unknown enum entry $s!")
        Left(DecodingFailure(reason, List.empty))
  }
}
abstract class JavaEnumCodec[T <: Enum[T]](private val ctor: String => T) extends SimpleStringBackedCodec[T](o => o.name(), s => ctor(s.toUpperCase)) {}

implicit object SsmlVoiceGenderCodec extends JavaEnumCodec[SsmlVoiceGender](SsmlVoiceGender.valueOf)
implicit object AudioEncodingCodec extends JavaEnumCodec[AudioEncoding](AudioEncoding.valueOf)
implicit object LocaleCodec extends SimpleStringBackedCodec[Locale](l => l.toLanguageTag, Locale.forLanguageTag)
implicit object InputFormatCodec extends Codec[Input] {
  override def apply(obj: Input): Json = obj match {
    case Text(text) => text.asJson
    case Ssml(ssml) => ssml.asJson
  }

  override def apply(c: HCursor): Result[Input] = {
    c.as[String].map { s =>
      if (s.startsWith("<speak>")) Ssml(s)
      else Text(s)
    }
  }
}

case class Config(texts: Map[String, SynthesisSpec]) derives ConfiguredCodec
case class SynthesisSpec(input: Input,
                         gender: SsmlVoiceGender,
                         language: Locale,
                         voice: String,
                         encoding: AudioEncoding,
                         speakingRate: Double = 1.0,
                         variables: Option[Map[String, String]]) derives ConfiguredCodec
