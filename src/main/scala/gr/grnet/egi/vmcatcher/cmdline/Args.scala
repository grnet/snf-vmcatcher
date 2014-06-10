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

import com.beust.jcommander.{JCommander, Parameters, Parameter}
import java.util.Locale
import gr.grnet.egi.vmcatcher.handler.{JustLogHandler, DequeueHandler}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object Args {
  class GlobalOptions {
    @Parameter(names = Array("-h", "-help", "--help"), help = true)
    val help = false

    @Parameter(names = Array("-v"))
    val verbose = false
  }

  @Parameters(commandDescription = "Show environment variables")
  class EnvCommand

  @Parameters(commandDescription = "Show the contents of the configuration file")
  class ConfCommand {
    @Parameter(names = Array("-conf"), description = "The configuration file the application uses")
    val conf = ""
  }

  @Parameters(commandDescription = "Use environment variables to enqueue a VM instance message to RabbitMQ")
  class EnqueueCommand

  @Parameters(commandDescription = "Dequeue one message from RabbitMQ and register the corresponding VM instance")
  class DequeueCommand {
    @Parameter(names = Array("-server"), description = "Run in server mode and dequeue a message at a time")
    val server = false

    @Parameter(names = Array("-sleepMillis"), description = "Milliseconds to wait between RabbitMQ message consumption")
    val sleepMillis = 1000L

    @Parameter(
      names = Array("-handler"),
      description = "JavaBean class that will handle on message from RabbitMQ. The default is gr.grnet.egi.vmcatcher.handler.JustLogHandler but you can specify gr.grnet.egi.vmcatcher.handler.VMRegistrationHandler for the real deal or even gr.grnet.egi.vmcatcher.handler.ThrowingHandler for debugging fun",
      converter = classOf[DequeueHandlerClassConverter]
    )
    val handler: DequeueHandler = new JustLogHandler
  }

  object Cmd {
    val globalOptions = new GlobalOptions
    val env = new EnvCommand
    val conf = new ConfCommand
    val enqueue = new EnqueueCommand
    val dequeue = new DequeueCommand
  }

  val jc = new JCommander()

  def name(ref: AnyRef) = {
    val name = ref.getClass.getName
    val nodot = name.substring(name.lastIndexOf('.') + 1)
    val nodollar = nodot.substring(nodot.lastIndexOf('$') + 1)
    val stripped = nodollar.stripSuffix("Command")
    val lower = stripped.toLowerCase(Locale.ENGLISH)
    lower
  }

  def add(ref: AnyRef): Unit = jc.addCommand(name(ref), ref)

  jc.addObject(Cmd.globalOptions)
  add(Cmd.env)
  add(Cmd.conf)
  add(Cmd.enqueue)
  add(Cmd.dequeue)
}
