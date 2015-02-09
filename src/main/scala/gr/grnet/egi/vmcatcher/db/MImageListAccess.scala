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

import net.liftweb.mapper._

/**
 * Represents the status of downloading an image list, parsing it etc.
 */
class MImageListAccess extends LongKeyedMapper[MImageListAccess] with IdPK {
  def getSingleton = MImageListAccess

  object f_imageListRef extends MappedLongForeignKey(this, MImageListRef) {
    override def dbColumnName = "image_list_ref_id"
  }

  object whenAccessed extends MappedDateTime(this) {
    override def dbColumnName: String = "when_accessed"
    override def dbNotNull_? : Boolean = true
    override def dbIndexed_? : Boolean = true

    override def defaultValue = new Date
  }

  object wasRetrieved extends MappedBoolean(this) {
    override def dbColumnName: String = "was_retrieved"
    override def dbNotNull_? : Boolean = true
  }

  object wasParsed extends MappedBoolean(this) {
    override def dbColumnName: String = "was_parsed"
    override def dbNotNull_? : Boolean = true
  }

  object errorMsg extends MappedPoliteString(this, 256) {
    override def dbColumnName: String = "error_msg"
  }

  object rawText extends MappedText(this) {
    override def dbColumnName = "raw_text"
  }

  object parsedJson extends MappedText(this) {
    override def dbColumnName = "parsed_json"
  }

  def saveRetrieved(rawText: String) =
    this.wasRetrieved(true).rawText(rawText).saveMe()

  def saveRetrievedAndParsed(rawText: String, parsedJson: String) =
    this.wasRetrieved(true).wasParsed(true).rawText(rawText).parsedJson(parsedJson).saveMe()

  def saveParsed(parsedJson: String) =
    this.wasRetrieved(true).wasParsed(true).parsedJson(parsedJson).saveMe()

  def saveNotRetrieved() =
    this.wasRetrieved(false).wasParsed(false).saveMe()

  def saveParsed(f: Boolean) = this.wasParsed(f).saveMe()
}

object MImageListAccess extends MImageListAccess with LongKeyedMetaMapper[MImageListAccess] {
  override def dbTableName: String = "IMAGE_LIST_ACCESS"
}
