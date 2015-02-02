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

package gr.grnet.egi.vmcatcher.db

import com.typesafe.config.Config
import net.liftweb.common.{Empty, Full}
import net.liftweb.db.StandardDBVendor
import net.liftweb.mapper.{DB, MapperRules, Schemifier}
import net.liftweb.util.DefaultConnectionIdentifier

/**
 *
 */
object MDB {
  def init(config: Config): Unit = {
    val dbConfig = config.getConfig("db")
    val driver = dbConfig.getString("driver")
    val url = dbConfig.getString("url")
    val username = dbConfig.getString("username")
    val password = dbConfig.getString("password")

    val vendor = new StandardDBVendor(
      driver,
      url,
      if(username.isEmpty) Empty else Full(username),
      if(password.isEmpty) Empty else Full(password)
    )

    DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    MapperRules.createForeignKeys_? = _ ⇒ true
    Schemifier.schemify(
      true,
      Schemifier.infoF _,
      MImageListAccess,
      MImageList,
      MImage
    )
  }
}
