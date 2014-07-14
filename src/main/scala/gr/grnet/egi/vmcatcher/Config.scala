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

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object Config {
  // Fix the Key.
  // _       _
  // the Config library does not like the ':' token inside keys, unless it is quoted.
  def fk(key: String) = key.replaceAll(":", "\":\"")

  def ofFilePath(path: String): com.typesafe.config.Config = {
    val file = new File(path)
    ConfigFactory.parseFile(file).resolve()
  }

  def ofString(s: String): com.typesafe.config.Config = ConfigFactory.parseString(s).resolve()
}
