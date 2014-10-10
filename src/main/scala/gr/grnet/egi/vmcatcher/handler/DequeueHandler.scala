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

package gr.grnet.egi.vmcatcher.handler

import gr.grnet.egi.vmcatcher.event.Event
import gr.grnet.egi.vmcatcher.image.ImageTransformers
import org.slf4j.Logger

/**
 * Handles one event from the queue.
 *
 */
trait DequeueHandler {
  def handle(
    log: Logger,
    event: Event,
    kamakiCloud: String,
    imageTransformers: ImageTransformers
  ): Unit
}
