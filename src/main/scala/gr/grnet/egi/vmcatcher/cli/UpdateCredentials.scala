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

package gr.grnet.egi.vmcatcher.cli

import com.beust.jcommander.{Parameter, Parameters}
import gr.grnet.egi.vmcatcher.cli.helper.NotEmptyStringValidator

/**
 *
 */
@Parameters(
  commandNames = Array("update-credentials"),
  commandDescription = "Updates the HTTP credentials used to access a protected image list"
)
class UpdateCredentials {
  @Parameter(
    names = Array("-name"),
    description = "The identifier for the image list",
    required = true,
    validateWith = classOf[NotEmptyStringValidator]
  )
  val name: String = null

  @Parameter(
    names = Array("-username"),
    description = "The username. This is usually an Access Token. See also https://wiki.appdb.egi.eu/main:faq:how_to_subscribe_to_a_private_image_list_using_the_vmcatcher",
    validateWith = classOf[NotEmptyStringValidator]
  )
  val username: String = null

  @Parameter(
    names = Array("-password"),
    description = "The password password. See also https://wiki.appdb.egi.eu/main:faq:how_to_subscribe_to_a_private_image_list_using_the_vmcatcher",
    validateWith = classOf[NotEmptyStringValidator]
  )
  val password: String = "x-oauth-basic"
}
