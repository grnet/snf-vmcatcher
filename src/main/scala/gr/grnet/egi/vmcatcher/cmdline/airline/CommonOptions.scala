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

import com.github.rvesse.airline.{Arguments, Option}

trait CommonOptions {
  trait NameArgument {
    @Arguments(
      description = "The identifier for the image list; it must be unique in the database. We use this, instead of the URL, to reference the image list instead",
      required = true
    )
    val name = ""
  }

  trait CredentialsOpt {
    @Option(
      name = Array("--username"),
      description = "Optional username in case the image list is protected. This is usually an Access Token. See also http://goo.gl/TazEI3",
      required = false,
      arity = 1
    )
    val username = ""

    @Option(
      name = Array("--password"),
      description = "Optional password, in case the image list is protected. See also the description of --username.",
      required = false,
      arity = 1
    )
    val password = ""
  }
}
