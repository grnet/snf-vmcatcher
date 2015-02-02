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

import com.beust.jcommander._
import gr.grnet.egi.vmcatcher.Main

/**
 *
 */
object Args {
  def nameOf(cmd: AnyRef): String = {
    val p = cmd.getClass.getAnnotation(classOf[Parameters])
    p.commandNames()(0)
  }

  private def makeJCommander: JCommander = {
    val jc = new JCommander()

    jc.setProgramName(Main.getClass.getName.dropRight(1))

    jc.addObject(ParsedCmdLine.globalOptions)
    jc.addCommand(ParsedCmdLine.usage)

    jc.addCommand(ParsedCmdLine.showEnv)
    jc.addCommand(ParsedCmdLine.showConf)

    jc.addCommand(ParsedCmdLine.enqueueFromEnv)
    jc.addCommand(ParsedCmdLine.enqueueFromImageList)
    jc.addCommand(ParsedCmdLine.dequeue)
    jc.addCommand(ParsedCmdLine.drainQueue)
    jc.addCommand(ParsedCmdLine.testQueue)

    jc.addCommand(ParsedCmdLine.parseImageList)
    jc.addCommand(ParsedCmdLine.getImageList)

    jc.addCommand(ParsedCmdLine.registerNow)

    jc.addCommand(ParsedCmdLine.transform)

    jc
  }

  val jc = makeJCommander
}
