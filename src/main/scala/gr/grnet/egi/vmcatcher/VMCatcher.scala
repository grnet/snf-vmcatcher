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

import gr.grnet.egi.vmcatcher.db._
import gr.grnet.egi.vmcatcher.util.UsernamePassword

/**
 * The API of the VMCatcher.
 */
trait VMCatcher {
  /**
   * Registers the details of an image list.
   */
  def registerImageList(name: String, url: URL, isActive: Boolean, upOpt: Option[UsernamePassword]): MImageListRef

  /**
   * Returns all registered image lists.
   */
  def listImageLists(): List[MImageListRef]

  /**
   * Lists the image revisions of an image list (which has the given name).
   */
  def listImageRevisions(name: String): List[MImageRevision]

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
   * Returns any new image revisions.
   */
  def fetchNewImageRevisions(name: String): (MImageListRef, MImageListAccess, List[MImageRevision])

  /**
   * Returns the image descriptions of an image list, as they were more recently parsed.
   */
  def listImageList(name: String): List[MImage]

  /**
   * Returns the currently known image definitions for the given image list.
   */
  def currentImageList(name: String): List[MCurrentImage]

  /**
   * Get the image with the given dc:identifier
   */
  def getImage(dcIdentifier: String): MImage
}
