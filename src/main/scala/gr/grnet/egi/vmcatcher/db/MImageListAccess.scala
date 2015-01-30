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

import net.liftweb.mapper._

/**
 *
 */
class MImageListAccess extends LongKeyedMapper[MImageListAccess] with IdPK {
  def getSingleton = MImageListAccess

  object url extends MappedPoliteString(this, 256) {
    override def dbColumnName = "url"
    override def dbIndexed_? = true
  }

  object username extends MappedPoliteString(this, 128) {
    override def dbColumnName = "username"
  }

  object password extends MappedPoliteString(this, 128) {
    override def dbColumnName = "password"
  }
}

object MImageListAccess extends MImageListAccess with LongKeyedMetaMapper[MImageListAccess] {
  override def dbTableName = "IMAGE_LIST_ACCESS"
}
