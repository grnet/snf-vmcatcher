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

package gr.grnet.egi.vmcatcher.cmdline.airline

import java.util.Locale

import com.typesafe.config.{ConfigRenderOptions, ConfigValueFactory, Config}
import gr.grnet.egi.vmcatcher.LogHelper
import io.airlift.airline.{Option, Command}
import scala.collection.JavaConverters._

object IaaS extends LogHelper {
  @Command(name = "describe", description = "Give a high-level descriptions of registered images")
  class Describe extends Global {
    def run(): Unit = {
      val (fromVmCatcher, otherImages) = iaas.listImages()
      INFO(s"Found ${fromVmCatcher.size} snf-vmcatcher registered images")
      INFO(s"Found ${otherImages.size}         other registered images")
    }
  }

  @Command(name = "ls", description = "List registered images")
  class Ls extends Global {
    @Option(
      name = Array("--vmcatcher-only"),
      description = "Show only the (snf-)vmcatcher-related images",
      required = false,
      arity = 0
    )
    val vmCatcherOnly = false

    def lsImage(config: Config): Unit = {
      val id = config.getString("id")
      val updated_at = config.getString("updated_at")
      val size = config.getString("size")
      val properties = config.getConfig("properties")
      val propertiesRoot = properties.root()
      val vmcatcherPropsMap =
        propertiesRoot.asScala.filter { case (k, v) ⇒
            k.toLowerCase(Locale.ENGLISH).startsWith("vmcatcher_")
        }. map { case (k, v) ⇒
          val vv = String.valueOf(propertiesRoot.get(k).unwrapped())
          k → vv
        }

      val vmcatcherPropsConfig = ConfigValueFactory.fromAnyRef(vmcatcherPropsMap.asJava)
      val jsonVmcatcherProps = vmcatcherPropsConfig.render(ConfigRenderOptions.concise())

      INFO(s"$id $updated_at $size $jsonVmcatcherProps")
    }

    def lsImages(images: List[Config]): Unit = {
      INFO(s"IAAS_UUID IAAS_UPDATED_AT SIZE JSON_PROPERTIES")
      images.foreach(lsImage)
    }

    def run(): Unit = {
      val (fromVmCatcher, otherImages) = iaas.listImages()
      INFO(s"Found ${fromVmCatcher.size} snf-vmcatcher registered images")
      lsImages(fromVmCatcher)

      if(!vmCatcherOnly) {
        INFO("")
        INFO(s"Found ${otherImages.size}         other registered images")
        lsImages(otherImages)
      }
    }
  }
}
