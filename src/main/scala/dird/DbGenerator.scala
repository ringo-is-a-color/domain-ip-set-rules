package dird

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
    stmt.executeUpdate(ipSetTableCreateSql)
    stmt.executeUpdate(ipSetTagsTableCreateSql)

    conn.setAutoCommit(false)
    val domainTypesPrepareStmt = conn.prepareStatement(domainTagsInsertSqlTemplate)
    val domainsPrepareStmt = conn.prepareStatement(domainsInsertSqlTemplate)
    domainRules.foreach { case (tag, domainsByType) =>
      domainTypesPrepareStmt.setString(1, tag)
      domainTypesPrepareStmt.addBatch()

      domainsByType.foreach { (`type`, domains) =>
        domains.foreach { domain =>
          domainsPrepareStmt.setInt(1, toTypeInt(`type`))
          domainsPrepareStmt.setString(2, domain)
          domainsPrepareStmt.setString(3, tag)
          domainsPrepareStmt.addBatch()
        }
      }
    }

    val insertDomainTypesCount = domainTypesPrepareStmt.executeBatch().sum
    assert(
      insertDomainTypesCount == domainRules.size,
      s"inserted domain tags' count $insertDomainTypesCount doesn't not equal to domain tags' count ${domainRules.size} from data"
    )
    val insertDomainsCount = domainsPrepareStmt.executeBatch().sum
    val domainsCount = domainRules.map(_._2.map(_._2.length).sum).sum
    assert(
      insertDomainsCount == domainsCount,
      s"inserted domains' count $insertDomainsCount doesn't not equal to domains' count $domainsCount from data"
    )
    conn.commit()

    val ipSetTagsPrepareStmt = conn.prepareStatement(ipSetTagsInsertSqlTemplate)
    val ipSetPrepareStmt = conn.prepareStatement(ipSetInsertSqlTemplate)
    ipSetRules.foreach { case (tag, cidrs) =>
      ipSetTagsPrepareStmt.setString(1, tag)
      ipSetTagsPrepareStmt.addBatch()

      cidrs.foreach { cidrBytes =>
        ipSetPrepareStmt.setBytes(1, cidrBytes)
        ipSetPrepareStmt.setString(2, tag)
        ipSetPrepareStmt.addBatch()
      }
    }

    val insertIpSetTagsCount = ipSetTagsPrepareStmt.executeBatch().sum
    assert(
      insertIpSetTagsCount == ipSetRules.size,
      s"inserted CIDR types' count $insertIpSetTagsCount doesn't not equal to CIDR types' count ${ipSetRules.size} from data"
    )
    val insertCidrsCount = ipSetPrepareStmt.executeBatch().sum
    val cidrsCount = ipSetRules.map(_._2.length).sum
    assert(
      insertCidrsCount == cidrsCount,
      s"inserted CIDRs' count $insertCidrsCount doesn't not equal to CIDRs' count $cidrsCount from data"
    )
    conn.commit()
  }
  println("Create DB file successfully!")

  def toTypeInt(`type`: String): Int = `type` match
    case "full"    => 0
    case "suffix"  => 1
    case "keyword" => 2
    case "regex"   => 3
