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

package gr.grnet.egi.vmcatcher.rabbit

import com.typesafe.config.Config
import com.rabbitmq.client.{ConnectionFactory, Address}
import java.util.Collections
import scala.collection.JavaConverters._

/**
 *
 */
case class RabbitConnector(config: Config) {
  def connect(): Rabbit = {
    val rconf = config.getConfig("rabbitmq")
    val username = rconf.getString("username")
    val queue = rconf.getString("queue")
    val exchange = rconf.getString("exchange")
    val routingKey = rconf.getString("routingKey")
    val vhost = rconf.getString("vhost")
    val password = rconf.getString("password")
    val servers = rconf.getStringList("servers").asScala
    val addresses = servers.map(Address.parseAddress).toArray

    val f = new ConnectionFactory
    f.setUsername(username)
    f.setPassword(password)
    f.setVirtualHost(vhost)

    val conn = f.newConnection(addresses)
    val chan = conn.createChannel()
    chan.basicQos(1, true)
    val exch = chan.exchangeDeclare(exchange, "topic", true, false, Collections.emptyMap())
    val queu = chan.queueDeclare(queue, true, false, false, Collections.emptyMap())
    val bind = chan.queueBind(queue, exchange, routingKey)

    Rabbit(
      config = config,
      f = f,
      conn = conn,
      chan = chan,
      queue = queue,
      exchange = exchange,
      routingKey = routingKey
    )
  }
}
