package users.services

import authentication.api.SecurityUserCreator
import commons.exceptions.ValidationException
import commons.repositories.DateTimeProvider
import commons.utils.DbioUtils
import authentication.models.{NewSecurityUser, SecurityUserId}
import users.models.{User, UserId, UserRegistration}
import users.repositories.UserRepo
import play.api.Configuration
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

private[users] class UserRegistrationService(userRegistrationValidator: UserRegistrationValidator,
                                             securityUserCreator: SecurityUserCreator,
                                             dateTimeProvider: DateTimeProvider,
                                             userRepo: UserRepo,
                                             config: Configuration) {

  private val defaultImage = Some(config.get[String]("app.defaultImage"))

  def register(userRegistration: UserRegistration): DBIO[User] = {
    for {
      _ <- validate(userRegistration)
      user <- doRegister(userRegistration)
    } yield user
  }

  private def validate(userRegistration: UserRegistration) = {
    userRegistrationValidator.validate(userRegistration)
      .flatMap(violations => DbioUtils.fail(violations.isEmpty, new ValidationException(violations)))
  }

  /** Prepares credentials for security user creation (password flows through to auth layer). */
  private def prepareSecurityUserForCreation(userRegistration: UserRegistration): NewSecurityUser = {
    NewSecurityUser(userRegistration.email, userRegistration.password)
  }

  private def doRegister(userRegistration: UserRegistration) = {
    val newSecurityUser = prepareSecurityUserForCreation(userRegistration)
    for {
      securityUser <- securityUserCreator.create(newSecurityUser)
      now = dateTimeProvider.now
      user = User(UserId(-1), securityUser.id, userRegistration.username, userRegistration.email, null, defaultImage,
        now, now)
      savedUser <- userRepo.insertAndGet(user)
    } yield savedUser
  }
}



