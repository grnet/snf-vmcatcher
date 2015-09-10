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

import com.beust.jcommander.{Parameter, ParametersDelegate, Parameters}
import gr.grnet.egi.vmcatcher.cli.common.{KamakiCloudDelegate, ConfDelegate, InsecureSSLDelegate}
import gr.grnet.egi.vmcatcher.cli.helper.{DequeueHandlerClassConverter, NotNullValueValidator}
import gr.grnet.egi.vmcatcher.image.handler.DequeueHandler

/**
 *
 */
@Parameters(
  commandNames = Array("dequeue"),
  commandDescription = "Dequeue one message from RabbitMQ and register the corresponding VM instance"
)
class Dequeue {
  @ParametersDelegate
  val insecureSSLDelegate = new InsecureSSLDelegate
  def insecureSSL = insecureSSLDelegate.insecureSSL

  @ParametersDelegate
  val confDelegate = new ConfDelegate
  def conf = confDelegate.conf

  @Parameter(names = Array("-server"), description = "Run in server mode and dequeue a message at a time")
  val server = false

  @Parameter(names = Array("-sleepMillis"), description = "Milliseconds to wait between RabbitMQ message consumption")
  val sleepMillis = 1000L

  @Parameter(
    names = Array("-handler"),
    description = "The Java class that will handle a message from RabbitMQ. Use gr.grnet.egi.vmcatcher.image.handler.SynnefoVMRegistrationHandler for the standard behavior. Other values are gr.grnet.egi.vmcatcher.image.handler.JustLogHandler and gr.grnet.egi.vmcatcher.image.handler.ThrowingHandler",
    validateValueWith = classOf[NotNullValueValidator[_]],
    converter = classOf[DequeueHandlerClassConverter]
  )
  val handler: DequeueHandler = new gr.grnet.egi.vmcatcher.image.handler.SynnefoVMRegistrationHandler

  @ParametersDelegate
  val kamakiCloudDelegate = new KamakiCloudDelegate

  def kamakiCloud = kamakiCloudDelegate.kamakiCloud
}
