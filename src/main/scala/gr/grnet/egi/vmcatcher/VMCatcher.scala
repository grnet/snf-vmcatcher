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

import java.net.URL

import gr.grnet.egi.vmcatcher.db.{MCurrentImage, MImageListRef}
import gr.grnet.egi.vmcatcher.util.UsernamePassword

/**
 *
 */
trait VMCatcher {
  /**
   * Registers the details of an image list.
   */
  def registerImageList(name: String, url: URL, active: Boolean, upOpt: Option[UsernamePassword]): MImageListRef

  /**
   * Activates an image list, so that it will be retrieved and parsed when requested.
   * Returns the previous activation status.
   */
  def activateImageList(name: String): Boolean

  /**
   * Deactivates an image list.
   * Returns the previous activation status.
   */
  def deactivateImageList(name: String): Boolean

  def updateCredentials(name: String, upOpt: Option[UsernamePassword]): Unit

  /**
   * Fetches and updates the image definitions of the given image list (referenced by its name).
   */
  def updateImages(name: String): Unit

  /**
   * Returns the currently known image definitions for the given image list.
   */
  def currentImageList(name: String): List[MCurrentImage]

//  /**
//   * Fetches the image list description and parses the contained image descriptions.
//   * If everything went OK, then it returns a list of the parsed image data.
//   */
//  def fetchImageList(name: String): List[MImage]

}