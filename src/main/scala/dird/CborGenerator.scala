package dird

import io.bullet.borer.{Cbor, Encoder}

import java.nio.file.{Files, Path}
// https://www.rfc-editor.org/rfc/rfc4180#section-2

val csvLineSeparator = "\r\n"

def generateCborFiles(domainRules: DomainRules, ipSetRules: IpSetRules): Unit =
  writeToFile("data/domain-rules.cbor", domainRules)
  writeToFile("data/ip-set-rules.cbor", ipSetRules)

  writeStringToFile("data/domain-tag-names.csv", domainRules.keySet.mkString(csvLineSeparator))
  writeStringToFile("data/ip-set-tag-names.csv", ipSetRules.keySet.mkString(csvLineSeparator))

  println("Create CBOR files successfully!")

  def writeToFile[T: Encoder](path: String, t: T) =
    Files.write(Path.of(path), Cbor.encode(t).toByteArray)

  def writeStringToFile(path: String, str: String) =
    Files.writeString(Path.of(path), str)
