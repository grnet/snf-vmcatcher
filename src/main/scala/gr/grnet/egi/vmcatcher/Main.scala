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

import java.io.{File, IOException}
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import com.beust.jcommander.ParameterException
import gr.grnet.egi.vmcatcher.cmdline.CmdLine._
import gr.grnet.egi.vmcatcher.cmdline._
import gr.grnet.egi.vmcatcher.config.{Config, RabbitMQConfig}
import gr.grnet.egi.vmcatcher.event._
import gr.grnet.egi.vmcatcher.image.handler.HandlerData
import gr.grnet.egi.vmcatcher.image.transformer.ImageTransformers
import gr.grnet.egi.vmcatcher.queue.{QueueConnectAttempt, QueueConnectFailedAttempt, QueueConnectFirstAttempt}
import gr.grnet.egi.vmcatcher.rabbit.{Rabbit, RabbitConnector}
import gr.grnet.egi.vmcatcher.util.{GetImage, UsernamePassword}
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

/**
 *
 */
object Main extends {
  val t0 = System.currentTimeMillis()
  var _args = Array[String]()
  lazy val argsDebugStr = _args.mkString(" ")

  final val ProgName = getClass.getName.stripSuffix("$")
  final val Log = LoggerFactory.getLogger(getClass)

  lazy val vmcatcher = new StdVMCatcher(config)

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

  def mkcmd[A <: AnyRef](c: A, f: (A) ⇒ Unit): (String, () ⇒ Unit) = {
    val name = nameOf(c)
    val command = () ⇒ f(c)
    name → command
  } 
  
  val commandMap = Map(
    mkcmd(CmdLine.usage, do_usage),

    // Debugging
    mkcmd(CmdLine.showEnv, do_show_env),
    mkcmd(CmdLine.showConf, do_show_conf),

    // Queues
    mkcmd(CmdLine.enqueueFromEnv, do_enqueue_from_env),
//    mkcmd(CmdLine.enqueueFromImageList, do_enqueue_from_image_list),
    mkcmd(CmdLine.dequeue, do_dequeue),
    mkcmd(CmdLine.drainQueue, do_drain_queue),
    mkcmd(CmdLine.testQueue, do_test_queue),

    // Image lists
    mkcmd(CmdLine.registerImageList,   do_register_image_list),
    mkcmd(CmdLine.activateImageList,   do_activate_image_list),
    mkcmd(CmdLine.deactivateImageList, do_deactivate_image_list),
    mkcmd(CmdLine.updateCredentials,   do_update_credentials),
    mkcmd(CmdLine.fetchImageList,      do_fetch_image_list),
//    mkcmd(CmdLine.parseImageList, do_parse_image_list),
//    mkcmd(CmdLine.getImageList, do_get_image_list),

    // Images
    mkcmd(CmdLine.registerImageNow, do_register_now),

    mkcmd(CmdLine.transform, do_transform)
  )

  def env: Map[String, String] = sys.env
  def envAsJson = Json.jsonOfMap(env)
  def envAsPrettyJson: String =  Json.jsonOfMap(env)

  def isVerbose = CmdLine.globalOptions.verbose
  def isHelp    = CmdLine.globalOptions.help
  def isServer  = CmdLine.dequeue.server
  def serverSleepMillis = CmdLine.dequeue.sleepMillis max 0L min 1000L
  def dequeueHandler = CmdLine.dequeue.handler

  def do_usage(args: Usage): Unit = jc.usage()

  def do_show_env(args: ShowEnv): Unit = println(envAsPrettyJson)

  def do_show_conf(args: ShowConf): Unit = println(config.toString)

  def do_enqueue(connector: RabbitConnector): Unit = {
    val event = Event.ofSysEnv
    Log.info(s"event (sysenv) = $event")
    val json = event.toJson

    val rabbit = connector.connect()

    rabbit.publish(json)
    rabbit.close()
  }
  
  def do_enqueue_from_env(args: EnqueueFromEnv): Unit = {
    val connector = RabbitConnector(config.getRabbitConfig)
    do_enqueue(connector)
  }

  def do_register_image_list(args: RegisterImageList): Unit = {
    val upOpt = UsernamePassword.optional(args.username, args.password)
    val ref = vmcatcher.registerImageList(args.name, args.url, args.isActive, upOpt)
    INFO(s"Registered $ref")
  }

  def do_activate_image_list(args: ActivateImageList): Unit = {
    val wasActive = vmcatcher.activateImageList(args.name)
    if(wasActive) { INFO(s"Already active") }
    else          { INFO(s"Activated") }
  }

  def do_deactivate_image_list(args: DeactivateImageList): Unit = {
    val wasActive = vmcatcher.deactivateImageList(args.name)
    if(wasActive) { INFO(s"Deactivated") }
    else          { INFO(s"Already deactive") }
  }

  def do_update_credentials(args: UpdateCredentials): Unit = {
    val upOpt = UsernamePassword.optional(args.username, args.password)
    vmcatcher.updateCredentials(args.name, upOpt)
    if(upOpt.isDefined) { INFO(s"Credentials have been set") }
    else                { INFO(s"Credentials have been cleared") }
  }

  def do_fetch_image_list(args: FetchImageList): Unit = {
    val (ref, currentImages) = vmcatcher.fetchImageList(args.name)
    INFO(s"Fetched image list $ref, parsed ${currentImages.size} images")
    for {
      currentImage ← currentImages
      image ← currentImage.f_image.obj
    } {
      INFO(s"Parsed image $image")
    }
  }

//  def do_parse_image_list(args: ParseImageList): Unit = {
//    val imageListContainerURL = args.imageListUrl
//    val token = args.token
//    val rawImageListContainer = Sys.downloadRawImageList(imageListContainerURL, Option(token))
//    Log.info (s"imageListContainer (URL ) = $imageListContainerURL")
//    Log.debug(s"imageListContainer (raw ) =\n$rawImageListContainer")
//    val imageListContainerJson = parseImageListContainerJson(rawImageListContainer)
//    Log.info (s"imageListContainer (json) =\n$imageListContainerJson")
//
//    val events0 = Events.ofJson(imageListContainerJson, Map())
//    INFO(s"Found ${events0.size} events")
//    if(events0.isEmpty) { return }
//
//    // Sort by size ascending and print basic info
//    val sortedEvents =
//      try events0.sortBy(_(ImageEventField.VMCATCHER_EVENT_HV_SIZE).toLong)
//      catch {
//        case e: Exception ⇒
//          Log.warn(s"Could not sort events", e)
//          events0
//      }
//
//    val miniKeys = Seq(
//      ImageEventField.VMCATCHER_EVENT_DC_IDENTIFIER,
//      ImageEventField.VMCATCHER_EVENT_AD_MPURI,
//      ImageEventField.VMCATCHER_EVENT_HV_URI,
//      ImageEventField.VMCATCHER_EVENT_HV_SIZE)
//
//    for(event ← sortedEvents) {
//      val miniMap = Map((for(key ← miniKeys) yield (key, event(key))):_*)
//      val miniInfo = miniMap.mkString("[", ", ", "]")
//      INFO(s"Found: $miniInfo")
//    }
//  }

//  def do_get_image_list(args: GetImageList): Unit = {
//    val url = args.url
//    val token = args.token
//    val rawImageListContainer = Sys.downloadRawImageList(url, Option(token))
//    Log.info (s"imageListContainer (URL ) = $url")
//    val imageListContainerJson = parseImageListContainerJson(rawImageListContainer)
//    INFO(s"imageListContainer (json) =\n$imageListContainerJson")
//  }

//  def do_enqueue_from_image_list(args: EnqueueFromImageList): Unit = {
//    val imageListURL = args.imageListUrl
//    val imageIdentifier = args.imageIdentifier
//    val tokenOpt = Option(args.token)
//
//    val rawText = Sys.downloadRawImageList(imageListURL, tokenOpt)
//    Log.info(s"imageList (URL) = $imageListURL")
//    Log.info(s"imageList (raw) = $rawText")
//    val jsonImageList = parseImageListContainerJson(rawText)
//    val events0 = Events.ofJson(
//      jsonImageList,
//      //Map(ExternalEventField.VMCATCHER_X_EVENT_IMAGE_LIST_URL → imageListURL.toString)
//      Map()
//    )
//
//    events0 match {
//      case Nil ⇒
//        val errMsg = s"Could not parse events from image list"
//        ERROR(errMsg)
//        EXIT(4)
//
//      case event :: _ ⇒
//        val dcIdentifier = event(ImageListEventField.VMCATCHER_EVENT_IL_DC_IDENTIFIER)
//        val parsedMsg = s"Parsed image list dc:identifier = $dcIdentifier"
//        INFO(parsedMsg)
//        val events =
//          if(imageIdentifier eq null)
//            events0
//          else
//            events0.filter(_(ImageEventField.VMCATCHER_EVENT_DC_IDENTIFIER) == imageIdentifier)
//
//        if(events.isEmpty) {
//          val errMsg = s"Image identifier $imageIdentifier not found"
//          ERROR(errMsg)
//          val identifiers = events0.map(_(ImageEventField.VMCATCHER_EVENT_DC_IDENTIFIER))
//          val availableMsg = s"Available identifiers are: ${identifiers.mkString(", ")}"
//          INFO(availableMsg)
//          EXIT(3)
//        }
//
//        val matchMsg = s"Matched ${events.size} event(s)"
//        INFO(matchMsg)
//
//        val connector = RabbitConnector(config.getRabbitConfig)
//        val rabbit = connector.connect()
//
//        for {
//          event ← events
//        } {
//          val imageIdent = event(ImageEventField.VMCATCHER_EVENT_DC_IDENTIFIER)
//          val image_HV_URI = event(ImageEventField.VMCATCHER_EVENT_HV_URI)
//          val image_AD_MPURI = event(ImageEventField.VMCATCHER_EVENT_AD_MPURI)
//          val eventMsg = s"Enqueueing event for dc:identifier = $imageIdent, hv:uri = $image_HV_URI, ad:mpuri = $image_AD_MPURI"
//          INFO(eventMsg)
//          Log.info(s"event (image) = $event")
//
//          rabbit.publish(event.toJson)
//        }
//
//        val enqueuedMsg = s"Enqueued ${events.size} event(s)"
//        INFO(enqueuedMsg)
//
//        rabbit.close()
//    }
//  }

  def do_dequeue_(connector: RabbitConnector, data: HandlerData): Unit = {
    def doOnce(rabbit: Rabbit): Unit = {
      try {
        rabbit.getAndAck {} { response ⇒
          val jsonMsgBytes = response.getBody
          val jsonMsg = new String(jsonMsgBytes, StandardCharsets.UTF_8)
          val event = Event.ofJson(jsonMsg)

          Log.info(s"dequeueHandler = ${dequeueHandler.getClass.getName}")
          dequeueHandler.handle(event, data)
        }
      }
      finally rabbit.close()
    }

    @tailrec
    def connectToRabbit(lastStatus: QueueConnectAttempt): Rabbit = {
      try {
        val rabbit = connector.connect()

        lastStatus match {
          case QueueConnectFailedAttempt(firstAttemptMillis, failedAttempts) ⇒
            val dtMillis = System.currentTimeMillis() - firstAttemptMillis
            val dtSec = dtMillis / 1000
            Log.info(s"OK, successfully connected to Rabbit after $dtSec sec and $failedAttempts attempts")

          case QueueConnectFirstAttempt(firstAttemptMillis) ⇒
            if(!isServer) {
              val dtMillis = System.currentTimeMillis() - firstAttemptMillis
              Log.info(s"OK, successfully connected to Rabbit after $dtMillis ms")
            }

          case _ ⇒
        }

        rabbit
      }
      catch {
        case e: Exception ⇒
          if(lastStatus.isFirstAttempt) {
            Log.error("First failed attempt to connect to the queue", e)
          }
          else {
            val failedAttempts = lastStatus.failedAttempts + 1
            Log.error(s"Successive ($failedAttempts) failed attempt to connect to the queue: $e")
          }
          Thread.sleep(serverSleepMillis)
          connectToRabbit(lastStatus.toFailed)
      }
    }

    @tailrec
    def doOnceOrLoop(): Unit = {
      try {
        val attempt = QueueConnectFirstAttempt(System.currentTimeMillis())
        val rabbit = connectToRabbit(attempt)
        doOnce(rabbit)
      }
      catch {
        case unrelated: Exception ⇒
          Log.error("", unrelated)
          if(!isServer) throw unrelated
      }

      if(isServer) {
        // DO not reconnect too often
        Thread.sleep(serverSleepMillis)
        doOnceOrLoop()
      }
    }

    doOnceOrLoop()
  }

  def do_dequeue(args: Dequeue): Unit = {
    val kamakiCloud = args.kamakiCloud
    val insecureSSL = args.insecureSSL
    val workingFolder = CmdLine.globalOptions.workingFolder
    val data = HandlerData(Log, kamakiCloud, ImageTransformers, insecureSSL, workingFolder)

    if(insecureSSL) {
      Log.warn(s"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
      Log.warn(s"! Insecure SSL mode. This is provided as a debugging aid only !!")
      Log.warn(s"! If you trust a (possibly self-signed) certificate, add it to !")
      Log.warn(s"! the trust store. Do not ignore SSL validation errors !!!!!!!!!")
      Log.warn(s"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    }

    val connector = RabbitConnector(config.getRabbitConfig)
    do_dequeue_(connector, data)
  }

  def do_register_now(args: RegisterImageNow): Unit = {
    val url = args.url
    val insecureSSL = args.insecureSSL
    val kamakiCloud = args.kamakiCloud
    val osfamily = args.osfamily
    val users = args.users
    val format = args.format
    val formatOpt = Option(format).map(Sys.fixFormat)
    val workingFolder = CmdLine.globalOptions.workingFolder
    val data = HandlerData(Log, kamakiCloud, ImageTransformers, insecureSSL, workingFolder)

    val properties = Sys.minimumImageProperties(osfamily, users)

    Sys.downloadAndPublishImageFile(
      formatOpt,
      properties,
      url,
      data,
      None
    )
  }

  def do_drain_queue(args: DrainQueue): Unit = {
    val connector = RabbitConnector(config.getRabbitConfig)
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

  def do_transform(args: Transform): Unit = {
    val imageURL = args.url
    val insecureSSL = args.insecureSSL
    val workingFolder = CmdLine.globalOptions.workingFolder
    val data = HandlerData(Log, "", ImageTransformers, insecureSSL, workingFolder)
    val GetImage(isTemporary, imageFile) = Sys.getImage(imageURL, data)

    try {
      val tramsformedFileOpt = ImageTransformers.transform(None, imageFile, workingFolder)
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

  def do_test_queue(args: TestQueue): Unit = {
    val rabbitConfig = config.getRabbitConfig
    val maskedRabbitConfig = RabbitMQConfig.newBuilder(rabbitConfig).setPassword("***").build()
    val connector = RabbitConnector(rabbitConfig)
    try {
      val rabbit = connector.connect()
      rabbit.close()
      val successMsg = s"Successfully connected to queue using $maskedRabbitConfig"
      Log.info(successMsg)
      System.out.println(successMsg)
    }
    catch {
      case e: IOException ⇒
        val errMsg = s"Could not connect to queue using $maskedRabbitConfig"
        Log.error(errMsg, e)
        System.err.println(errMsg)
    }
  }

  lazy val config: Config = {
    val path = CmdLine.globalOptions.config
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

  def mainV(args: Array[String]): Unit = {
    beginSequence(args)
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

    if(isHelp || isNoCommand) {
      jc.usage()
      EXIT(1, endSequence)
    }
    else {
      commandMap.get(command) match {
        case None ⇒
          throw new ParameterException(s"Unknown command $command")
        case Some(commandf) ⇒
          commandf()
          EXIT(0, endSequence)
      }
      commandMap(command)()

    }
  }

  def main(args: Array[String]): Unit = {
    try mainV(args)
    catch {
      case e: ParameterException ⇒
        ERROR(e.getMessage)
        EXIT(2, endSequence)

      case e: IllegalArgumentException ⇒
        ERROR(e.getMessage)
        EXIT(2, endSequence)

      case e: VMCatcherException ⇒
        System.err.println(e.msg)
        EXIT(e.code.code, endSequence)

      case e: Exception ⇒
        System.err.println(e.getMessage)
        Log.error("", e)
        e.printStackTrace(System.err)
        EXIT(3, endSequence)

      case e: Throwable ⇒
        System.err.println(e.getMessage)
        Log.error("", e)
        e.printStackTrace(System.err)
        EXIT(4, endSequence)
    }
  }
}
