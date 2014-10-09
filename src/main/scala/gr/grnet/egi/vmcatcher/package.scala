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

package gr.grnet.egi

/**
 *
 */
package object vmcatcher {
  def DEFER(f: ⇒ Unit) = () ⇒ f

  def deepScalaToJava[J](t: J): Any = {
    import scala.collection.JavaConverters._

    t match {
      case tMap: Map[_, _] ⇒
        tMap.map { case (k, v) ⇒ deepScalaToJava(k) → deepScalaToJava(v) }. asJava

      case id ⇒ id
    }
  }


  implicit class JsonToStringMap(val json: String) extends AnyVal {
    def jsonToStringMap: Map[String, String] = Json.stringMapOfJson(json)
  }
}
