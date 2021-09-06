package zmart.inventory.endpoints

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.Endpoint
import sttp.tapir.generic.auto._
import sttp.tapir.json.zio._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir._
import zhttp.http.{Http, Request, Response}
import zio.{Has, RIO, ZIO}
import zmart.fw.http.auth.AuthHelper
import zmart.inventory.services.{CreateItemRequest, Item, ItemService}

object ItemEndpoints {
  val listingLogic: Unit => ZIO[Has[ItemService], String, Seq[Item]] = _ => ItemService.all().mapError(_.getMessage)
  val findByIdLogic: Long => ZIO[Has[ItemService], String, Item]     = id => ItemService.get(id).mapError(_.getMessage)

  val createLogic: (JwtClaim, CreateItemRequest) => ZIO[Has[ItemService], String, Item] = (jwtClaim, request) =>
    ItemService.create(request).mapError(_.getMessage)

  // TODO - add this to the Auth FW stuff
  val validateTokenLogic: String => ZIO[Any, String, JwtClaim] = token =>
    ZIO.fromTry(Jwt.decode(token, AuthHelper.SECRET_KEY, Seq(JwtAlgorithm.HS512))).mapError(_.getMessage)

  val baseEndpoint: Endpoint[Unit, String, Unit, Any] =
    endpoint.tags(List("Items")).errorOut(stringBody).in("items")

  val secureBaseEndpoint: ZPartialServerEndpoint[Any, JwtClaim, Unit, String, Unit] =
    baseEndpoint
      .in(auth.bearer[String]())
      .name("HTTP Authentication")
      .description("Bearer token.  Must be a valid JSON Web Token.")
      .zServerLogicForCurrent(validateTokenLogic)

  val itemsListing: ZEndpoint[Unit, String, Seq[Item]] = baseEndpoint.get
    .name("List Items")
    .summary("List all Items in the shop.")
    .description("Returns a full list of all items in the shop.  No paging is currently implemented.")
    .out(jsonBody[Seq[Item]].description("All the Items"))

  val itemsById: ZEndpoint[Long, String, Item] = baseEndpoint.get
    .name("Get Item")
    .summary("Get a Specific Item")
    .description("Given an ID for an Item, return it.")
    .in(path[Long](name = "id").description("The ID of the item you want."))
    .out(jsonBody[Item].description("The Item"))

  val create: ZPartialServerEndpoint[Any, JwtClaim, CreateItemRequest, String, Item] = secureBaseEndpoint.post
    .name("Create Item")
    .summary("Create a New Item")
    .description("Create a new Item in the database")
    .in(jsonBody[CreateItemRequest].description("Information needed to create an Item"))
    .out(jsonBody[Item].description("The Item"))

  val listServerEndpoint: ZServerEndpoint[Has[ItemService], Unit, String, Seq[Item]] = itemsListing.zServerLogic[Has[ItemService]](listingLogic)
  val getServerEndpoint: ZServerEndpoint[Has[ItemService], Long, String, Item]       = itemsById.zServerLogic[Has[ItemService]](findByIdLogic)

  val createServerEndpoint: ZServerEndpoint[Has[ItemService], (create.T, CreateItemRequest), String, Item] = create.serverLogic { case (claim, request) =>
    createLogic(claim, request)
  }

  val allEndpoints = List(listServerEndpoint, getServerEndpoint, createServerEndpoint)

  // NOTE - because Tapir ZServerEndpoint gives us an R type with ZioStreams with WebSockets and the zio-http interpreter
  // doesn't support the WebSockets capability yet - we have to cast this down and use the EffectType type alias to help us
  // mash this into shape.
  type EffectType[A] = RIO[Has[ItemService], A]

  val allServerEndpoints =
    allEndpoints
      .asInstanceOf[List[ServerEndpoint[_, _, _, ZioStreams, EffectType]]]

  val itemsHttpApp: Http[Has[ItemService], Throwable, Request, Response[Has[ItemService], Throwable]] =
    ZioHttpInterpreter().toHttp(allServerEndpoints)
}
