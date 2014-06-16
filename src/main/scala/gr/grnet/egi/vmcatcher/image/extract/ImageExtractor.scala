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

package gr.grnet.egi.vmcatcher.image.extract

import java.io.File

import org.slf4j.Logger

/**
 * Extract an image to the format supported by `kamaki`, that is raw.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
trait ImageExtractor {
  def canExtract(format: String): Boolean
  def extract(log: Logger, map: Map[String, String], format: String, imageFile: File): File
}

object ImageExtractor {
  /**
   * A list of all known image extractors.
   */
  val AllKnown: List[ImageExtractor] = List(
    new CpioBz2Extractor,
    new CpioGzExtractor
  )

  def findExtractor(format: String): Option[ImageExtractor] = AllKnown.find(_.canExtract(format))
}
