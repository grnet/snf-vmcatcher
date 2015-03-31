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

import gr.grnet.egi.vmcatcher.util.stacktraceAsString
import gr.grnet.egi.vmcatcher.http.HttpResponse
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

  object httpStatusCode extends MappedInt(this) {
    override def dbColumnName: String = "http_status_code"
    override def dbNotNull_? : Boolean = true
  }

  object httpStatusText extends MappedPoliteString(this, 64) {
    override def dbColumnName: String = "http_status_text"
  }

  object exceptionMsg extends MappedPoliteString(this, 256) {
    override def dbColumnName: String = "exception_msg"
  }

  object f_stacktrace extends MappedLongForeignKey(this, MText) {
    override def dbColumnName: String = "stacktrace_id"
  }

  /**
   * The linked text is the HTTP response body.
   */
  object f_rawText extends MappedLongForeignKey(this, MText) {
    override def dbColumnName = "raw_text_id"
  }

  object f_parsedJson extends MappedLongForeignKey(this, MText) {
    override def dbColumnName = "parsed_json_id"
  }

  def parsedJson(json: String) = {
    val rawMText = MText.getOrCreate(json)
    f_parsedJson(rawMText)
  }

  def rawText(raw: String) = {
    val rawMText = MText.getOrCreate(raw)
    f_rawText(rawMText)
  }

  def setRetrieved() =
    this.
      wasRetrieved(true).
      wasParsed(false)

  def setParsed(parsedJson: String) =
    this.
      wasParsed(true).
      parsedJson(parsedJson)

  def isOK = /*wasRetrieved.get && */wasParsed.get
}

object MImageListAccess extends MImageListAccess with LongKeyedMetaMapper[MImageListAccess] {
  override def dbTableName: String = "IMAGE_LIST_ACCESS"

  private def mk(ref: MImageListRef, r: HttpResponse): MImageListAccess =
    create.
      f_imageListRef(ref).
      httpStatusCode(r.statusCode).
      httpStatusText(r.statusText).
      rawText(r.getUtf8)

  def createRetrieved(ref: MImageListRef, r: HttpResponse): MImageListAccess =
    mk(ref, r).
      wasRetrieved(true).
      wasParsed(false)

  def createErrorStatus(ref: MImageListRef, r: HttpResponse): MImageListAccess = {
    assert(!r.is2XX)
    mk(ref, r).
      wasRetrieved(false).
      wasParsed(false)
  }

  def createNotRetrieved(ref: MImageListRef, t: Throwable): MImageListAccess = {
    val stacktrace = stacktraceAsString(t)
    val mtext = MText.getOrCreate(stacktrace)

    create.
      f_imageListRef(ref).
      f_stacktrace(mtext).
      wasRetrieved(false).
      wasParsed(false).
      exceptionMsg(s"${t.getClass.getName}: ${t.getMessage}")
  }
}
