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

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class ImageConfig(val config: com.typesafe.config.Config)
  extends ConfigWrapper
  with    ConfigWrapperDcIdentifier
  with    ConfigWrapperDcTitle
  with    ConfigWrapperHvURI
  with    ConfigWrapperHvVersion {

  def hvFormat     = getString("hv:format")

  def slArch      = getString("sl:arch")
  def slComments  = getString("sl:comments")
  def slOS        = getString("sl:os")
  def slOSName    = getString("sl:osname")
  def slOSVersion = getString("sl:osversion")
}

object ImageConfig {
  def ofString(json: String): ImageConfig = {
    val config = Config.ofString(json)
    val imageConfig = new ImageConfig(config)
    imageConfig
  }
}