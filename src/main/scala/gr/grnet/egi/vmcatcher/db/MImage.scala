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
 * A parsed image description. The respective table is used to hold the history
 * of all parsed images, so you may find the same revision of an image multiple times.
 */
class MImage extends LongKeyedMapper[MImage] with IdPK {
  def getSingleton = MImage

  // redundant but useful
  object f_imageListRef extends MappedLongForeignKey(this, MImageListRef) {
    override def dbColumnName = "image_list_ref_id"
    override def dbNotNull_? = true
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
    f_json(mText)
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

  def createRevision: MImageRevision = {
    MImageRevision.create.
      dcIdentifier(this.dcIdentifier.get).
      adMpuri(this.adMpuri.get).
      uniqueID(this.uniqueID.get).
      whenAccessed(this.f_imageListAccess.obj.dmap(new Date)(_.whenAccessed.get)).
      f_imageListRef(this.f_imageListRef.obj).
      f_imageListAccess(this.f_imageListAccess.obj).
      f_image(this)
  }
}

object MImage extends MImage with LongKeyedMetaMapper[MImage] {
  override def dbTableName = "IMAGE"

  def findByUniqueID(uniqueID: String): Box[MImage] = MImage.find(By(MImage.uniqueID, uniqueID))
}
