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

package gr.grnet.egi.vmcatcher.cmdline

import java.net.URL

import com.beust.jcommander.{Parameter, Parameters}
import gr.grnet.egi.vmcatcher.cmdline.helper.{URLStringConverter, NotNullValueValidator, NotEmptyStringValidator}

/**
 *
 */
@Parameters(
  commandNames = Array("register-image-list"),
  commandDescription = "Register the image list in our database"
)
class RegisterImageList {
  @Parameter(
    names = Array("-name"),
    description = "The identifier for the image list; it must be unique in the database. Use this, instead of the URL, to reference the image list instead",
    required = true,
    validateWith = classOf[NotEmptyStringValidator]
  )
  val name: String = null

  @Parameter(
    names = Array("-url"),
    description = "The URL of the image list",
    required = true,
    validateWith = classOf[NotEmptyStringValidator],
    converter = classOf[URLStringConverter]
  )
  val url: URL = null

  @Parameter(
    names = Array("-username"),
    description = "Optional username in case the image list is protected. This is usually an Access Token. See also https://wiki.appdb.egi.eu/main:faq:how_to_subscribe_to_a_private_image_list_using_the_vmcatcher",
    validateWith = classOf[NotEmptyStringValidator]
  )
  val username: String = null

  @Parameter(
    names = Array("-password"),
    description = "Optional password, in case the image list is protected. See also https://wiki.appdb.egi.eu/main:faq:how_to_subscribe_to_a_private_image_list_using_the_vmcatcher",
    validateWith = classOf[NotEmptyStringValidator]
  )
  val password: String = "x-oauth-basic"
}
