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

import com.beust.jcommander.{Parameter, Parameters}
import gr.grnet.egi.vmcatcher.cmdline.helper.NotEmptyStringValidator

/**
 *
 */
@Parameters(
  commandNames = Array("fetch-image-list"),
  commandDescription = "Fetches the description of the image list and parses it to images"
)
class FetchImageList {
  @Parameter(
    names = Array("-name"),
    description = "The identifier for the image list",
    required = true,
    validateWith = classOf[NotEmptyStringValidator]
  )
  val name: String = null
}
