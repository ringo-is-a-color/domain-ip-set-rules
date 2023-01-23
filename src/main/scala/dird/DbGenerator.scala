package dird

import upickle.default.*

import java.nio.file.{Files, Path}
import java.sql.DriverManager
import scala.util.Using

val dbPath = "data/domain-ip-set-rules.db"

def generateDb(domainRules: DomainRules, ipSetRules: IpSetRules): Unit =
  Files.deleteIfExists(Path.of(dbPath))
  Using.resource(DriverManager.getConnection(s"jdbc:sqlite:$dbPath")) { conn =>
    val stmt = conn.createStatement()
    stmt.executeUpdate(domainTagsTableCreateSql)
    stmt.executeUpdate(domainTypesTableCreateSql)
    stmt.executeUpdate(domainsTableCreateSql)
    stmt.executeUpdate(domainTypesInsertSql)
    stmt.executeUpdate(ipSetTagsTableCreateSql)
    stmt.executeUpdate(ipSetTypesTableCreateSql)
    stmt.executeUpdate(ipSetTableCreateSql)
    stmt.executeUpdate(ipSetTypesInsertSql)

    conn.setAutoCommit(false)
    val domainTypesPrepareStmt = conn.prepareStatement(domainTagsInsertSqlTemplate)
    val domainsPrepareStmt = conn.prepareStatement(domainsInsertSqlTemplate)
    var totalInsertedDomainRows = 0
    domainRules.foreach { case (tag, domainsByType) =>
      domainTypesPrepareStmt.setString(1, tag)
      domainTypesPrepareStmt.addBatch()

      domainsByType.foreach { (`type`, domains) =>
        val domainJsonArray = write(domains)
        totalInsertedDomainRows += 1
        domainsPrepareStmt.setInt(1, toTypeInt(`type`))
        domainsPrepareStmt.setString(2, domainJsonArray)
        domainsPrepareStmt.setString(3, tag)
        domainsPrepareStmt.addBatch()
      }
    }

    val insertDomainTypesCount = domainTypesPrepareStmt.executeBatch().sum
    assert(
      insertDomainTypesCount == domainRules.size,
      s"inserted domain tags' count $insertDomainTypesCount doesn't not equal to domain tags' count ${domainRules.size} from data"
    )
    val insertDomainsCount = domainsPrepareStmt.executeBatch().sum
    val domainsCount = domainRules.map(_._2.map(_._1.length).sum).sum
    assert(
      insertDomainsCount == totalInsertedDomainRows,
      s"inserted domains' row count $insertDomainsCount doesn't not equal to domains' row count $totalInsertedDomainRows from data"
    )
    conn.commit()

    val ipSetTagsPrepareStmt = conn.prepareStatement(ipSetTagsInsertSqlTemplate)
    val ipSetPrepareStmt = conn.prepareStatement(ipSetInsertSqlTemplate)
    var totalInsertedIpSetRows = 0
    ipSetRules.foreach { case (tag, cidrs) =>
      ipSetTagsPrepareStmt.setString(1, tag)
      ipSetTagsPrepareStmt.addBatch()

      val cidrsByLength = cidrs.groupBy(_.length)
      cidrsByLength.foreach { case (length, sets) =>
        totalInsertedIpSetRows += 1
        length match
          case 5  => ipSetPrepareStmt.setInt(1, 0)
          case 17 => ipSetPrepareStmt.setInt(1, 1)
          case i  => throw new IllegalStateException(s"unrecognized CIDR length '$i")
        ipSetPrepareStmt.setBytes(2, sets.flatten.toArray)
        ipSetPrepareStmt.setString(3, tag)
        ipSetPrepareStmt.addBatch()
      }
    }

    val insertIpSetTagsCount = ipSetTagsPrepareStmt.executeBatch().sum
    assert(
      insertIpSetTagsCount == ipSetRules.size,
      s"inserted CIDR types' count $insertIpSetTagsCount doesn't not equal to CIDR types' count ${ipSetRules.size} from data"
    )
    val insertCidrsCount = ipSetPrepareStmt.executeBatch().sum
    assert(
      insertCidrsCount == totalInsertedIpSetRows,
      s"inserted CIDRs' row count $insertCidrsCount doesn't not equal to CIDRs' row count $totalInsertedIpSetRows from data"
    )
    conn.commit()
  }
  println("Create DB file successfully!")

  def toTypeInt(`type`: String): Int = `type` match
    case "full"    => 0
    case "suffix"  => 1
    case "keyword" => 2
    case "regex"   => 3
