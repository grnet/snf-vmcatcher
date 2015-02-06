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

import java.util.Date

import gr.grnet.egi.vmcatcher.util.UsernamePassword
import net.liftweb.mapper._

/**
 *
 */
class MImageListRef extends LongKeyedMapper[MImageListRef] with IdPK {
  def getSingleton = MImageListRef

  object whenRegistered extends MappedDateTime(this) {
    override def dbColumnName: String = "when_registered"
    override def dbNotNull_? = true
    override def defaultValue: Date = new Date
  }

  object isActive extends MappedBoolean(this) {
    override def dbColumnName: String = "is_active"
  }

  object name extends MappedPoliteString(this, 64) {
    override def dbColumnName = "name"
    override def dbNotNull_? = true
  }

  object url extends MappedPoliteString(this, 256) {
    override def dbColumnName = "url"
    override def dbNotNull_? = true
  }

  object username extends MappedPoliteString(this, 128) {
    override def dbColumnName = "username"
  }

  object password extends MappedPoliteString(this, 128) {
    override def dbColumnName = "password"
  }

  def credentialsOpt: Option[UsernamePassword] =
    username.get match {
      case null ⇒ None
      case username ⇒ Some(UsernamePassword(username, password.get))
    }

  def setCredentials(upOpt: Option[UsernamePassword]) = {
    for {
      up ← upOpt
    } {
      this.username(up.username).password(up.password)
    }
    this
  }

  def activate()   = this.isActive(true).saveMe()
  def deactivate() = this.isActive(false).saveMe()

  def newAccess() = MImageListAccess.create.f_imageListRef(this).whenAccessed(new Date)
}

object MImageListRef extends MImageListRef with LongKeyedMetaMapper[MImageListRef] {
  override def dbTableName = "IMAGE_LIST_REF"

  def findByName(name: String) = MImageListRef.find(By(MImageListRef.name, name)).toOption
}
