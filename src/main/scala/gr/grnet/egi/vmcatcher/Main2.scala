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

import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.inject.Inject

import gr.grnet.egi.vmcatcher.config.Config
import gr.grnet.egi.vmcatcher.util.UsernamePassword
import io.airlift.airline._
import io.airlift.airline.help.Help
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.slf4j.LoggerFactory

/**
 *
 */
object Main2 {
  type Cmd = Runnable

  val t0 = System.currentTimeMillis()
  var _args = Array[String]()
  lazy val argsDebugStr = _args.mkString(" ")

  final val ProgName = getClass.getName.stripSuffix("$")
  final val Log = LoggerFactory.getLogger(getClass)

  def beginSequence(args: Array[String]): Unit = {
    _args = args
    Log.info("=" * 30)
    Log.info(s"BEGIN snf-vmcatcher ($t0) [$argsDebugStr]")
  }

  def endSequence(): Unit = {
    val t1 = System.currentTimeMillis()
    val dtms = t1 - t0
    Log.info(s"END snf-vmcatcher ($dtms ms) [$argsDebugStr]")
    Log.info("=" * 30)
  }

  def ERROR(s: String): Unit = {
    System.err.println(s)
    Log.error(s)
  }

  def INFO(s: String): Unit = {
    System.err.println(s)
    Log.info(s)
  }

  def EXIT(status: Int, alsoDo: () ⇒ Any = () ⇒ ()): Nothing = {
    Log.warn(s"Exiting with status $status")
    alsoDo()
    sys.exit(status)
  }

  @Command(name = "", description = "")
  trait Global extends Cmd {
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

    lazy val vmcatcher: VMCatcher = new StdVMCatcher(config)
    lazy val iaas: IaaS = new KamakiBasedIaaS(config.getIaasConfig)
  }

  @Command(name = "register", description = "Registers the image list in our database")
  class ImageListRegister extends Global {
    @Option(
      name = Array("--name"),
      description = "The identifier for the image list; it must be unique in the database. Use this, instead of the URL, to reference the image list instead",
      required = true,
      arity = 1
    )
    val name = ""

    @Option(
      name = Array("--url"),
      description = "The URL of the image list",
      required = true,
      arity = 1
    )
    val url = ""

    @Option(
      name = Array("--username"),
      description = "Optional username in case the image list is protected. This is usually an Access Token. See also http://goo.gl/TazEI3",
      required = false,
      arity = 1
    )
    val username = ""

    @Option(
      name = Array("--password"),
      description = "Optional password, in case the image list is protected. See also the description of --username.",
      required = false,
      arity = 1
    )
    val password = ""

    @Option(
      name = Array("--activate"),
      description = "Register the image list as active (true) or inactive (false). Default is true.",
      required = false,
      arity = 1
    )
    val activate = true

    def run(): Unit = {
      val upOpt = UsernamePassword.optional(username, password)
      val ref = vmcatcher.registerImageList(name, new URL(url), activate, upOpt)
      INFO(s"Registered $ref")
    }
  }

  @Command(name = "ls", description = "Gets all registers image lists")
  class ImageListLs extends Global {
    def run(): Unit = {
      val ils = vmcatcher.getImageLists()
      val nameLengths = ils.map(_.name.get.length)
      val maxNameLen = nameLengths.foldLeft(0)(_ max _)

      if(ils.nonEmpty) {
        INFO(s"NAME DATE TIME ACTIVE HTTP_USERNAME URL")
      }

      for(il ← ils) {
        val name = il.name.get
        val when = il.whenRegistered.get
        val isActive = il.isActive.get
        val username = il.username.get
        val url = il.url.get

        val nameSlack = " " * (maxNameLen - name.length)
        val isActiveSlack = if(isActive) "  " else " "

        INFO(s"$name$nameSlack $when $isActive$isActiveSlack $username $url")
      }
    }
  }

  trait ImageListIdentifierOpt {
    @Arguments(
      description = "The identifier for the image list; it must be unique in the database. We use this, instead of the URL, to reference the image list instead",
      required = true
    )
    val name = ""
  }

  @Command(name = "activate", description = "Activates the image list in our database")
  class ImageListActivate extends Global with ImageListIdentifierOpt {
    def run(): Unit = {
      val wasActive = vmcatcher.activateImageList(name)
      if(wasActive) { INFO(s"Already active") }
      else          { INFO(s"Activated") }
    }
  }

  @Command(name = "deactivate", description = "Deactivates the image list in our database")
  class ImageListDeactivate extends Global with ImageListIdentifierOpt {
    def run(): Unit = {
      val wasActive = vmcatcher.deactivateImageList(name)
      if(wasActive) { INFO(s"Deactivated") }
      else          { INFO(s"Already deactive") }
    }
  }

  @Command(name = "credentials", description = "Updates the HTTP credentials used to access a protected image list")
  class ImageListCredentials extends Global with ImageListIdentifierOpt {
    def run(): Unit = {
      val wasActive = vmcatcher.deactivateImageList(name)
      if(wasActive) { INFO(s"Deactivated") }
      else          { INFO(s"Already deactive") }
    }
  }

  def makeCli: Cli[Cmd] = {
    val builder = Cli.builder[Cmd](getClass.getCanonicalName.dropRight(1))

    builder.
      withCommand(classOf[Global]).
      withDefaultCommand(classOf[Help])

    val imageList = builder.withGroup("image-list")
    imageList.
      withDescription("Actions related to an image list").
      withCommands(
        classOf[ImageListRegister],
        classOf[ImageListLs],
        classOf[ImageListActivate],
        classOf[ImageListDeactivate]
      )

    builder.build()
  }

  def main(args: Array[String]): Unit = {
    val cli = makeCli

    try {
      val command = cli.parse(args: _*)
      command.run()
    }
    catch {
      case e @ (_:ParseCommandMissingException |
                _:ParseCommandUnrecognizedException |
                _:ParseOptionMissingException |
                _:ParseArgumentsUnexpectedException |
                _:ParseArgumentsMissingException) ⇒
        System.err.println(e.getMessage)
        sys.exit(1)

      case e: Exception ⇒
        e.printStackTrace(System.err)
        sys.exit(2)
    }
  }
}
