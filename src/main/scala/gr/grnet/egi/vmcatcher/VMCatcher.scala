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

import gr.grnet.egi.vmcatcher.ErrorCode._
import gr.grnet.egi.vmcatcher.db._
import gr.grnet.egi.vmcatcher.util.UsernamePassword

/**
 * The API of the VMCatcher.
 */
trait VMCatcher {
  /**
   * Registers the details of an image list.
   */
  def registerImageList(name: String, url: URL, isActive: Boolean, upOpt: Option[UsernamePassword]): MImageList

  /**
   * Returns all registered image lists.
   */
  def listImageLists(): List[MImageList]

  /**
   * Finds the image list with the given name.
   */
  def findImageListRefByName(name: String): Option[MImageList]

  /**
   * Applies a function to an [[MImageList]] that is looked up by name.
   * If no such [[MImageList]] exists, throws a [[VMCatcherException]].
   */
  def forImageListByName[T](name: String)(f: (MImageList) ⇒ T): T =
    findImageListRefByName(name) match {
      case None ⇒
        throw new VMCatcherException(ImageListNotFound, s"Image list $name not found")
      case Some(ref) ⇒
        f(ref)
    }

  /**
   * Lists all the images known to have been specified in the image list of the given name.
   */
  def listImages(name: String): List[MImage]

  /**
   * Lists the latest images from the image list of the given name.
   * Latest means that they where parsed from the latest image list JSON accessed.
   */
  def listLatestImages(name: String): List[MImage]

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
  def fetchImageList(name: String): ImageListFetchResult
}

case class ImageListFetchResult(
  imageList: MImageList,
  imageListAccess: MImageListAccess,
  oldLatestImages: List[MImage],
  images: List[MImage]
)
