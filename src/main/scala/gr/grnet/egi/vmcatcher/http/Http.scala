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

import java.net.{Proxy, URL}

import com.squareup.okhttp._
import gr.grnet.egi.vmcatcher.util.UsernamePassword

object Http {
  private[this] val StdOkHttpClient = new OkHttpClient

  def Authenticator(username: String, password: String): Authenticator = {
    val authorization = Credentials.basic(username, password)

    new Authenticator {
      def authenticateProxy(proxy: Proxy, response: Response): Request = null

      def authenticate(proxy: Proxy, response: Response): Request =
        response.request().newBuilder().
          addHeader("Authorization", authorization).
          build()
    }
  }

  def Authenticator(up: UsernamePassword): Authenticator = {
    val username = up.username
    val password = Option(up.password).getOrElse("")
    Authenticator(username, password)
  }

  def GET(url: URL, upOpt: Option[UsernamePassword]): HttpResponse = {
    val client = StdOkHttpClient.clone()

    for(up ← upOpt) yield {
      client.setAuthenticator(Authenticator(up))
    }

    val request = new Request.Builder().url(url).get().build()
    val call = client.newCall(request)
    val response = call.execute()
    val code = response.code()
    val text = response.message()
    val body = response.body()

    HttpResponse(code, text, body)
  }
}
