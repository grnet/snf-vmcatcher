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

import gr.grnet.egi.vmcatcher.Digest
import net.liftweb.common.Box
import net.liftweb.mapper._

/**
 * Provides information about an image. This includes information regarding the
 *   a) parsed image data from its JSON description and
 *   b) image registration status, as far as the IaaS is concerned.
 */
class MImage extends LongKeyedMapper[MImage] with IdPK {
  def getSingleton = MImage

  // redundant but useful
  object f_imageListRef extends MappedLongForeignKey(this, MImageList) {
    override def dbColumnName = "image_list_ref_id"
    override def dbNotNull_? = true
  }

  // redundant but useful
  object whenAccessed extends MappedDateTime(this) {
    override def dbColumnName: String = "when_accessed"
    override def dbNotNull_? : Boolean = true
    override def dbIndexed_? : Boolean = true
  }

  object f_imageListAccess extends MappedLongForeignKey(this, MImageListAccess) {
    override def dbColumnName = "image_list_access_id"
  }

  object f_json extends MappedLongForeignKey(this, MText) {
    override def dbColumnName = "json_id"
  }

  def json(text: String) = {
    val mText = MText.getOrCreate(text)
    f_json(mText)
  }

  object f_envJson extends MappedLongForeignKey(this, MText) {
    override def dbColumnName = "env_json_id"
  }

  def envJson(text: String) = {
    val mText = MText.getOrCreate(text)
    f_envJson(mText)
  }

  object adMpuri extends MappedString(this, 512) {
    override def dbColumnName = "ad_mpuri"
  }

  object adUserUri extends MappedString(this, 512) {
    override def dbColumnName = "ad_user_uri"
  }

  object adUserGuid extends MappedString(this, 128) {
    override def dbColumnName = "ad_user_guid"
  }

  object adUserFullName extends MappedString(this, 128) {
    override def dbColumnName = "ad_user_full_name"
  }

  object dcIdentifier extends MappedString(this, 128) {
    override def dbColumnName = "dc_identifier"
  }

  object dcTitle extends MappedString(this, 128) {
    override def dbColumnName = "dc_title"
  }

  object hvUri extends MappedString(this, 512) {
    override def dbColumnName = "hv_uri"
  }

  object hvHypervisor extends MappedString(this, 64) {
    override def dbColumnName = "hv_hypervisor"
  }

  object hvFormat extends MappedPoliteString(this, 64) {
    override def dbColumnName = "hv_format"
  }

  object hvSize extends MappedLong(this) {
    override def dbColumnName = "hv_size"
  }

  object slOs extends MappedString(this, 64) {
    override def dbColumnName = "sl_os"
  }

  object slOsName extends MappedString(this, 64) {
    override def dbColumnName = "sl_os_name"
  }

  object slOsVersion extends MappedString(this, 64) {
    override def dbColumnName = "sl_os_version"
  }

  object slArch extends MappedString(this, 64) {
    override def dbColumnName = "sl_arch"
  }

  object slChecksum512 extends MappedString(this, 64 * 2 + "sha512-".length) {
    override def dbColumnName = "sl_checksum_512"
  }

  object uniqueID extends MappedString(this, 32 * 2) {
    override def dbColumnName = "unique_id"
  }

  /**
   * The label of an image identifies the image and is stables across revisions.
   */
  def imageLabel = dcIdentifier.get
  def imageRevision = adMpuri.get

  /**
   * This uniquely identifies an image revisions across all image lists.
   */
  def computeUniqueID = Digest.hexSha256(imageLabel + imageRevision)

  def repr: String = s"""{label: "$imageLabel", revision: "$imageRevision", uniqueID: "$computeUniqueID"}"""

  object isActive extends MappedBoolean(this) {
    override def dbColumnName: String = "is_active"
  }

  /**
   * This is true iff the image was defined in the latest image list access.
   */
  object isLatest extends MappedBoolean(this) {
    override def dbColumnName: String = "is_latest"
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
   * This file is transient.
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
   * The ID that the uploaded image has in the IaaS.
   * For Synnefo this is a UUID of 36 chars.
   */
  object uploadID extends MappedString(this, 36) {
    override def dbColumnName: String = "upload_id"
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

  def findAllOfImageListRef(ref: MImageList): List[MImage] =
    MImage.findAll(By(MImage.f_imageListRef, ref))
}

object MImage extends MImage with LongKeyedMetaMapper[MImage] {
  override def dbTableName = "IMAGE"

  def findByUniqueID(uniqueID: String): Box[MImage] = MImage.find(By(MImage.uniqueID, uniqueID))

  def findAllByUniqueIDPrefix(uniqueID: String): List[MImage] = MImage.findAll(Like(MImage.uniqueID, uniqueID))

  def existsByUniqueID(uniqueID: String): Boolean = this.findByUniqueID(uniqueID).isDefined
}
