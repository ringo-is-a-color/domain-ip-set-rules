package dird

import com.comcast.ip4s.Hostname
import dird.protobuf.domains.{Domain, GeoSiteList}

import java.io.FileInputStream
import java.net.URL
import scala.util.Using

type DomainRules = Map[String, Map[String, Seq[String]]]

object DomainRulesStore:
  private val geoSitesDataUrl = "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat"
  private val geoSitesLocalDataPath = "data/geosite.dat"
  def load(useLocalDataFile: Boolean): DomainRules =
    val geoSites =
      Using.resource(if useLocalDataFile then FileInputStream(geoSitesLocalDataPath) else URL(geoSitesDataUrl).openStream) {
        in => GeoSiteList.parseFrom(in)
      }
    toDomainRules(geoSites)

  private def toDomainRules(geoSites: GeoSiteList): DomainRules =
    val sites = geoSites.entry.map(site => site.countryCode.toLowerCase -> site.domain)
    val siteMap = sites.toMap
    assert(siteMap.size == sites.size, "the geosite data has duplicate tags")

    // TODO: use a strict version of `mapValues` in the future Scala version
    siteMap.view.mapValues {
      _.map { domain =>
        val `type` = toTypeInt(domain.`type`)
        `type` -> (`type` match
          case "regex" => domain.value.r.regex // check the regex is valid
          case _       => Hostname.fromString(domain.value).get.toString
        )
      }.groupMap(_._1)(_._2)
    }.toMap

  private def toTypeInt(`type`: Domain.Type): String = `type` match
    case Domain.Type.Full       => "full"
    case Domain.Type.RootDomain => "suffix"
    case Domain.Type.Plain      => "keyword"
    case Domain.Type.Regex      => "regex"
    case Domain.Type.Unrecognized(domainTypeNumber) =>
      throw new IllegalStateException(s"unrecognized domain type number '$domainTypeNumber")
