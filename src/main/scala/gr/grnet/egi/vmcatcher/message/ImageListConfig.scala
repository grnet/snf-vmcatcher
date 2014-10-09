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
package message

import scala.collection.JavaConverters._

/**
 *
 */
class ImageListConfig(val config: com.typesafe.config.Config)
  extends ConfigWrapper
  with    ConfigWrapperDcIdentifier
  with    ConfigWrapperDcTitle
  with    ConfigWrapperHvURI
  with    ConfigWrapperHvVersion {

  def images: List[ImageConfig] = {
    val jconfigList = config.getConfigList(fk("hv:images"))
    val configList = jconfigList.asScala.map(_.getConfig(fk("hv:image"))).toList
    configList.map(new ImageConfig(_))
  }

  def projectImageDcIdentifier(ident: String): List[ImageConfig] =
    ident match {
      case null | "" ⇒ images
      case _ ⇒ images.find(_.dcIdentifier == ident).toList
    }
}

object ImageListConfig {
  def ofString(s: String): ImageListConfig = {
    val config = Config.ofString(s).getConfig(Config.fk("hv:imagelist"))
    new ImageListConfig(config)
  }
}
