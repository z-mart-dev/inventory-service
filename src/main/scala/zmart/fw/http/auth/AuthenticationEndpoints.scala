package zmart.fw.http.auth

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.generic.auto._
import sttp.tapir.json.zio._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir._
import zhttp.http.{Http, Request, Response}
import zio.{IO, RIO, ZIO}

import java.time.Clock

object AuthenticationEndpoints {

  val loginLogic: LoginRequest => ZIO[Any, String, String] = loginRequest =>
    for {
      authenticatedUser <- AuthHelper.validateLogin(loginRequest).mapError(fl => s"${fl.user} has a failed login attempt.")
      jwtClaim          <- ZIO.effect(AuthHelper.jwtEncode(authenticatedUser.username)).mapError(_.getMessage)
    } yield jwtClaim

  val loginEndpoint: ZEndpoint[LoginRequest, String, String] = endpoint
    .tags(List("User Authentication"))
    .name("Login")
    .summary("Login via User Credentials")
    .description("Login via User Credentials and receive a valid JSON Web Token (JWT) in response.  This can be used as a Bearer token for other endpoints.")
    .in("login")
    .post
    .in(jsonBody[LoginRequest].description("User credentials."))
    .out(stringBody.description("A Valid JWT"))
    .errorOut(stringBody)

  val loginServerEndpoint: ZServerEndpoint[Any, LoginRequest, String, String] = loginEndpoint.zServerLogic(loginLogic)

  type EffectType[A] = RIO[Any, A]

  val loginApp: Http[Any, Throwable, Request, Response[Any, Throwable]] =
    ZioHttpInterpreter().toHttp(loginServerEndpoint.asInstanceOf[ServerEndpoint[_, _, _, ZioStreams, EffectType]])
}

object AuthHelper {
  // Secret Authentication key
  val SECRET_KEY = "secretKey"

  implicit val clock: Clock = Clock.systemUTC

  // Helper to encode the JWT token
  def jwtEncode(username: String): String = {
    val json  = s"""{"user": "${username}"}"""
    val claim = JwtClaim { json }.issuedNow.expiresIn(300)
    Jwt.encode(claim, SECRET_KEY, JwtAlgorithm.HS512)
  }

  def validateLogin(request: LoginRequest): IO[FailedLogin, AuthenticatedUser] =
    if (request.password == request.username.reverse) {
      ZIO.succeed(AuthenticatedUser(request.username))
    } else {
      ZIO.fail(FailedLogin(request.username))
    }
}
