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
case class Event(
  VMCATCHER_EVENT_DC_DESCRIPTION: String,
  VMCATCHER_EVENT_DC_IDENTIFIER: String,
  VMCATCHER_EVENT_DC_TITLE: String,
  VMCATCHER_EVENT_HV_HYPERVISOR: String,
  VMCATCHER_EVENT_HV_SIZE: String,
  VMCATCHER_EVENT_HV_URI: String,
  VMCATCHER_EVENT_SL_ARCH: String,
  VMCATCHER_EVENT_SL_CHECKSUM_SHA512: String,
  VMCATCHER_EVENT_SL_COMMENTS: String,
  VMCATCHER_EVENT_SL_OS: String,
  VMCATCHER_EVENT_SL_OSVERSION: String,
  VMCATCHER_EVENT_TYPE: String,
  VMCATCHER_EVENT_FILENAME: String,
  VMCATCHER_EVENT_IL_DC_IDENTIFIER: String,
  VMCATCHER_EVENT_HV_FORMAT: String
)

object Event {
  def ofMap(map: Map[String, String]): Event = {
    def get(key: Key): String = map.get(key.name()).getOrElse("")

    Event(
      VMCATCHER_EVENT_DC_DESCRIPTION = get(Key.VMCATCHER_EVENT_DC_DESCRIPTION),
      VMCATCHER_EVENT_DC_IDENTIFIER = get(Key.VMCATCHER_EVENT_DC_IDENTIFIER),
      VMCATCHER_EVENT_DC_TITLE = get(Key.VMCATCHER_EVENT_DC_TITLE),
      VMCATCHER_EVENT_HV_HYPERVISOR = get(Key.VMCATCHER_EVENT_HV_HYPERVISOR),
      VMCATCHER_EVENT_HV_SIZE = get(Key.VMCATCHER_EVENT_HV_SIZE),
      VMCATCHER_EVENT_HV_URI = get(Key.VMCATCHER_EVENT_HV_URI),
      VMCATCHER_EVENT_SL_ARCH = get(Key.VMCATCHER_EVENT_SL_ARCH),
      VMCATCHER_EVENT_SL_CHECKSUM_SHA512 = get(Key.VMCATCHER_EVENT_SL_CHECKSUM_SHA512),
      VMCATCHER_EVENT_SL_COMMENTS = get(Key.VMCATCHER_EVENT_SL_COMMENTS),
      VMCATCHER_EVENT_SL_OS = get(Key.VMCATCHER_EVENT_SL_OS),
      VMCATCHER_EVENT_SL_OSVERSION = get(Key.VMCATCHER_EVENT_SL_OSVERSION),
      VMCATCHER_EVENT_TYPE = get(Key.VMCATCHER_EVENT_TYPE),
      VMCATCHER_EVENT_FILENAME = get(Key.VMCATCHER_EVENT_FILENAME),
      VMCATCHER_EVENT_IL_DC_IDENTIFIER = get(Key.VMCATCHER_EVENT_IL_DC_IDENTIFIER),
      VMCATCHER_EVENT_HV_FORMAT = get(Key.VMCATCHER_EVENT_HV_FORMAT)
    )
  }
}