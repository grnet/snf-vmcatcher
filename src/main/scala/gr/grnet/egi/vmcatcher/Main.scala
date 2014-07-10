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
import com.typesafe.config.{ConfigRenderOptions, Config, ConfigFactory}
import java.nio.charset.StandardCharsets
import com.beust.jcommander.ParameterException
import gr.grnet.egi.vmcatcher.cmdline.Args
import Args.Cmd
import scala.annotation.tailrec
import gr.grnet.egi.vmcatcher.rabbit.{RabbitConnector, Rabbit}
import org.slf4j.LoggerFactory

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object Main extends {
  final val ProgName = getClass.getName.stripSuffix("$")
  final val Log = LoggerFactory.getLogger(getClass)

  val configRenderOptions =
    ConfigRenderOptions.defaults().
      setComments(true).
      setOriginComments(false).
      setFormatted(true).
      setJson(true)

  val commandMap = Map(
    Args.name( Cmd.env     ) → DEFER { do_env()     },
    Args.name( Cmd.conf    ) → DEFER { do_conf()    },
    Args.name( Cmd.enqueue ) → DEFER { do_enqueue() },
    Args.name( Cmd.dequeue ) → DEFER { do_dequeue() }
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

  def isVerbose = Cmd.globalOptions.verbose
  def isHelp    = Cmd.globalOptions.help
  def isServer  = Cmd.dequeue.server
  def serverSleepMillis = Cmd.dequeue.sleepMillis max 0L min 1000L
  def dequeueHandler = Cmd.dequeue.handler

  lazy val config: Config = {
    val config =
      Args.Cmd.globalOptions.conf match {
        case "" ⇒
          val msg = "No application.conf specified"
          throw new ParameterException(msg)

        case path ⇒
          Log.info(s"Load conf from $path")
          val file = new File(path)
          ConfigFactory.parseFile(file)
      }

    config.resolve()
  }

  def do_env(): Unit = println(envAsPrettyJson)

  def do_conf(): Unit = println(stringOfConfig(config))

  def do_enqueue(connector: RabbitConnector): Unit = {
    val map = vmCatcherSysEnv
    val jsonMsg = Json.jsonOfMap(map)
    Log.info(s"jsonMsg = $jsonMsg")
    val rabbit = connector.connect()

    rabbit.publish(jsonMsg)
    rabbit.close()
  }
  
  def do_enqueue(): Unit = {
    val connector = RabbitConnector(config)
    do_enqueue(connector)
  }

  def do_dequeue(connector: RabbitConnector): Unit = {
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
          dequeueHandler.handle(Log, jsonMsg, map)
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

  def do_dequeue(): Unit = {
    val connector = RabbitConnector(config)
    do_dequeue(connector)
  }

  def main(args: Array[String]): Unit = {
    val jc = Args.jc
    try {
      jc.parse(args:_*)

      val map = Map(
        ("-v", isVerbose),
        ("-h", isHelp),
        ("-server", isServer),
        ("-sleepMillis", serverSleepMillis),
        ("-handler", dequeueHandler.getClass.getName)
      )

      Log.info(map.mkString(", "))

      val command = jc.getParsedCommand
      val isNoCommand = command eq null
      val kamakiCloud = Cmd.dequeue.kamakiCloud
      val haveNoCloud = (kamakiCloud eq null) || kamakiCloud.isEmpty

      if(isHelp || isNoCommand || haveNoCloud)
        jc.usage()
      else
        commandMap(command)()
    }
    catch {
      case e: ParameterException ⇒
        System.err.println(e.getMessage)
        jc.usage()
        System.exit(1)

      case e: Exception ⇒
        e.printStackTrace(System.err)
        System.exit(2)
    }
  }
}
