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

package gr.grnet.egi.vmcatcher.data

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class Conf(
  VMCATCHER_RDBMS: String,
  VMCATCHER_CACHE_EVENT: String,
  VMCATCHER_LOG_CONF: String,
  VMCATCHER_DIR_CERT: String,
  VMCATCHER_CACHE_DIR_CACHE: String,
  VMCATCHER_CACHE_DIR_DOWNLOAD: String,
  VMCATCHER_CACHE_DIR_EXPIRE: String,
  VMCATCHER_CACHE_ACTION_DOWNLOAD: String,
  VMCATCHER_CACHE_ACTION_CHECK: String,
  VMCATCHER_CACHE_ACTION_EXPIRE: String
)

object Conf {
  def ofMap(map: Map[String, String]): Conf = {
    def get(key: Key): String = map.get(key.name()).getOrElse("")

    Conf(
      VMCATCHER_RDBMS = get(Key.VMCATCHER_RDBMS),
      VMCATCHER_CACHE_EVENT = get(Key.VMCATCHER_CACHE_EVENT),
      VMCATCHER_LOG_CONF = get(Key.VMCATCHER_LOG_CONF),
      VMCATCHER_DIR_CERT = get(Key.VMCATCHER_DIR_CERT),
      VMCATCHER_CACHE_DIR_CACHE = get(Key.VMCATCHER_CACHE_DIR_CACHE),
      VMCATCHER_CACHE_DIR_DOWNLOAD = get(Key.VMCATCHER_CACHE_DIR_DOWNLOAD),
      VMCATCHER_CACHE_DIR_EXPIRE = get(Key.VMCATCHER_CACHE_DIR_EXPIRE),
      VMCATCHER_CACHE_ACTION_DOWNLOAD = get(Key.VMCATCHER_CACHE_ACTION_DOWNLOAD),
      VMCATCHER_CACHE_ACTION_CHECK = get(Key.VMCATCHER_CACHE_ACTION_CHECK),
      VMCATCHER_CACHE_ACTION_EXPIRE = get(Key.VMCATCHER_CACHE_ACTION_EXPIRE)
    )
  }
}
