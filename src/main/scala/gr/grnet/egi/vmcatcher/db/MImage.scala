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

import java.net.URI
import java.util.Locale

import gr.grnet.egi.vmcatcher.event.EventOrigin
import net.liftweb.mapper._

/**
 * Provides information about an image. This includes information regarding the
 *   a) parsed image data from its JSON description and
 *   b) image registration status, as far as the IaaS is concerned.
 */
class MImage extends LongKeyedMapper[MImage] with IdPK {
  def getSingleton = MImage

  // redundant but useful
  object f_imageList extends MappedLongForeignKey(this, MImageList) {
    override def dbColumnName = "image_list_id"
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

  object eventOrigin extends MappedString(this, EventOrigin.values().map(_.name().length).max) {
    override def dbColumnName = "event_origin"

    def apply(eo: EventOrigin): MImage = this(eo.name().toLowerCase(Locale.ENGLISH))
  }

  object adMpuri extends MappedString(this, 512) {
    override def dbColumnName = "ad_mpuri"
  }

  /**
   * The revision of the image. This is extracted from "ad:mpuri" attribute
   */
  object revision extends MappedString(this, 128) {
    override def dbColumnName = "revision"
  }

  def computeRevision = {
    // fullRevURI:  https://appdb.egi.eu/store/vo/image/38d42ca1-f4e9-5b5c-98de-37eb2d26301a:604/
    // path:        /store/vo/image/38d42ca1-f4e9-5b5c-98de-37eb2d26301a:604/
    // imageLabel:  38d42ca1-f4e9-5b5c-98de-37eb2d26301a
    // revision:    604
    val fullRevURI = new URI(adMpuri.get)
    val path = fullRevURI.getPath
    val identifier = dcIdentifier.get
    path.indexOf(identifier) match {
      case -1 ⇒
        adMpuri.get
      case labelIndex ⇒
        val revStartIndex = labelIndex + identifier.length + 1
        val rev = path.substring(revStartIndex).filter(_ != '/') // stripSuffix("/")

        rev
    }
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

  def identifierAndRevision = (dcIdentifier.get, revision.get)

  def repr: String = {
    val (name, revision) = identifierAndRevision
    s"$name:$revision"
  }

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
    MImage.findAll(By(MImage.f_imageList, ref))
}

object MImage extends MImage with LongKeyedMetaMapper[MImage] {
  override def dbTableName = "IMAGE"
}
