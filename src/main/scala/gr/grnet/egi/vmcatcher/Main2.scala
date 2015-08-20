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

import com.github.rvesse.airline.Cli
import com.github.rvesse.airline.help.Help
import com.github.rvesse.airline.parser._
import gr.grnet.egi.vmcatcher.cmdline.airline.{Global, ImageList}

/**
 *
 */
object Main2 extends LogHelper {
  val t0 = System.currentTimeMillis()
  var _args = Array[String]()
  lazy val argsDebugStr = _args.mkString(" ")

  final val ProgName = getClass.getName.stripSuffix("$")

  def beginSequence(args: Array[String]): Unit = {
    _args = args
    log.info("=" * 30)
    log.info(s"BEGIN snf-vmcatcher ($t0) [$argsDebugStr]")
  }

  def endSequence(): Unit = {
    val t1 = System.currentTimeMillis()
    val dtms = t1 - t0
    log.info(s"END snf-vmcatcher ($dtms ms) [$argsDebugStr]")
    log.info("=" * 30)
  }

  def EXIT(status: Int): Nothing = {
    log.warn(s"Exiting with status $status")
    endSequence()
    sys.exit(status)
  }

  def makeCli: Cli[Runnable] = {
    val builder = Cli.builder[Runnable](getClass.getCanonicalName.dropRight(1))

    builder.
      withCommand(classOf[Global]).
      withDefaultCommand(classOf[Help])

    val imageList = builder.withGroup("image-list")
    imageList.
      withDescription("Actions related to an image list").
      withCommands(
        classOf[ImageList.Register],
        classOf[ImageList.ShowLists],
        classOf[ImageList.ShowAccess],
        classOf[ImageList.Ls],
        classOf[ImageList.Activate],
        classOf[ImageList.Deactivate],
        classOf[ImageList.Credentials],
        classOf[ImageList.Fetch]
      )

    val image = builder.withGroup("image")
    image.
      withDescription("Actions related to an image")

    val iaas = builder.withGroup("iaas")
    iaas.
      withDescription("Actions related to the IaaS where images are uploaded/registered").
      withCommands(
        classOf[gr.grnet.egi.vmcatcher.cmdline.airline.IaaS.Describe],
        classOf[gr.grnet.egi.vmcatcher.cmdline.airline.IaaS.Ls]
      )

    builder.build()
  }

  def main(args: Array[String]): Unit = {
    beginSequence(args)

    val cli = makeCli

    try {
      val command = cli.parse(args: _*)
      command.run()
      EXIT(0)
    }
    catch {
      case e @ (_:ParseCommandMissingException |
                _:ParseCommandUnrecognizedException |
                _:ParseOptionMissingException |
                _:ParseArgumentsUnexpectedException |
                _:ParseArgumentsMissingException) ⇒
        ERROR(e.getMessage)
        EXIT(1)

      case e: VMCatcherException ⇒
        System.err.println(e.getMessage)
        log.error(s"$e", e)
        EXIT(e.code.code)

      case e: Exception ⇒
        System.err.println(e.getMessage)
        log.error("", e)
        e.printStackTrace(System.err)
        EXIT(3)

      case e: Throwable ⇒
        System.err.println(e.getMessage)
        log.error("", e)
        e.printStackTrace(System.err)
        EXIT(4)
    }
  }
}
