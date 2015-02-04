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

import java.io.Closeable
import java.nio.charset.StandardCharsets

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client._
import gr.grnet.egi.vmcatcher.config.RabbitMQConfig

/**
 *
 */
case class Rabbit(
  config: RabbitMQConfig,
  f: ConnectionFactory,
  conn: Connection,
  chan: Channel,
  queue: String,
  exchange: String,
  routingKey: String
) extends Closeable {

  override def close(): Unit = {
    chan.close()
    conn.close()
  }
  
  def publish(json: String): Unit = {
    val basicProps = new BasicProperties.Builder().
      contentType("application/json").
      deliveryMode(2).
      priority(0).
      build()
    chan.basicPublish(
      /* exchange   = */ exchange,
      /* routingKey = */ queue,
      /* props      = */ basicProps,
      /* body       = */ json.getBytes(StandardCharsets.UTF_8)
    )
  }

  def get(): GetResponse = chan.basicGet(queue, false)

  def ack(response: GetResponse): Unit = chan.basicAck(response.getEnvelope.getDeliveryTag, false)

  /**
   * Gets one message from the queue and ACKs it if the provided function terminates without an exception.
   * If `f` throws an exception, it is propagated to the caller.
   */
  def getAndAck[A](onEmpty: ⇒A)(onResponse: (GetResponse) ⇒ A): A =
    get() match {
      case null ⇒
        onEmpty
      case response ⇒
        val retval = onResponse(response)
        ack(response)
        retval
    }
}
