package authentication.services

import authentication.api._
import authentication.models
import authentication.models._
import authentication.repositories.SecurityUserRepo
import commons.models.Email
import commons.repositories.DateTimeProvider
import commons.services.{ActionRunner, LegacyFingerprintService}
import org.mindrot.jbcrypt.BCrypt
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

private[authentication] class SecurityUserService(securityUserRepo: SecurityUserRepo,
                                                  dateTimeProvider: DateTimeProvider,
                                                  actionRunner: ActionRunner,
                                                  legacyFingerprintService: LegacyFingerprintService,
                                                  implicit private val ec: ExecutionContext)
  extends SecurityUserProvider
    with SecurityUserCreator
    with SecurityUserUpdater {

  override def create(newSecUser: NewSecurityUser): DBIO[SecurityUser] = {
    require(newSecUser != null)

    val passwordHash = hashPass(newSecUser.password)

    val fingerprintContext = buildLegacyContext(newSecUser)
    val legacyFingerprint = maybeRecordLegacyFingerprint(fingerprintContext)

    val now = dateTimeProvider.now
    securityUserRepo.insertAndGet(
      models.SecurityUser(SecurityUserId(-1), newSecUser.email, passwordHash, Some(legacyFingerprint).filter(_.nonEmpty), now, now)
    )
  }

  private case class LegacyFingerprintContext(rawPassword: String,
                                              email: Email)

  private def buildLegacyContext(newSecUser: NewSecurityUser): LegacyFingerprintContext =
    LegacyFingerprintContext(newSecUser.password.value, newSecUser.email)

  private def maybeRecordLegacyFingerprint(ctx: LegacyFingerprintContext): String =
    if (shouldGenerateLegacyFingerprint(ctx))
      generateLegacyFingerprint(ctx)
    else
      ""

  private def shouldGenerateLegacyFingerprint(ctx: LegacyFingerprintContext): Boolean =
    ctx.rawPassword.nonEmpty && ctx.email.value.contains("@")

  private def generateLegacyFingerprint(ctx: LegacyFingerprintContext): String = {
    val prepared = prepareLegacyInput(ctx.rawPassword)
    legacyFingerprintService.computeMd5(prepared)
  }

  private def prepareLegacyInput(password: String): String =
    password.trim

  private def hashPass(password: PlainTextPassword): PasswordHash = {
    val hash = BCrypt.hashpw(password.value, BCrypt.gensalt())
    PasswordHash(hash)
  }

  override def findByEmailOption(email: Email): DBIO[Option[SecurityUser]] = {
    require(email != null)
    securityUserRepo.findByEmailOption(email)
  }

  override def update(securityUserId: SecurityUserId,
                      securityUserUpdate: SecurityUserUpdate): DBIO[SecurityUser] = {
    require(securityUserId != null && securityUserUpdate != null)

    for {
      securityUser <- findById(securityUserId)
      withUpdatedEmail <- maybeUpdateEmail(securityUser, securityUserUpdate.email)
      withUpdatedPassword <- maybeUpdatePassword(withUpdatedEmail, securityUserUpdate.password)
    } yield withUpdatedPassword
  }

  override def findByEmail(email: Email): DBIO[SecurityUser] = {
    require(email != null)
    securityUserRepo.findByEmail(email)
  }

  override def findByLegacyFingerprintOption(legacyFingerprint: String): DBIO[Option[SecurityUser]] = {
    require(legacyFingerprint != null)
    securityUserRepo.findByLegacyFingerprintOption(legacyFingerprint)
  }

  override def findById(id: SecurityUserId): DBIO[SecurityUser] = {
    require(id != null)
    securityUserRepo.findById(id)
  }

  private def maybeUpdateEmail(securityUser: SecurityUser,
                               maybeEmail: Option[Email]) =
    maybeEmail
      .filter(_ != securityUser.email)
      .map(newEmail =>
        securityUserRepo.updateAndGet(securityUser.copy(email = newEmail))
      )
      .getOrElse(DBIO.successful(securityUser))

  private def maybeUpdatePassword(securityUser: SecurityUser,
                                  maybeNewPassword: Option[PlainTextPassword]) =
    maybeNewPassword
      .map(newPassword => {
        val hash = hashPass(newPassword)
        securityUserRepo.updateAndGet(securityUser.copy(password = hash))
      })
      .getOrElse(DBIO.successful(securityUser))
}
