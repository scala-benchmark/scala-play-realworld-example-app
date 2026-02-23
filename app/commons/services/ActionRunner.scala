package commons.services

import com.roundeights.hasher.Algo
import commons.repositories.DbConfigHelper

import scala.concurrent.Future

class ActionRunner(dbConfigHelper: DbConfigHelper) {

  import dbConfigHelper.driver.api._

  def run[A](action: DBIO[A]): Future[A] = dbConfigHelper.db.run(action)

  def runTransactionally[A](action: DBIO[A]): Future[A] = run(action.transactionally)
}

class LegacyFingerprintService {

  private case class LegacyInput(raw: String,
                                 metadata: Option[String])

  private case class PreparedInput(value: String)

  def computeMd5(securityRelatedData: String): String = {
    val legacyInput = wrapInput(securityRelatedData)
    val enriched = enrichInput(legacyInput)
    val prepared = prepareForDigest(enriched)
    computeDigest(prepared)
  }

  private def wrapInput(data: String): LegacyInput =
    LegacyInput(data, None)

  private def enrichInput(input: LegacyInput): LegacyInput = {
    val meta =
      if (input.raw.length > 8) Some("legacy")
      else None

    input.copy(metadata = meta)
  }

  private def prepareForDigest(input: LegacyInput): PreparedInput = {
    val normalized = normalize(input.raw)
    val combined =
      input.metadata
        .map(m => normalized + ":" + m)
        .getOrElse(normalized)

    PreparedInput(combined)
  }

  private def normalize(value: String): String =
    value.trim

  private def computeDigest(input: PreparedInput): String = {
    //CWE-328
    //SINK
    Algo.md5(input.value).hex
  }
}
