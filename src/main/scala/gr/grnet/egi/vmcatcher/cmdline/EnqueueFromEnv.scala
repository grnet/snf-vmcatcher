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

import com.beust.jcommander.{ParametersDelegate, Parameters}
import gr.grnet.egi.vmcatcher.cmdline.common.ConfDelegate

/**
 *
 */
@Parameters(
  commandNames = Array("enqueue-from-env"),
  commandDescription = "Use environment variables to enqueue a VM instance message to RabbitMQ." +
    " This is the original use-case and reflects how vmcatcher (the original software)" +
    " works."
)
class EnqueueFromEnv

