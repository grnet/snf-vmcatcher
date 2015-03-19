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

package gr.grnet.egi.vmcatcher.http

import com.squareup.okhttp.ResponseBody
import okio.Okio

case class HttpResponse(
  statusCode: Int,
  statusText: String,
  body: ResponseBody
) {
  def is2XX: Boolean = (statusCode >= 200) && (statusCode < 300)

  lazy val getUtf8: String = {
    val stream = body.byteStream()

    val source = Okio.source(stream)
    val buffer = Okio.buffer(source)
    val utf8 = buffer.readUtf8()
    stream.close()
    utf8
  }
}
