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

import com.beust.jcommander.{ParametersDelegate, Parameters}
import gr.grnet.egi.vmcatcher.cmdline.common.PersonalAccessTokenDelegate
import gr.grnet.egi.vmcatcher.cmdline.helper.ImageListUrlDelegate

/**
 *
 */
@Parameters(
  commandNames = Array("get-image-list"),
  commandDescription = "Retrieves a vmcatcher-compatible, JSON-encoded image list. Helpful for debugging."
)
class GetImageList {
  @ParametersDelegate
  val urlDelegate = new ImageListUrlDelegate
  def url = urlDelegate.url

  @ParametersDelegate
  val tokenDelegate = new PersonalAccessTokenDelegate
  def token = tokenDelegate.token
}
