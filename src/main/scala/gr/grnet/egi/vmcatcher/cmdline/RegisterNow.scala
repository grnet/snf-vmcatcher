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

import com.beust.jcommander.{ParametersDelegate, Parameter, Parameters}
import gr.grnet.egi.vmcatcher.cmdline.common.{KamakiCloudDelegate, InsecureSSLDelegate}
import gr.grnet.egi.vmcatcher.cmdline.helper.{URLStringConverter, NotNullValueValidator, NotEmptyStringValidator}

/**
 *
 */
@Parameters(
  commandNames = Array("register-now"),
  commandDescription = "Directly register the corresponding VM instance. This is helpful in debugging"
)
class RegisterNow {
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

  @ParametersDelegate
  val kamakiCloudDelegate = new KamakiCloudDelegate
  def kamakiCloud = kamakiCloudDelegate.kamakiCloud

  @Parameter(
    names = Array("-osfamily"),
    description = "The OS family, e.g. 'linux' or 'windows'",
    required = true,
    validateWith = classOf[NotEmptyStringValidator],
    validateValueWith = classOf[NotNullValueValidator[_]]
  )
  val osfamily = "linux"

  @Parameter(
    names = Array("-users"),
    description = "The OS 'users' that will become a field in the metafile properties",
    required = true,
    validateWith = classOf[NotEmptyStringValidator],
    validateValueWith = classOf[NotNullValueValidator[_]]
  )
  val users = "root"

  @Parameter(
    names = Array("-format"),
    description = "Use this VM format if none can be automatically discovered",
    required = false,
    validateWith = classOf[NotEmptyStringValidator],
    validateValueWith = classOf[NotNullValueValidator[_]]
  )
  val format: String = null
}
