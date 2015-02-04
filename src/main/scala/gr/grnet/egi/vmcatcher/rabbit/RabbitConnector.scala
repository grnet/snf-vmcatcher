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

import java.util.Collections

import com.rabbitmq.client.{Address, ConnectionFactory}
import gr.grnet.egi.vmcatcher.config.RabbitMQConfig
import scala.collection.JavaConverters._

/**
 *
 */
case class RabbitConnector(config: RabbitMQConfig) {
  def connect(): Rabbit = {
    val username = config.getUsername
    val queue = config.getQueue
    val exchange = config.getExchange
    val routingKey = config.getRoutingKey
    val vhost = config.getVhost
    val password = config.getPassword
    val servers = config.getServers.asScala
    val addresses = servers.map(Address.parseAddress).toArray

    val f = new ConnectionFactory
//    f.setAutomaticRecoveryEnabled(true)
//    f.setTopologyRecoveryEnabled(true)  // this is the default anyway
//    f.setNetworkRecoveryInterval(250)
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
