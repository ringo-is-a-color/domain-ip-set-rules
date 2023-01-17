package dird

import com.comcast.ip4s.{IpAddress, Ipv4Address}
import dird.protobuf.cidrs.GeoIPList

import java.io.FileInputStream
import java.net.URL
import scala.util.Using

type IpSetRules = Map[String, Seq[Array[Byte]]]

object IpSetRulesStore:
  private val geoIpDataUrl = "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geoip.dat"
  private val geoIpLocalDataPath = "data/geoip.dat"
  def load(useLocalDataFile: Boolean): IpSetRules =
    val geoIPList = Using.resource(if useLocalDataFile then FileInputStream(geoIpLocalDataPath) else URL(geoIpDataUrl).openStream) {
      in => GeoIPList.parseFrom(in)
    }
    toIpSetRules(geoIPList)

  private def toIpSetRules(geoIPList: GeoIPList): IpSetRules =
    val geoIpSetByTag = geoIPList.entry.map { geoIp =>
      // the geoip data doesn't use this field but we still check it
      // to make sure this behavior is still the same in the future
      assert(!geoIp.inverseMatch)
      geoIp.countryCode.toLowerCase -> geoIp.cidr
    }
    val geoIpSetMap = geoIpSetByTag.toMap
    assert(geoIpSetMap.size == geoIpSetByTag.size, "the geoip data has duplicate tags")

    // TODO: use a strict version of `mapValues` in the future Scala version
    geoIpSetMap.view.mapValues {
      _.map { cidr =>
        // the geoip data doesn't use this field but we still check it
        // to make sure this behavior is still the same in the future
        assert(cidr.ipAddr.isEmpty)

        val ipBytes = cidr.ip.toByteArray
        val ip = IpAddress.fromBytes(ipBytes).get
        val maxPrefix = ip match
          case _: Ipv4Address => 32
          case _              => 128

        assert(cidr.prefix <= maxPrefix, s"the CIDR's prefix ${cidr.prefix} is larger than $maxPrefix")
        ipBytes :+ cidr.prefix.toByte
      }
    }.toMap
