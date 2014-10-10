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

import java.io.File

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._

/**
 *
 */
object Config {
  def ofFilePath(path: String): com.typesafe.config.Config = {
    val file = new File(path)
    ConfigFactory.parseFile(file).resolve()
  }

  def ofString(s: String): com.typesafe.config.Config = ConfigFactory.parseString(s).resolve()

  def toMap(config: com.typesafe.config.Config): Map[String, String] =
    config.root().unwrapped().asScala.map{ case (k, v) ⇒ (k, String.valueOf(v)) }.toMap
}
