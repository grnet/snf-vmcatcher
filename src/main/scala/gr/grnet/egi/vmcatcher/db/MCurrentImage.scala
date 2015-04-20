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
 * Current definition of an image.
 */
class MCurrentImage extends LongKeyedMapper[MCurrentImage] with IdPK {
  def getSingleton = MCurrentImage

  object f_image extends MappedLongForeignKey(this, MImage) {
    override def dbColumnName = "image_id"
    override def dbNotNull_? = true
  }

  // redundant but useful
  object f_imageListAccess extends MappedLongForeignKey(this, MImageListAccess) {
    override def dbColumnName = "image_list_access_id"
    override def dbNotNull_? = true
  }

  // redundant but useful
  object f_imageListRef extends MappedLongForeignKey(this, MImageListRef) {
    override def dbColumnName = "image_list_ref_id"
    override def dbNotNull_? = true
  }

  // redundant but useful
  object dcIdentifier extends MappedString(this, MImage.dcIdentifier.maxLen) {
    override def dbColumnName = MImage.dcIdentifier.dbColumnName
  }

  // redundant but useful
  object adMpuri extends MappedString(this, MImage.adMpuri.maxLen) {
    override def dbColumnName = MImage.adMpuri.dbColumnName
  }

  def findAllOfImageListRef(ref: MImageListRef): List[MCurrentImage] =
    MCurrentImage.findAll(By(MCurrentImage.f_imageListRef, ref))
}

object MCurrentImage extends MCurrentImage with LongKeyedMetaMapper[MCurrentImage] {
  override def dbTableName = "CURRENT_IMAGE"
}
