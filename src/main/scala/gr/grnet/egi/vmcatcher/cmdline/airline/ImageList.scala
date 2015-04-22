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

package gr.grnet.egi.vmcatcher.cmdline.airline

import java.net.URL

import gr.grnet.egi.vmcatcher.Program
import gr.grnet.egi.vmcatcher.util.UsernamePassword
import io.airlift.airline.{Command, Option}

object ImageList extends Program with CommonOptions {
  @Command(name = "register", description = "Registers the image list in our database")
  class Register extends Global with NameArgument with CredentialsOpt {
    @Option(
      name = Array("--url"),
      description = "The URL of the image list",
      required = true,
      arity = 1
    )
    val url = ""

    @Option(
      name = Array("--activate"),
      description = "Register the image list as active (true) or inactive (false). Default is true.",
      required = false,
      arity = 1
    )
    val activate = true

    def run(): Unit = {
      val upOpt = UsernamePassword.optional(username, password)
      val ref = vmcatcher.registerImageList(name, new URL(url), activate, upOpt)
      INFO(s"Registered $ref")
    }
  }

  @Command(name = "show-all", description = "Prints the known image lists")
  class ShowAll extends Global {
    def run(): Unit = {
      val ils = vmcatcher.listImageLists()
      val nameLengths = ils.map(_.name.get.length)
      val maxNameLen = nameLengths.foldLeft(0)(_ max _)

      if(ils.nonEmpty) {
        INFO(s"NAME DATE TIME ACTIVE HTTP_USERNAME URL")
      }

      for(il ← ils) {
        val name = il.name.get
        val when = il.whenRegistered.get
        val isActive = il.isActive.get
        val username = il.username.get
        val url = il.url.get

        val nameSlack = " " * (maxNameLen - name.length)
        val isActiveSlack = if(isActive) "  " else " "

        INFO(s"$name$nameSlack $when $isActive$isActiveSlack $username $url")
      }
    }
  }

  @Command(name = "ls", description = "Prints the images of an image list (the most recent version of each image)")
  class Ls extends Global with NameArgument {
    def run(): Unit = {
      val revisions = vmcatcher.listImageRevisions(name)
      revisions.foreach(println)
    }
  }

  @Command(name = "activate", description = "Activates the image list in our database")
  class Activate extends Global with NameArgument {
    def run(): Unit = {
      val wasActive = vmcatcher.activateImageList(name)
      if(wasActive) { INFO(s"Already active") }
      else          { INFO(s"Activated") }
    }
  }

  @Command(name = "deactivate", description = "Deactivates the image list in our database")
  class Deactivate extends Global with NameArgument {
    def run(): Unit = {
      val wasActive = vmcatcher.deactivateImageList(name)
      if(wasActive) { INFO(s"Deactivated") }
      else          { INFO(s"Already deactive") }
    }
  }

  @Command(name = "fetch", description = "Fetches the description of the image list and parses it to images")
  class Fetch extends Global with NameArgument {
    def run(): Unit = {
      val (ref, access, revisions) = vmcatcher.fetchNewImageRevisions(name)

      if(access.isOK) {
        INFO(s"Fetched image list $name, found ${revisions.size} new image revisions")
        for {
          imageRevision ← revisions
          image ← imageRevision.f_image.obj
        } {
          INFO(s"Fetched image revision ${image.repr}")
        }
      }
      else {
        val statusCode = access.httpStatusCode.get
        val statusText = access.httpStatusText.get
        val body = access.f_rawText.obj.map(_.textData.get).getOrElse("")
        ERROR(s"Cannot fetch image list $name. HTTP returned status [$statusCode $statusText] and body [$body]")
      }
    }
  }

  @Command(name = "credentials", description = "Updates the HTTP credentials used to access a protected image list")
  class Credentials extends Global with NameArgument with CredentialsOpt {
    def run(): Unit = {
      val upOpt = UsernamePassword.optional(username, password)
      vmcatcher.updateCredentials(name, upOpt)
      if(upOpt.isDefined) { INFO(s"Credentials have been set") }
      else                { INFO(s"Credentials have been cleared") }
    }
  }
}
