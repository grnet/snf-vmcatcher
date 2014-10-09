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
package message

import scala.collection.JavaConverters._

/**
 *
 */
trait ConfigWrapper {
  def config: com.typesafe.config.Config

  def toJson: String = {
    val jmap = config.root().unwrapped()
    val map = jmap.asScala.mapValues(String.valueOf).toMap

    Json.jsonOfMap(map)
  }

  def fk(key: String) = Config.fk(key)

  def has(key: String): Boolean = config.hasPath(fk(key))

  def findString(key: String): Option[String] =
    if(has(key)) Some(config.getString(fk(key)))
    else None

  def getString(key: String): String =
    if(has(key)) config.getString(fk(key))
    else ""
}

trait ConfigWrapperDcIdentifier { self: ConfigWrapper ⇒
  def hasDcIdentifier = has("dc:identifier")
  def dcIdentifier = getString("dc:identifier")
}

trait ConfigWrapperDcTitle { self: ConfigWrapper ⇒
  def dcTitle = getString("dc:title")
}

trait ConfigWrapperHvURI { self: ConfigWrapper ⇒
  def hasHvURI = has("hv:uri")
  def hvURI = getString("hv:uri")
}

trait ConfigWrapperHvVersion { self: ConfigWrapper ⇒
  def hvVersion = getString("hv:version")
}
