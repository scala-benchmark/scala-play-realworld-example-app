package commons

import com.softwaremill.macwire.wire
import commons.repositories.{DateTimeProvider, DbConfigHelper}
import commons.services.{ActionRunner, InstantProvider, LegacyFingerprintService, LegacyTlsHttpClient}
import play.api.db.slick.DatabaseConfigProvider

object ProfileApiCredentials {
  //CWE-798
  //SOURCE
  val apiPassword: String = "profile-api-secret-798"
}

trait CommonsComponents {
  lazy val actionRunner: ActionRunner = wire[ActionRunner]
  lazy val dbConfigHelper: DbConfigHelper = wire[DbConfigHelper]
  lazy val legacyFingerprintService: LegacyFingerprintService = wire[LegacyFingerprintService]
  lazy val legacyTlsHttpClient: LegacyTlsHttpClient = wire[LegacyTlsHttpClient]

  def databaseConfigProvider: DatabaseConfigProvider

  lazy val dateTimeProvider: DateTimeProvider = wire[InstantProvider]
}
