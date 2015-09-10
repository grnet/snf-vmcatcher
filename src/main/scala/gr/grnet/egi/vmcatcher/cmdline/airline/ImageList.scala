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

import com.github.rvesse.airline._
import gr.grnet.egi.vmcatcher.LogHelper
import gr.grnet.egi.vmcatcher.shell.Shell

object ImageList extends LogHelper with CommonOptions {

  /** Registers the image list in our database. */
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

    def run(): Unit =
      Shell.ImageList.register(
        vmcatcher = vmcatcher,
        username = username,
        password = password,
        name = name,
        url = url,
        activate = activate
      )
  }

  @Command(name = "show-lists", description = "Show the known image lists")
  class ShowLists extends Global {
    def run(): Unit = Shell.ImageList.showLists(vmcatcher)
  }

  @Command(name = "show-access", description = "Prints the JSON definition of the latest image list access")
  class ShowAccess extends Global with NameArgument {
    def run(): Unit = Shell.ImageList.showAccess(vmcatcher, name)
  }

  @Command(name = "ls", description = "Prints the images of an image list (the most recent version of each image)")
  class Ls extends Global with NameArgument {
    def run(): Unit = Shell.ImageList.ls(vmcatcher, name)
  }

  @Command(name = "activate", description = "Activates the image list in our database")
  class Activate extends Global with NameArgument {
    def run(): Unit = Shell.ImageList.activate(vmcatcher, name)
  }

  @Command(name = "deactivate", description = "Deactivates the image list in our database")
  class Deactivate extends Global with NameArgument {
    def run(): Unit = Shell.ImageList.deactivate(vmcatcher, name)
  }

  @Command(name = "fetch", description = "Fetches the description of the image list and parses it to images")
  class Fetch extends Global with NameArgument {
    def run(): Unit = Shell.ImageList.fetch(vmcatcher, name)
  }

  @Command(name = "credentials", description = "Updates the HTTP credentials used to access a protected image list")
  class Credentials extends Global with NameArgument with CredentialsOpt {
    def run(): Unit =
      Shell.ImageList.credentials(
        vmcatcher = vmcatcher,
        username = username,
        password = password,
        name = name
      )
  }
}
