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

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.inject.Inject

import gr.grnet.egi.vmcatcher._
import gr.grnet.egi.vmcatcher.config.Config
import io.airlift.airline.{Command, HelpOption, Option, OptionType}
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.slf4j.LoggerFactory

/**
 *
 */
@Command(name = "", description = "")
trait Global extends Runnable {
  final val log = LoggerFactory.getLogger(getClass)

  @Inject
  val helpOption: HelpOption = null

  @Option(
    `type` = OptionType.GLOBAL,
    name = Array("-c", "--config"),
    description = "The configuration file the application uses"
  )
  val path = "./config.json"

  lazy val config: Config = {
    val file = new File(path)
    if(!file.exists()) {
      throw new IllegalArgumentException(s"Configuration file $path does not exist")
    }
    else if(!file.isFile) {
      throw new IllegalArgumentException(s"Configuration file $path is not a file (!)")
    }

    val bytes = Files.readAllBytes(file.toPath)
    val json = new String(bytes, StandardCharsets.UTF_8)
    val instance = new Config()
    val schema = instance.getSchema
    val reader = new SpecificDatumReader[Config](schema)
    val decoderFactory = DecoderFactory.get()
    val jsonDecoder = decoderFactory.jsonDecoder(schema, json)
    val validatingDecoder = decoderFactory.validatingDecoder(schema, jsonDecoder)

    reader.read(instance, validatingDecoder)
  }

  lazy val iaasConfig = config.getIaasConfig

  lazy val vmcatcher: VMCatcher = new StdVMCatcher(config)
  lazy val iaas: IaaS = new KamakiBasedIaaS(config.getIaasConfig)
}
