import scopt.{OptionParser, Read}

import java.nio.file.{Path, Paths}

case class Arguments(config: Path, only: Option[String], outputDirectory: Path)

object Arguments {
  implicit val pathRead: Read[Path] = Read.reads(Paths.get(_))

  val parser: OptionParser[Arguments] = new OptionParser[Arguments]("text-to-speech-helper") {

    arg[Path]("config").required().valueName("<path>").text("The path to the configuration file (Yaml).").action { (path, config) =>
      config.copy(config = path)
    }

    opt[String]("only").optional().valueName("<name>").text("The synthesis spec to render.").action { (only, config) =>
      config.copy(only = Some(only))
    }

    opt[Path]("out-dir").optional().valueName("<path>").text("The path to output the audio files to.").action { (path, config) =>
      config.copy(outputDirectory = path)
    }
  }

  def parse(args: Seq[String]): Option[Arguments] = parser.parse(args, Arguments(null, None, Paths.get(".")))
}