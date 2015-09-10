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

package gr.grnet.egi.vmcatcher.cli.airline

import com.github.rvesse.airline._
import gr.grnet.egi.vmcatcher.LogHelper
import gr.grnet.egi.vmcatcher.cli.shell.Shell

object IaaS extends LogHelper {
  @Command(name = "describe", description = "Give a high-level descriptions of registered images")
  class Describe extends Global {
    def run(): Unit = Shell.IaaS.describe(iaas)
  }

  @Command(name = "ls", description = "List registered images")
  class Ls extends Global {
    @Option(
      name = Array("--vmcatcher-only"),
      description = "Show only the (snf-)vmcatcher-related images",
      required = false,
      arity = 0
    )
    val vmCatcherOnly = false

    def run(): Unit = Shell.IaaS.ls(iaas = iaas, vmCatcherOnly = vmCatcherOnly)
  }
}
