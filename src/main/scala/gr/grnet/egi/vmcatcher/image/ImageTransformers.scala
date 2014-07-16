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

package gr.grnet.egi.vmcatcher.image

import java.io.File

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
trait ImageTransformers {
  def transformers: List[ImageTransformer]

  def findForFile(file: File): Option[ImageTransformer] =
    transformers.find(_.canTransform(None, file)) match {
      case some @ Some(_) ⇒ some
      case None ⇒ ImageTransformers.LastResortTransformerOption
    }

  def findForFormat(format: String, file: File): Option[ImageTransformer] =
    transformers.find(_.canTransform(Some(format), file)) match {
      case some @ Some(_) ⇒ some
      case None ⇒ ImageTransformers.LastResortTransformerOption
    }
}

object ImageTransformers extends ImageTransformers {
  final val LastResortTransformerOption: Option[ImageTransformer] =
    Some(new IdentityTransformer)

  final val transformers = List(
    new VmdkTrasnformer,
    new GzTransformer,
    new Bz2Transformer,
    new OvaTransformer
  )
}
