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

import com.beust.jcommander.{JCommander, Parameters}
import gr.grnet.egi.vmcatcher.Main

/**
 *
 */
class CmdLine {
  val globalOptions = new GlobalOptions
  val usage = new Usage

  val showEnv = new ShowEnv
  val showConf = new ShowConf

  val enqueueFromEnv = new EnqueueFromEnv
  val enqueueFromImageList = new EnqueueFromImageList
  val dequeue = new Dequeue
  val drainQueue = new DrainQueue
  val testQueue = new TestQueue

  val parseImageList = new ParseImageList
  val getImageList = new GetImageList

  val registerImageNow = new RegisterImageNow

  val registerImageList = new RegisterImageList
  val activateImageList = new ActivateImageList
  val deactivateImageList = new DeactivateImageList
  val updateCredentials = new UpdateCredentials

  val transform = new Transform
}

object CmdLine extends CmdLine {
  def nameOf(cmd: AnyRef): String = {
    val p = cmd.getClass.getAnnotation(classOf[Parameters])
    p.commandNames()(0)
  }

  private def makeJCommander: JCommander = {
    val jc = new JCommander()

    jc.setProgramName(Main.getClass.getName.dropRight(1))

    jc.addObject(CmdLine.globalOptions)
    jc.addCommand(CmdLine.usage)

    // Debugging
    jc.addCommand(CmdLine.showEnv)
    jc.addCommand(CmdLine.showConf)

    // Queues
    jc.addCommand(CmdLine.enqueueFromEnv)
    jc.addCommand(CmdLine.enqueueFromImageList)
    jc.addCommand(CmdLine.dequeue)
    jc.addCommand(CmdLine.drainQueue)
    jc.addCommand(CmdLine.testQueue)

    // Image lists
    jc.addCommand(CmdLine.registerImageList)
    jc.addCommand(CmdLine.activateImageList)
    jc.addCommand(CmdLine.deactivateImageList)
    jc.addCommand(CmdLine.updateCredentials)
    jc.addCommand(CmdLine.parseImageList)
    jc.addCommand(CmdLine.getImageList)

    // Images
    jc.addCommand(CmdLine.registerImageNow)


    // Helpers
    jc.addCommand(CmdLine.transform)

    jc
  }

  val jc = makeJCommander
}
