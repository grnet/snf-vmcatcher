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
import java.util.Scanner

import com.beust.jcommander.ParameterException
import com.typesafe.config.{Config, ConfigRenderOptions}
import gr.grnet.egi.vmcatcher.cmdline.Args
import gr.grnet.egi.vmcatcher.cmdline.Args.ParsedCmdLine
import gr.grnet.egi.vmcatcher.image.ImageTransformers
import gr.grnet.egi.vmcatcher.message.ImageListConfig
import gr.grnet.egi.vmcatcher.rabbit.{Rabbit, RabbitConnector}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
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
    Args.nameOf( ParsedCmdLine.registerNow          ) → DEFER { do_register_now           ( ParsedCmdLine.registerNow          ) }
  )

  def stringOfConfig(config: Config) = config.root().render(configRenderOptions)

  def env: Map[String, String] = sys.env
  def envAsJson = Json.jsonOfMap(env)
  def envAsPrettyJson: String =  Json.jsonOfMap(env)
  def VMCatcherFilter(key: String) = key.startsWith("VMCATCHER_")

  /**
   * Creates the subset of environment variables whose key starts with "VMCATCHER_".
   */
  def vmCatcherSysEnv: Map[String, String] = env.filter { case (k, _) ⇒ VMCatcherFilter(k) }

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
    val map = vmCatcherSysEnv
    val jsonMsg = Json.jsonOfMap(map)
    Log.info(s"jsonMsg = $jsonMsg")
    val rabbit = connector.connect()

    rabbit.publish(jsonMsg)
    rabbit.close()
  }
  
  def do_enqueue_from_env(args: Args.EnqueueFromEnv): Unit = {
    val connector = RabbitConnector(configOfPath(args.conf))
    do_enqueue(connector)
  }

  def do_enqueue_from_image_list(args: Args.EnqueueFromImageList): Unit = {
    val imageListURL = args.imageListUrl
    val imageIdentifier = args.imageIdentifier

    val response = Http.GET(imageListURL)
    response.handshake()
    if(!response.isSuccessful) {
      Log.error(s"Could not fetch $imageListURL. GET returned ${response.code()} ${response.message()}")
      if(!isServer) { sys.exit(1) }
      return
    }

    val imageListStr = response.body().string()
    Log.info(imageListStr)
    val scanner = new Scanner(imageListStr)
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
    val jsonImageList = buffer.toString
    val imageListConfig = ImageListConfig.ofString(jsonImageList)
    Log.info(s"Parsed image list dc:identifier = ${imageListConfig.dcIdentifier}")
    Log.info(s"                         hv:uri = ${imageListConfig.hvURI}")
    val projectedForIdent = imageListConfig.projectImageDcIdentifier(imageIdentifier)
    if(projectedForIdent.size == 0) {
      Log.error(s"Image identifier $imageIdentifier not found in image list hv:uri = ${imageListConfig.hvURI}")
      Log.info(s"Available identifiers are: ${imageListConfig.images.map(_.dcIdentifier).mkString(", ")}")
      EXIT(3)
    }

    val connector = RabbitConnector(configOfPath(args.confDelegate.conf))
    val rabbit = connector.connect()

    for {
      imageConfig ← projectedForIdent
    } {
      val imageIdent = imageConfig.dcIdentifier
      val imageURI = imageConfig.hvURI
      Log.info(s"Enqueueing dc:identifier = $imageIdent, hv:uri = $imageURI")

      rabbit.publish(imageConfig.toJson)
    }

    rabbit.close()
  }

  def do_dequeue(connector: RabbitConnector, kamakiCloud: String): Unit = {
    @tailrec
    def loop(rabbit: Rabbit, isEmpty: Boolean): Unit = {
      val newIsEmpty =
        rabbit.getAndAck {
          if(!isEmpty) { Log.info("Queue is empty") }
          true
        } { response ⇒
          val jsonMsgBytes = response.getBody
          val jsonMsg = new String(jsonMsgBytes, StandardCharsets.UTF_8)
          val map = Json.mapOfJson(jsonMsg)

          Log.info(s"dequeueHandler = ${dequeueHandler.getClass.getName}")
          dequeueHandler.handle(
            Log,
            jsonMsg,
            map,
            kamakiCloud,
            ImageTransformers
          )
          false
        }

      if(isServer) {
        Thread.sleep(serverSleepMillis)
        loop(rabbit, newIsEmpty)
      }
    }

    var lastErrorMillis = System.currentTimeMillis()
    do {
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
        loop(rabbit, isEmpty = false)
        if(!isServer) { rabbit.close() }
      }
      catch {
        case e: java.net.ConnectException ⇒
          checkMillisAndPrint(e)
          Thread.sleep(serverSleepMillis)

        case e: Exception ⇒
          checkMillisAndPrint(e)
      }
    } while(isServer)
  }

  def do_dequeue(args: Args.Dequeue): Unit = {
    val kamakiCloud = args.kamakiCloud
    val connector = RabbitConnector(configOfPath(args.conf))
    do_dequeue(connector, kamakiCloud)
  }

  def do_register_now(args: Args.RegisterNow): Unit = {
    val url = args.url
    val kamakiCloud = args.kamakiCloud

    Sys.downloadAndPublishImageFile(
      Log,
      None,
      kamakiCloud,
      url,
      ImageTransformers
    )
  }

  def main(args: Array[String]): Unit = {
    val t0 = System.currentTimeMillis()
    val argsDebugStr = args.mkString(" ")
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
        EXIT(1)

      case e: Exception ⇒
        e.printStackTrace(System.err)
        EXIT(2)
    }
    finally {
      val t1 = System.currentTimeMillis()
      val dtms = t1 - t0
      Log.info(s"END snf-vmcatcher ($dtms ms) [$argsDebugStr]")
    }
  }
}
