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

package gr.grnet.egi.vmcatcher.cmdline.common

import com.beust.jcommander.Parameter
import gr.grnet.egi.vmcatcher.cmdline.helper.NotEmptyStringValidator

/**
 *
 */
class KamakiCloudDelegate {
  @Parameter(
    names = Array("-kamaki-cloud"),
    description = "The name of the cloud from ~/.kamakirc that will be used by kamaki for VM upload",
    required = true,
    validateWith = classOf[NotEmptyStringValidator]
  )
  val kamakiCloud: String = null
}
