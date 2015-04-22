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

import net.liftweb.common.Box
import net.liftweb.mapper._

/**
 *
 */
class MImageRevision extends LongKeyedMapper[MImageRevision] with IdPK {
  def getSingleton = MImageRevision

  object f_image extends MappedLongForeignKey(this, MImage) {
    override def dbColumnName = "image_id"
    override def dbNotNull_? = true
  }

  object isActive extends MappedBoolean(this) {
    override def dbColumnName: String = "is_active"
  }

  /**
   * The full path of the locally downloaded image file.
   * The image file is assumed to be transient.
   */
  object originalFile extends MappedPoliteString(this, 256) {
    override def dbColumnName: String = "original_file"
  }

  /**
   * The full path of the transformed image file that has been uploaded to the IaaS.
   */
  object transformedFile extends MappedPoliteString(this, 256) {
    override def dbColumnName: String = "transformed_file"
  }

  /**
   * When the image was transformed to a format understood by the target IaaS
   * (currently the raw format for Synnefo).
   */
  object whenTransformed extends MappedDateTime(this) {
    override def dbColumnName: String = "when_transformed"
  }

  /**
   * When the image was successfully uploaded to the IaaS.
   */
  object whenUploaded extends MappedDateTime(this) {
    override def dbColumnName: String = "when_uploaded"
  }

  /**
   * The URL of the uploaded image on the IaaS.
   */
  object uploadURL extends MappedPoliteString(this, 256) {
    override def dbColumnName: String = "upload_url"
  }

  /**
   * The exception message, if any exception was thrown.
   */
  object exceptionMsg extends MappedPoliteString(this, 256) {
    override def dbColumnName: String = "exception_msg"
  }

  /**
   * The exception stacktrace, if any exception was thrown.
   */
  object f_stacktrace extends MappedLongForeignKey(this, MText) {
    override def dbColumnName: String = "stacktrace_id"
  }

  // redundant but useful
  object whenAccessed extends MappedDateTime(this) {
    override def dbColumnName: String = "when_accessed"
    override def dbNotNull_? : Boolean = true
    override def dbIndexed_? : Boolean = true
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

  // redundant but useful
  object uniqueID extends MappedString(this, MImage.uniqueID.maxLen) {
    override def dbColumnName = "unique_id"
  }

  def findAllOfImageListRef(ref: MImageListRef): List[MImageRevision] =
    MImageRevision.findAll(By(MImageRevision.f_imageListRef, ref))
}

object MImageRevision extends MImageRevision with LongKeyedMetaMapper[MImageRevision] {
  override def dbTableName = "IMAGE_REVISION"

  def findByUniqueID(uniqueID: String): Box[MImageRevision] = MImageRevision.find(By(MImageRevision.uniqueID, uniqueID))
  def findByUniqueID(mimage: MImage): Box[MImageRevision] = findByUniqueID(mimage.uniqueID.get)
  def existsByUniqueID(mimage: MImage): Boolean = this.findByUniqueID(mimage).isDefined
}

