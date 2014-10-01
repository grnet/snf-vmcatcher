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

import com.beust.jcommander._
import gr.grnet.egi.vmcatcher.Main
import gr.grnet.egi.vmcatcher.handler.DequeueHandler

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object Args {
  def nameOf(cmd: AnyRef): String = {
    val p = cmd.getClass.getAnnotation(classOf[Parameters])
    p.commandNames()(0)
  }

  class ConfDelegate {
    @Parameter(
      names = Array("-conf"),
      description = "The configuration file the application uses",
      required = true
    )
    val conf: String = null
  }

  class KamakiCloudDelegate {
    @Parameter(
      names = Array("-kamaki-cloud"),
      description = "The name of the cloud from ~/.kamakirc that will be used by kamaki for VM upload",
      required = true,
      validateWith = classOf[NotEmptyStringValidator]
    )
    val kamakiCloud: String = null
  }

  class GlobalOptions {
    @Parameter(names = Array("-h", "-help", "--help"), help = true)
    val help = false

    @Parameter(names = Array("-v"), description = "Be verbose")
    val verbose = false
  }

  @Parameters(
    commandNames = Array("usage"),
    commandDescription = "Show usage"
  )
  class Usage

  @Parameters(
    commandNames = Array("show-env"),
    commandDescription = "Show environment variables"
  )
  class ShowEnv

  @Parameters(
    commandNames = Array("show-conf"),
    commandDescription = """Show the contents of the configuration file. Its contents must be JSON-encoded of the form:
                    rabbitmq {
                      servers = ["localhost:5672"]
                      username = "vmcatcher"
                      password = "*****"
                      queue = "vmcatcher"
                      exchange = "vmcatcher"
                      routingKey = "vmcatcher"
                      vhost = "/"
                    }"""
  )
  class ShowConf {
    @ParametersDelegate
    val confDelegate = new ConfDelegate

    def conf = confDelegate.conf
  }

  @Parameters(
    commandNames = Array("enqueue-from-env"),
    commandDescription = "Use environment variables to enqueue a VM instance message to RabbitMQ"
  )
  class EnqueueFromEnv {
    @ParametersDelegate
    val confDelegate = new ConfDelegate

    def conf = confDelegate.conf
  }

  @Parameters(
    commandNames = Array("enqueue-from-image-list"),
    commandDescription = "Use a vmcatcher-compatible, JSON-encoded image list to enqueue a VM instance message to RabbitMQ"
  )
  class EnqueueFromImageList {
    @ParametersDelegate
    val confDelegate = new ConfDelegate

    def conf = confDelegate.conf

    @Parameter(
      names = Array("-image-list-url"),
      description = "The URL of the image list. Use an http(s) URL.",
      required = true,
      validateWith = classOf[NotEmptyStringValidator],
      validateValueWith = classOf[NotNullValueValidator[_]],
      converter = classOf[URLStringConverter]
    )
    val imageListUrl: URL = null

    @Parameter(
      names = Array("-image-identifier"),
      description = "The 'dc:identifier' of the specific VM image you want to enqueue. If not given, then all VM images given in the list are enqueued.",
      validateWith = classOf[NotEmptyStringValidator]
    )
    val imageIdentifier: String = null
  }

  @Parameters(
    commandNames = Array("dequeue"),
    commandDescription = "Dequeue one message from RabbitMQ and register the corresponding VM instance"
  )
  class Dequeue {
    @ParametersDelegate
    val confDelegate = new ConfDelegate

    def conf = confDelegate.conf

    @Parameter(names = Array("-server"), description = "Run in server mode and dequeue a message at a time")
    val server = false

    @Parameter(names = Array("-sleepMillis"), description = "Milliseconds to wait between RabbitMQ message consumption")
    val sleepMillis = 1000L

    @Parameter(
      names = Array("-handler"),
      description = "The Java class that will handle a message from RabbitMQ. Use gr.grnet.egi.vmcatcher.handler.SynnefoVMRegistrationHandler for the standard behavior. Other values are gr.grnet.egi.vmcatcher.handler.JustLogHandler and gr.grnet.egi.vmcatcher.handler.ThrowingHandler",
      validateValueWith = classOf[NotNullValueValidator[_]],
      converter = classOf[DequeueHandlerClassConverter]
    )
    val handler: DequeueHandler = new gr.grnet.egi.vmcatcher.handler.SynnefoVMRegistrationHandler

    @ParametersDelegate
    val kamakiCloudDelegate = new KamakiCloudDelegate

    def kamakiCloud = kamakiCloudDelegate.kamakiCloud
  }

  @Parameters(
    commandNames = Array("register-now"),
    commandDescription = "Directly register the corresponding VM instance. This is helpful in debugging"
  )
  class RegisterNow {
    @Parameter(
      names = Array("-url"),
      description = "The http(s) URL from where to fetch the VM",
      required = true,
      validateWith = classOf[NotEmptyStringValidator],
      validateValueWith = classOf[NotNullValueValidator[_]],
      converter = classOf[URLStringConverter]
    )
    val url: URL = null

    @ParametersDelegate
    val kamakiCloudDelegate = new KamakiCloudDelegate

    def kamakiCloud = kamakiCloudDelegate.kamakiCloud
  }

  class ParsedCmdLine {
    val globalOptions = new GlobalOptions
    val usage = new Usage
    val showEnv = new ShowEnv
    val showConf = new ShowConf
    val enqueueFromEnv = new EnqueueFromEnv
    val enqueueFromImageList = new EnqueueFromImageList
    val dequeue = new Dequeue
    val registerNow = new RegisterNow
  }

  object ParsedCmdLine extends ParsedCmdLine

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
    jc.addCommand(ParsedCmdLine.registerNow)

    jc
  }

  val jc = makeJCommander
}
