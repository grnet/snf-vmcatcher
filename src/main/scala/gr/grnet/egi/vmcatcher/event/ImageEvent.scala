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

package gr.grnet.egi.vmcatcher.event

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import gr.grnet.egi.vmcatcher.util.TConfig

import scala.collection.JavaConverters._

case class ImageView(config: Config) {
  lazy val json = ImageView.getJson(config)
  lazy val map  = ImageView.getStringMap(config)

  def has(key: String): Boolean = map.contains(key)
  def apply(key: String, default: String): String = map.getOrElse(key, default)
  def get(key: String): Option[String] = map.get(key)
}

object ImageView {
  def getStringMap(config: Config): Map[String, String] =
    config.root().unwrapped().asScala.map { case (k, v) ⇒ (k, String.valueOf(v)) }.toMap

  def getJson(config: Config): String =
    config.root().render(ConfigRenderOptions.concise().setFormatted(true))
  
}

/**
 * Provides two views of the image event and convenient methods to obtain the contained fields.
 */
case class ImageEvent(
  originator: EventOriginator,
  envFieldsView: ImageView,
  imageJsonView: ImageView
) {
  def has(field: String): Boolean = envFieldsView.has(field) || imageJsonView.has(field)

  def has(field: IEnvField): Boolean = envFieldsView.has(field.name())

  def apply(field: String, default: String): String =
    imageJsonView(field, envFieldsView(field, default))

  def apply(field: IEnvField, default: String): String =
    envFieldsView(field.name(), default)

  def get(field: String): Option[String] =
    imageJsonView.get(field).fold(envFieldsView.get(field))(Some(_))

  def get(field: IEnvField): Option[String] =
    envFieldsView.get(field.name())
}

object ImageEvent {
  def AllFieldNames = EnvFieldCategory.allFields().asScala.map(_.name()).toSeq
  def SysEnvFieldPairs = {
    val SysEnv = sys.env
    AllFieldNames.flatMap(name ⇒ SysEnv.get(name).map(value ⇒ (name, value)))
  }
  def SysEnvFields = Map(SysEnvFieldPairs:_*)

  /**
   * We expect event field names in the environment.
   * We pick them up and create the respective event.
   * This is the normal route if we get data from the original vmcatcher, via the environment.
   * @return
   */
  def ofSysEnvFields: ImageEvent = {
    val sysEnvFieldsMap = SysEnvFields

    // We have the map with keys that look like environment variables of a Unix shell
    val fromEnvFields = ImageView(ConfigFactory.parseMap(sysEnvFieldsMap.asJava))

    // Compute the respective map with keys now resembling json attributes from an image list
    val jsonValuesFieldsMap =
      for {
        (fieldName, fieldValue) ← sysEnvFieldsMap
      } yield {
        (EnvFieldCategory.imageListJsonAttributeOfEnvField(fieldName), fieldValue)
      }
    val fromImageJson = ImageView(ConfigFactory.parseMap(jsonValuesFieldsMap.asJava))

    ImageEvent(
      originator = EventOriginator.vmcatcher_sysenv,
      envFieldsView = fromEnvFields,
      imageJsonView = fromImageJson
    )
  }

  def ofEnvFieldsConfig(config: Config): ImageEvent = {
    // We have the map with keys that look like environment variables of a Unix shell
    val fromEnvFields = ImageView(config)

    val envFieldsMap = config.root().unwrapped().asScala.map { case (k, v) ⇒ (k, s"$v") }
    // Compute the respective map with keys now resembling json attributes from an image list
    val jsonValuesFieldsMap =
      for {
        (fieldName, fieldValue) ← envFieldsMap
      } yield {
        (EnvFieldCategory.imageListJsonAttributeOfEnvField(fieldName), fieldValue)
      }
    val fromImageJson = ImageView(ConfigFactory.parseMap(jsonValuesFieldsMap.asJava))

    ImageEvent(
      originator = EventOriginator.vmcatcher_sysenv,
      envFieldsView = fromEnvFields,
      imageJsonView = fromImageJson
    )
  }

  def ofEnvFieldsJson(json: String): ImageEvent = ofEnvFieldsConfig(ConfigFactory.parseString(json))

  def ofImageJsonConfig(config: Config): ImageEvent = {
    val fromImageJson = ImageView(config)

    // We have the map with keys that are the json attributes from the image list
    val jsonFieldsMap = fromImageJson.map

    // We compute the map with the respective, environment-variable-like keys
    val envFieldsMap =
      for {
        (jsonAttr, fieldValue) ← jsonFieldsMap if EnvFieldCategory.existsImageListJsonAttribute(jsonAttr)
      } yield {
        (EnvFieldCategory.envFieldOfImageListJsonAttribute(jsonAttr).name(), fieldValue)
      }
    val fromEnvFields = ImageView(ConfigFactory.parseMap(envFieldsMap.asJava))

    ImageEvent(
      originator = EventOriginator.image_list_json,
      envFieldsView = fromEnvFields,
      imageJsonView = fromImageJson
    )
  }

  def ofImageJson(json: String): ImageEvent = ofImageJsonConfig(ConfigFactory.parseString(json))

  def parseImageListJson(imageListJson: String): List[ImageEvent] = {
    /////////////////
    // `json` goes like this:
    // {
    //    "hv:imagelist": {
    //      "dc:identifier": "302EC38C-4EF8-4C02-96A5-BF8E93D27FEA",
    //      "dc:source": "https://SOURCE",
    //      "dc:title": "VO SAMPLE image list.",
    //      "ad:vo": "VO",
    //      "hv:endorser": {
    //      "hv:x509": {...}
    //    },
    //      "hv:images": [
    //    {
    //      "hv:image": {
    //      "dc:description": "Image description",
    //      "dc:identifier": "24910A01-D1AC-49BE-B253-7746ED8F0DC0",
    //      "ad:mpuri": "https://SOURCE/images/24910A01-D1AC-49BE-B253-7746ED8F0DC0:89/",
    //      "dc:title": "Basic Ubuntu Server 12.04 LTS OS Disk Image",
    //      "ad:group": "Basic Ubuntu Server 12.04 LTS OS",
    // ...
    // }
    /////////////////

    // Fix the Key.
    // _       _
    // the TConfig library does not like the ':' token inside keys, unless it is quoted.
    def fk(key: String) = key.replaceAll(":", "\":\"")

    val imageListConfig = TConfig.ofString(imageListJson).getConfig(fk("hv:imagelist"))
    val imagesConfigList = imageListConfig.getConfigList(fk("hv:images"))
    val imageConfigList: List[Config] = imagesConfigList.asScala.map(_.getConfig(fk("hv:image"))).toList

    for {
      imageConfig ← imageConfigList
    } yield {
      ofImageJsonConfig(imageConfig)
    }
  }
}
