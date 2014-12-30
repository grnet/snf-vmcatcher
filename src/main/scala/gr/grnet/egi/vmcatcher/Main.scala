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
import java.nio.charset.StandardCharsets
import java.util.Scanner

import com.beust.jcommander.ParameterException
import com.squareup.okhttp._
import com.typesafe.config.ConfigRenderOptions
import gr.grnet.egi.vmcatcher.cmdline.Args
import gr.grnet.egi.vmcatcher.cmdline.Args.ParsedCmdLine
import gr.grnet.egi.vmcatcher.event._
import gr.grnet.egi.vmcatcher.image.transformer.ImageTransformers
import gr.grnet.egi.vmcatcher.rabbit.{Rabbit, RabbitConnector}
import okio.Okio
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

/**
 *
 */
object Main extends {
  final val ProgName = getClass.getName.stripSuffix("$")
  final val Log = LoggerFactory.getLogger(getClass)
  
  def EXIT(status: Int): Nothing = {
    Log.warn(s"Exiting with $status")
    sys.exit(status)
  }

  val configRenderOptions =
    ConfigRenderOptions.defaults().
      setComments(true).
      setOriginComments(false).
      setFormatted(true).
      setJson(true)

  val commandMap = Map(
    Args.nameOf( ParsedCmdLine.usage                ) → DEFER { do_usage                  ( ParsedCmdLine.usage                ) },
    Args.nameOf( ParsedCmdLine.showEnv              ) → DEFER { do_show_env               ( ParsedCmdLine.showEnv              ) },
    Args.nameOf( ParsedCmdLine.showConf             ) → DEFER { do_show_conf              ( ParsedCmdLine.showConf             ) },
    Args.nameOf( ParsedCmdLine.enqueueFromEnv       ) → DEFER { do_enqueue_from_env       ( ParsedCmdLine.enqueueFromEnv       ) },
    Args.nameOf( ParsedCmdLine.enqueueFromImageList ) → DEFER { do_enqueue_from_image_list( ParsedCmdLine.enqueueFromImageList ) },
    Args.nameOf( ParsedCmdLine.dequeue              ) → DEFER { do_dequeue                ( ParsedCmdLine.dequeue              ) },
    Args.nameOf( ParsedCmdLine.registerNow          ) → DEFER { do_register_now           ( ParsedCmdLine.registerNow          ) },
    Args.nameOf( ParsedCmdLine.parseImageList       ) → DEFER { do_parse_image_list       ( ParsedCmdLine.parseImageList       ) },
    Args.nameOf( ParsedCmdLine.getImageList         ) → DEFER { do_get_image_list         ( ParsedCmdLine.getImageList         ) },
    Args.nameOf( ParsedCmdLine.drainQueue           ) → DEFER { do_drain_queue            ( ParsedCmdLine.drainQueue           ) },
    Args.nameOf( ParsedCmdLine.transform            ) → DEFER { do_transform              ( ParsedCmdLine.transform            ) }
  )

  def stringOfConfig(config: com.typesafe.config.Config) = config.root().render(configRenderOptions)

  def env: Map[String, String] = sys.env
  def envAsJson = Json.jsonOfMap(env)
  def envAsPrettyJson: String =  Json.jsonOfMap(env)

  def isVerbose = ParsedCmdLine.globalOptions.verbose
  def isHelp    = ParsedCmdLine.globalOptions.help
  def isServer  = ParsedCmdLine.dequeue.server
  def serverSleepMillis = ParsedCmdLine.dequeue.sleepMillis max 0L min 1000L
  def dequeueHandler = ParsedCmdLine.dequeue.handler

  def configOfPath(path: String) = {
    Log.info(s"Load conf from $path")
    Config.ofFilePath(path)
  }

  def do_usage(args: Args.Usage): Unit = Args.jc.usage()

  def do_show_env(args: Args.ShowEnv): Unit = println(envAsPrettyJson)

  def do_show_conf(args: Args.ShowConf): Unit = println(stringOfConfig(configOfPath(args.conf)))

  def do_enqueue(connector: RabbitConnector): Unit = {
    val event = Event.ofSysEnv
    Log.info(s"event (sysenv) = $event")
    val json = event.toJson

    val rabbit = connector.connect()

    rabbit.publish(json)
    rabbit.close()
  }
  
  def do_enqueue_from_env(args: Args.EnqueueFromEnv): Unit = {
    val connector = RabbitConnector(configOfPath(args.conf))
    do_enqueue(connector)
  }

  def parseImageListContainerJson(rawImageList: String): String = {
    val scanner = new Scanner(rawImageList)
    scanner.nextLine() // Ignore "MIME-Version: 1.0" first line
    scanner.useDelimiter("boundary=\"")
    val boundaryPart = scanner.findInLine("boundary=\"(.+?)\"")
    val boundary = "--" + boundaryPart.substring("boundary=\"".length, boundaryPart.length - 1)
    Log.info(s"Found boundary $boundary")

    val buffer = new java.lang.StringBuilder

    @tailrec def scanUntilFirstBoundary(): Unit = {
      val nextLine = scanner.nextLine()
      if(nextLine != boundary) scanUntilFirstBoundary()
    }

    @tailrec def scanUntilLastBoundary(): Unit = {
      val nextLine = scanner.nextLine()
      if(nextLine != boundary) {
        buffer.append(nextLine + System.getProperty("line.separator"))
        scanUntilLastBoundary()
      }
    }

    scanUntilFirstBoundary()
    scanUntilLastBoundary()

    buffer.toString
  }

  def urlToUtf8(url: URL, tokenOpt: Option[String]): String = {
    val urlConnection = url.openConnection()
    for(token ← tokenOpt) {

      // The password seems to be fixed, as described in
      //   https://wiki.appdb.egi.eu/main:faq:how_to_subscribe_to_a_private_image_list_using_the_vmcatcher
      // TODO Make configurable
      val username = token
      val password = "x-oauth-basic"

      val credential = Credentials.basic(username, password)
      urlConnection.setRequestProperty("Authorization", credential)
    }

    urlConnection.connect()
    val stream = urlConnection.getInputStream

    val source = Okio.source(stream)
    val buffer = Okio.buffer(source)
    val str = buffer.readUtf8()
    stream.close()
    str
  }

  def do_parse_image_list(args: Args.ParseImageList): Unit = {
    val imageListContainerURL = args.imageListUrl
    val token = args.token
    val rawImageListContainer = urlToUtf8(imageListContainerURL, Option(token))
    Log.info (s"imageListContainer (URL ) = $imageListContainerURL")
    Log.debug(s"imageListContainer (raw ) =\n$rawImageListContainer")
    val imageListContainerJson = parseImageListContainerJson(rawImageListContainer)
    Log.info (s"imageListContainer (json) =\n$imageListContainerJson")
    val events0 = Events.ofImageListContainer(imageListContainerJson, Map())
    Log.info(s"Found ${events0.size} events")
    val identifiers = events0.map(_(ImageEventField.VMCATCHER_EVENT_DC_IDENTIFIER))
    Log.info(s"Image identifiers are: ${identifiers.mkString(", ")}")
  }

  def do_get_image_list(args: Args.GetImageList): Unit = {
    val imageListContainerURL = args.imageListUrl
    val token = args.token
    val rawImageListContainer = urlToUtf8(imageListContainerURL, Option(token))
    Log.info (s"imageListContainer (URL ) = $imageListContainerURL")
    val imageListContainerJson = parseImageListContainerJson(rawImageListContainer)
    Log.info (s"imageListContainer (json) =\n$imageListContainerJson")
    System.err.println(imageListContainerJson)
  }

  def do_enqueue_from_image_list(args: Args.EnqueueFromImageList): Unit = {
    val imageListURL = args.imageListUrl
    val imageIdentifier = args.imageIdentifier
    val tokenOpt = Option(args.token)

    val rawImageList = urlToUtf8(imageListURL, tokenOpt)
    Log.info(s"imageList (URL) = $imageListURL")
    Log.info(s"imageList (raw) = $rawImageList")
    val jsonImageList = parseImageListContainerJson(rawImageList)
    val events0 = Events.ofImageListContainer(jsonImageList, Map())

    events0 match {
      case Nil ⇒
        Log.error(s"Could not parse events from image list")
        EXIT(4)

      case event :: _ ⇒
        val dcIdentifier = event(ImageListEventField.VMCATCHER_EVENT_IL_DC_IDENTIFIER)
        Log.info(s"Parsed image list dc:identifier = $dcIdentifier")
        val events =
          if(imageIdentifier eq null)
            events0
          else
            events0.filter(_(ImageEventField.VMCATCHER_EVENT_DC_IDENTIFIER) == imageIdentifier)

        if(events.isEmpty) {
          Log.error(s"Image identifier $imageIdentifier not found")
          val identifiers = events0.map(_(ImageEventField.VMCATCHER_EVENT_DC_IDENTIFIER))
          Log.info(s"Available identifiers are: ${identifiers.mkString(", ")}")
          EXIT(3)
        }

        Log.info(s"Matched ${events.size} event(s)")

        val connector = RabbitConnector(configOfPath(args.confDelegate.conf))
        val rabbit = connector.connect()

        for {
          event ← events
        } {
          val imageIdent = event(ImageEventField.VMCATCHER_EVENT_DC_IDENTIFIER)
          val imageURI = event(ImageEventField.VMCATCHER_EVENT_HV_URI)
          Log.info(s"Enqueueing event for dc:identifier = $imageIdent, hv:uri = $imageURI")
          Log.info(s"event (image) = $event")

          rabbit.publish(event.toJson)
        }

        rabbit.close()
    }
  }

  def do_dequeue(connector: RabbitConnector, kamakiCloud: String): Unit = {
    /**
     * This is the main server loop.
     */
    @tailrec
    def serverLoop(rabbit: Rabbit, isEmpty: Boolean): Unit = {
      val newIsEmpty =
        rabbit.getAndAck {
          if(!isEmpty) { Log.info("Queue is empty") }
          true
        } { response ⇒
          val jsonMsgBytes = response.getBody
          val jsonMsg = new String(jsonMsgBytes, StandardCharsets.UTF_8)
          val event = Event.ofJson(jsonMsg)

          Log.info(s"dequeueHandler = ${dequeueHandler.getClass.getName}")
          dequeueHandler.handle(
            Log,
            event,
            kamakiCloud,
            ImageTransformers
          )
          false
        }

      if(isServer) {
        Thread.sleep(serverSleepMillis)
        serverLoop(rabbit, newIsEmpty)
      }
    }

    var lastErrorMillis = System.currentTimeMillis()

    @tailrec
    def healthLoop(): Unit = {
      val dtMax = 10 * serverSleepMillis

      def checkMillis(): Boolean = {
        val dt = System.currentTimeMillis() - lastErrorMillis
        val retval = dt >= dtMax

        if(retval) { lastErrorMillis = System.currentTimeMillis() }
        retval
      }

      def checkMillisAndPrint(e: Exception) = if(checkMillis()) { Log.info(f"$lastErrorMillis%-13d $e") }

      try {
        val rabbit = connector.connect()
        serverLoop(rabbit, isEmpty = false)
        if(!isServer) { rabbit.close() }
      }
      catch {
        case e: java.net.ConnectException ⇒
          checkMillisAndPrint(e)
          Thread.sleep(serverSleepMillis)

        case e: Exception ⇒
          checkMillisAndPrint(e)
      }

      if(isServer) healthLoop()
    }

    healthLoop()
  }

  def do_dequeue(args: Args.Dequeue): Unit = {
    val kamakiCloud = args.kamakiCloud
    val connector = RabbitConnector(configOfPath(args.conf))
    do_dequeue(connector, kamakiCloud)
  }

  def do_register_now(args: Args.RegisterNow): Unit = {
    val url = args.url
    val kamakiCloud = args.kamakiCloud
    val osfamily = args.osfamily
    val users = args.users
    val format = args.format
    val formatOpt = Option(format).map(Sys.fixFormat)

    val properties = Sys.minimumImageProperties(osfamily, users)

    Sys.downloadAndPublishImageFile(
      Log,
      formatOpt,
      properties,
      kamakiCloud,
      url,
      ImageTransformers
    )
  }

  def do_drain_queue(args: Args.DrainQueue): Unit = {
    val connector = RabbitConnector(configOfPath(args.conf))
    val rabbit = connector.connect()

    def drainLoop(count: Int): Int = {
      rabbit.get() match {
        case null ⇒
          rabbit.close()
          count

        case getResponse ⇒
          rabbit.ack(getResponse)

          try {
            val drainedBytes = getResponse.getBody
            val drainedString = new String(drainedBytes, StandardCharsets.UTF_8)
            val event = Event.ofJson(drainedString)
            Log.info(s"Drained event $count\n$event")
          }
          catch {
            case e: Exception ⇒
              Log.error(s"Error converting drained event $count to appropriate format: ${e.getMessage}")
          }

          drainLoop(count + 1)
      }
    }

    val howmany = drainLoop(0)
    val msg = s"Drained $howmany messages"
    Log.info(msg)
    System.out.println(msg)
  }

  def do_transform(args: Args.Transform): Unit = {
    val imageURL = args.url
    val GetImage(isTemporary, imageFile) = Sys.getImage(Log, imageURL)

    try {
      val tramsformedFileOpt = ImageTransformers.transform(None, imageFile)
      for {
        transformedFile ← tramsformedFileOpt
      } {
        Log.info(s"do_transform(): Transformed $imageURL to $transformedFile.")
        System.out.println(s"Transformed $imageURL to $transformedFile. Do not forget to delete the temporary file.")
        System.out.println(s"$transformedFile")
      }
    }
    finally {
      if(isTemporary){
        Log.info(s"do_transform(): Deleting temporary $imageFile")
        imageFile.delete()
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val t0 = System.currentTimeMillis()
    val argsDebugStr = args.mkString(" ")
    Log.info("=" * 30)
    Log.info(s"BEGIN snf-vmcatcher [$argsDebugStr]")
    val jc = Args.jc
    try {
      jc.parse(args:_*)

      val map = Map(
        ("-v", isVerbose),
        ("-h", isHelp),
        ("-server", isServer),
        ("-sleepMillis", serverSleepMillis),
        ("-handler", dequeueHandler)
      )

      Log.info(map.mkString(", "))

      val command = jc.getParsedCommand
      val isNoCommand = command eq null

      if(isHelp || isNoCommand)
        jc.usage()
      else
        commandMap(command)()
    }
    catch {
      case e: ParameterException ⇒
        System.err.println(e.getMessage)
        Log.error(e.getMessage)
        EXIT(1)

      case e: Exception ⇒
        System.err.println(e.getMessage)
        Log.error("", e)
        EXIT(2)

      case e: Throwable ⇒
        System.err.println(e.getMessage)
        Log.error("", e)
        EXIT(3)
    }
    finally {
      val t1 = System.currentTimeMillis()
      val dtms = t1 - t0
      Log.info(s"END snf-vmcatcher ($dtms ms) [$argsDebugStr]")
      Log.info("=" * 30)
    }
  }
}
