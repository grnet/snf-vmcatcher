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

import com.beust.jcommander.{Parameter, ParametersDelegate}
import gr.grnet.egi.vmcatcher.cli.common.ConfDelegate
import gr.grnet.egi.vmcatcher.cli.helper.NotEmptyStringValidator

/**
 *
 */
class GlobalOptions {
  @Parameter(names = Array("-h", "-help", "--help"), help = true)
  val help = false

  @Parameter(names = Array("-v"), description = "Be verbose")
  val verbose = false

  @Parameter(
    names = Array("-working-folder"),
    description = "The working folder were images are downloaded and transformed",
    validateWith = classOf[NotEmptyStringValidator]
  )
  val workingFolder = "/mnt/tmp"

  @ParametersDelegate
  private[this] val confDelegate = new ConfDelegate
  def config = confDelegate.conf
}
