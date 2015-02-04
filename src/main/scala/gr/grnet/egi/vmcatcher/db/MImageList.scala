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
class MImageList extends LongKeyedMapper[MImageList] with IdPK {
  def getSingleton = MImageList

  object f_imageListRef extends MappedLongForeignKey(this, MImageListRef) {
    override def dbColumnName = "image_list_ref_id"
  }

  object whenAccessed extends MappedDateTime(this) {
    override def dbColumnName = "when_accessed"
  }
  
  object wasSuccessfulAccess extends MappedBoolean(this) {
    override def dbColumnName = "was_successful_access"
  }

  object accessHttpCode extends MappedInt(this) {
    override def dbColumnName = "access_http_code"
  }

  object retrievedRawData extends MappedText(this) {
    override def dbColumnName = "retrieved_raw_data"
  }

  object retrievedParsedJson extends MappedText(this) {
    override def dbColumnName = "retrieved_parsed_json"
  }
}

object MImageList extends MImageList with LongKeyedMetaMapper[MImageList] {
  override def dbTableName = "IMAGE_LIST"
}
