/*
 * Copyright (C) 2014 GRNET S.A.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package gr.grnet.egi.vmcatcher

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, JsonFactory}
import scala.collection.JavaConverters._
import com.fasterxml.jackson.core.`type`.TypeReference
import java.io.StringWriter

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object Json {
  val mapper = new ObjectMapper()
  mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

  val JMapTypeRef = new TypeReference[java.util.Map[String, String]] {}

  val jf = new JsonFactory(mapper)
  val pp = new DefaultPrettyPrinter()

  jf.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
  jf.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true)
  jf.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true)

  def mapOfJson(json: String): Map[String, String] = {
    val jmap = mapper.readValue[java.util.Map[String, String]](json, JMapTypeRef)

    jmap.asScala.toMap
  }

  def jsonOfMap(map: Map[String, String], pretty: Boolean = true): String = {
    val jmap = map.asJava
    val sw = new StringWriter()
    val jg = jf.createGenerator(sw)
    if(pretty) { jg.setPrettyPrinter(pp) }
    jg.writeObject(jmap)
    sw.toString
  }
}
