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

package gr.grnet.egi.vmcatcher

import java.io.File
import java.net.URL
import java.nio.file.{StandardCopyOption, Files}

import com.squareup.okhttp.{OkHttpClient, Request, Response}

/**
 *
 */
object Http {
  val StdOkHttpClient = new OkHttpClient

  def GET(url: URL): Response = {
    val builder = new Request.Builder().url(url).get()
    val request = builder.build()
    val call = StdOkHttpClient.newCall(request)
    val response = call.execute()
    response
  }

  def downloadToFile(url: URL, file: File): Unit = {
    val response = GET(url)
    val input = response.body().byteStream()
    Files.copy(input, file.toPath, StandardCopyOption.REPLACE_EXISTING)
  }
}
