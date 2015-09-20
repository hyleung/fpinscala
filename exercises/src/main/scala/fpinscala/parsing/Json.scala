package fpinscala.parsing

/**
 * Created with IntelliJ IDEA.
 * Date: 15-09-19
 * Time: 11:44 AM
 * To change this template use File | Settings | File Templates.
 */
trait Json

object Json {
	case object JNull extends Json
	case class JNumber(get: Double) extends Json
	case class JString(get: String) extends Json
	case class JBool(get: Boolean) extends Json
	case class JArray(get: IndexedSeq[Json]) extends Json
	case class JObject(get: Map[String, Json]) extends Json

	def jsonParser[Parser[+_]](P:Parsers[Parser]):Parser[Json] = {
		import P._

		val jTrue:Parser[JBool] = string("true").map(_ => JBool(true))
		val jfalse:Parser[JBool] = string("false").map(_ => JBool(false))
		val jNull:Parser[Json] = string("null").map(_ => JNull)
		//return the grammar for json
		???
	}
}
