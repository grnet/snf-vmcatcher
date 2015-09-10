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

import java.net.URL

import com.beust.jcommander.{Parameter, Parameters, ParametersDelegate}
import gr.grnet.egi.vmcatcher.cli.common.InsecureSSLDelegate
import gr.grnet.egi.vmcatcher.cli.helper.{NotEmptyStringValidator, NotNullValueValidator, URLStringConverter}

/**
 *
 */
@Parameters(
  commandNames = Array("transform"),
  commandDescription = "Transform a VM image to raw format"
)
class Transform {
  @Parameter(
    names = Array("-url"),
    description = "The URL from where to fetch the VM.",
    required = true,
    validateWith = classOf[NotEmptyStringValidator],
    validateValueWith = classOf[NotNullValueValidator[_]],
    converter = classOf[URLStringConverter]
  )
  val url: URL = null

  @ParametersDelegate
  val insecureSSLDelegate = new InsecureSSLDelegate
  def insecureSSL = insecureSSLDelegate.insecureSSL
}
