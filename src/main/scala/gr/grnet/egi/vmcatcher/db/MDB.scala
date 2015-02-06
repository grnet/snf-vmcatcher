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

import gr.grnet.egi.vmcatcher.config.DBConfig
import net.liftweb.common.{Empty, Full}
import net.liftweb.db.StandardDBVendor
import net.liftweb.mapper.{DB, MapperRules, Schemifier}
import net.liftweb.util.DefaultConnectionIdentifier

/**
 *
 */
object MDB {
  def empty_?(s: String) = (s eq null) || s.isEmpty

  def init(dbConfig: DBConfig): Unit = {
    System.setProperty("run.mode", System.getProperty("run.mode", "production"))

    val driver = dbConfig.getJdbcDriver
    val url = dbConfig.getJdbcURL
    val username = dbConfig.getJdbcUsername
    val password = dbConfig.getJdbcPassword

    val vendor = new StandardDBVendor(
      driver,
      url,
      if(empty_?(username)) Empty else Full(username),
      if(empty_?(password)) Empty else Full(password)
    )

    DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)

    MapperRules.createForeignKeys_? = _ ⇒ true

    Schemifier.schemify(
      true,
      Schemifier.infoF _,
      MImageListRef,
      MImageListAccess,
      MCurrentImage,
      MImage
    )
  }
}
