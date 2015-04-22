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

package gr.grnet.egi.vmcatcher

import gr.grnet.egi.vmcatcher.cmdline.airline.{Global, ImageList}
import io.airlift.airline._
import io.airlift.airline.help.Help

/**
 *
 */
object Main2 extends Program {



  def makeCli: Cli[Cmd] = {
    val builder = Cli.builder[Cmd](getClass.getCanonicalName.dropRight(1))

    builder.
      withCommand(classOf[Global]).
      withDefaultCommand(classOf[Help])

    val imageList = builder.withGroup("image-list")
    imageList.
      withDescription("Actions related to an image list").
      withCommands(
        classOf[ImageList.Register],
        classOf[ImageList.ShowAll],
        classOf[ImageList.Ls],
        classOf[ImageList.Activate],
        classOf[ImageList.Deactivate],
        classOf[ImageList.Credentials],
        classOf[ImageList.Fetch]
      )

    val image = builder.withGroup("image")
    image.
      withDescription("Actions related to an image")

    builder.build()
  }

  def main(args: Array[String]): Unit = {
    val cli = makeCli

    try {
      val command = cli.parse(args: _*)
      command.run()
    }
    catch {
      case e @ (_:ParseCommandMissingException |
                _:ParseCommandUnrecognizedException |
                _:ParseOptionMissingException |
                _:ParseArgumentsUnexpectedException |
                _:ParseArgumentsMissingException) ⇒
        ERROR(e.getMessage)
        EXIT(1, endSequence)

      case e: VMCatcherException ⇒
        System.err.println(e.getMessage)
        Log.error(s"$e", e)
        EXIT(e.code.code, endSequence)

      case e: Exception ⇒
        System.err.println(e.getMessage)
        Log.error("", e)
        e.printStackTrace(System.err)
        EXIT(3, endSequence)

      case e: Throwable ⇒
        System.err.println(e.getMessage)
        Log.error("", e)
        e.printStackTrace(System.err)
        EXIT(4, endSequence)
    }
  }
}
