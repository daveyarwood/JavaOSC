/*
 * Copyright (C) 2004-2019, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc.transport;

import com.illposed.osc.OSCPacket;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.OSCParseException;
import java.io.IOException;

/**
 * An interface about sending and receiving OSC packets over a network.
 *
 * Implementations:
 * <ul>
 * <li>{@link UDPTransport}</li>
 * <li>{@link TCPTransport}</li>
 * </ul>
 */
public interface Transport {
	/**
	 * Converts and sends an OSC packet (message or bundle) to the remote address.
	 * @param packet the bundle or message to be converted and sent
	 * @throws IOException if we run out of memory while converting,
	 *   or a socket I/O error occurs while sending
	 * @throws OSCSerializeException if the packet fails to serialize
	 */
	void send(final OSCPacket packet) throws IOException, OSCSerializeException;

	/**
	 * Receive an OSC packet.
	 * @return the packet received
	 * @throws IOException if an I/O error occurs while trying to read from the
	 * @throws OSCParseException if the packet fails to parse
	 * channel
	 */
	OSCPacket receive() throws IOException, OSCParseException;

	boolean isBlocking();

	void connect() throws IOException;

	void disconnect() throws IOException;

	boolean isConnected();

	/**
	 * Close the socket and free-up resources.
	 * It is recommended that clients call this when they are done with the
	 * port.
	 * @throws IOException If an I/O error occurs
	 */
	void close() throws IOException;
}
