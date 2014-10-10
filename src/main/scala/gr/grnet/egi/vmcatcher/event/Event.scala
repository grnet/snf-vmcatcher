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

import com.typesafe.config.{Config ⇒ TConfig}
import gr.grnet.egi.vmcatcher.event.EventFieldSection.{Image, ImageList}
import gr.grnet.egi.vmcatcher.{Main, Config, Json}

import scala.collection.JavaConverters._

/**
 *
 */
sealed trait Event {
  def originator: EventOriginator

  def has(field: IEventField): Boolean

  def apply(field: IEventField, default: String = ""): String

  def get(field: IEventField): Option[String] = if(has(field)) Some(apply(field)) else None

  def toMap: Map[String, String]

  def toJson: String = Json.jsonOfMap(toMap)

  override def toString = toJson
}

/**
 * An [[gr.grnet.egi.vmcatcher.event.Event]] produced by a map.
 * @param map
 */
class MapEvent(val originator: EventOriginator, map: Map[String, String]) extends Event {
  def has(field: IEventField) = map.contains(field.name)

  def apply(field: IEventField, default: String = "") = map.getOrElse(field.name, default)

  override def get(field: IEventField): Option[String] = map.get(field.name)

  def toMap: Map[String, String] = map
}

object Event {
  def SysEnv = sys.env
  def AllFieldNames = EventFieldSection.allFields().asScala.map(_.name()).toSeq
  def ExistingFieldPairs = AllFieldNames.flatMap(name ⇒ SysEnv.get(name).map(value ⇒ (name, value)))
  def ExistingFields = Map(ExistingFieldPairs:_*)

  def ofSysEnv: Event = new MapEvent(EventOriginator.vmcatcher_sysenv, ExistingFields)

  /**
   *
   * @param json As submitted to the queue
   * @return
   */
  def ofJson(json: String): Event = {
    val map = Json.stringMapOfJson(json)
    new MapEvent(EventOriginator.vanilla_json, map)
  }


  // Fix the Key.
  // _       _
  // the Config library does not like the ':' token inside keys, unless it is quoted.
  def fk(key: String) = key.replaceAll(":", "\":\"")
}

object Events {
  import Event.fk

  def ofImageListContainer(
    json: String,
    externalFieldsMap: Map[ExternalEventField, String]
  ): List[Event] = {

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

    val externalMap = externalFieldsMap.map { case (k, v) ⇒ (k.name(), v) }

    val imageListConfig = Config.ofString(json).getConfig(fk("hv:imagelist"))
    val imageListMap = {
      val jsonFieldNames = ImageList.getJsonFieldNames.asScala.toSet

      // Keep only relevant field names as keys
      // and then translate them to vmcatcher variable names
      Config.
        stringMapOfFilteredFields(imageListConfig, jsonFieldNames).
        map { case (k, v) ⇒ (ImageListEventField.ofJsonField(k).name(), v) }
    }

    val imagesConfigList = imageListConfig.getConfigList(fk("hv:images"))
    val imageConfigList = imagesConfigList.asScala.map(_.getConfig(fk("hv:image"))).toList

    for {
      imageConfig ← imageConfigList
    } yield {
      val jsonFieldNames = Image.getJsonFieldNames.asScala.toSet

      // Keep only relevant field names as keys
      // and then translate them to vmcatcher variable names
      val imageMap = Config.
        stringMapOfFilteredFields(imageConfig, jsonFieldNames).
        map { case (k, v) ⇒ (ImageEventField.ofJsonField(k).name(), v) }

      val map = externalMap ++ imageListMap ++ imageMap
      val event = new MapEvent(EventOriginator.image_list_json, map)

      Main.Log.info(s"event = $event")
      event
    }
  }
}

/**
 * An [[gr.grnet.egi.vmcatcher.event.Event]] produced by an image list.
 *
 * @param originator
 * @param imageListConfig
 * @param imageConfig
 * @param externalFields
 */
class ImageConfigEvent(
  val originator: EventOriginator,
  imageListConfig: TConfig,
  imageConfig: TConfig,
  externalFields: Map[ExternalEventField, String]
) extends Event {

  def has(field: IEventField) = {
    lazy val path = Event.fk(field.jsonField)

    field.section match {
      case ImageList ⇒
        imageListConfig.hasPath(path)

      case Image ⇒
        imageConfig.hasPath(path)

      case EventFieldSection.External ⇒
        field match {
          case external: ExternalEventField ⇒
            externalFields.contains(external)

          case _ ⇒
            false
        }
    }
  }

  def apply(field: IEventField, default: String = "") = {
    lazy val path = Event.fk(field.jsonField)
    def getOrElse(config: TConfig) = if(config.hasPath(path)) config.getString(path) else default

    field.section match {
      case ImageList ⇒
        getOrElse(imageListConfig)

      case Image ⇒
        getOrElse(imageConfig)

      case EventFieldSection.External ⇒
        field match {
          case external: ExternalEventField ⇒
            externalFields.getOrElse(external, default)

          case _ ⇒
            default
        }
    }
  }

  def toMap: Map[String, String] = {
    val imageMap = Config.toMap(imageConfig)
    val imageListMap = Config.toMap(imageListConfig)
    val externalMap = externalFields.map{ case (k, v) ⇒ (k.name, v) }

    imageMap ++ imageListMap ++ externalMap
  }
}
