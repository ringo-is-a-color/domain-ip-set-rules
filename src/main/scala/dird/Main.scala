package dird

import java.nio.file.Files
import java.nio.file.Path

// TODO: use newMain in the future Scala version
@main def generateRuleFiles(useLocalDataFile: Boolean): Unit =
  Files.createDirectories(Path.of("data"))
  val domainRules = DomainRulesStore.load(useLocalDataFile)
  val ipSetRules = IpSetRulesStore.load(useLocalDataFile)
  generateCborFiles(domainRules, ipSetRules)
  generateDb(domainRules, ipSetRules)
