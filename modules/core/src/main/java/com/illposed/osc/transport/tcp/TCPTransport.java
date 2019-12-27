/*
 * Copyright (C) 2004-2019, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc.transport.tcp;

import com.illposed.osc.OSCPacket;
import com.illposed.osc.OSCParser;
import com.illposed.osc.OSCParseException;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.OSCSerializer;
import com.illposed.osc.OSCSerializerAndParserBuilder;
import com.illposed.osc.transport.Transport;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * A {@link Transport} implementation for sending and receiving OSC packets over
 * a network via TCP.
 */
public class TCPTransport implements Transport {
	private final InetSocketAddress local;
	private final InetSocketAddress remote;
	private final OSCParser parser;
	private final OSCSerializerAndParserBuilder serializerBuilder;

	private Socket clientSocket = null;
	private ServerSocket serverSocket = null;

  private Socket getClientSocket() throws IOException {
    if (clientSocket == null || clientSocket.isClosed()) {
      clientSocket = new Socket();
    }

    return clientSocket;
  }

  private ServerSocket getServerSocket() throws IOException {
    if (serverSocket == null || serverSocket.isClosed()) {
      serverSocket = new ServerSocket();
      serverSocket.setReuseAddress(true);
      serverSocket.bind(local);
    }

    return serverSocket;
  }

	public TCPTransport(
		final InetSocketAddress local,
		final InetSocketAddress remote)
		throws IOException
	{
		this(local, remote, new OSCSerializerAndParserBuilder());
	}

	public TCPTransport(
		final InetSocketAddress local,
		final InetSocketAddress remote,
		final OSCSerializerAndParserBuilder serializerAndParserBuilder)
		throws IOException
	{
		this.local = local;
		this.remote = remote;
		this.parser = serializerAndParserBuilder.buildParser();
		this.serializerBuilder = serializerAndParserBuilder;
	}

	@Override
	public void connect() throws IOException {
    // Not relevant for TCP. TCP does involve connections, but we create them as
    // we need them, when either `send` or `receive` is invoked.
  }

	@Override
	public void disconnect() throws IOException {
		// Not possible/relevant for TCP.
	}

	@Override
	public boolean isConnected() {
    // Not relevant for TCP.
		return false;
	}

  public boolean isListening() throws IOException {
    try {
      new Socket(local.getAddress(), local.getPort()).close();
      return true;
    } catch (ConnectException ce) {
      return false;
    }
  }

	/**
		* Close the socket and free-up resources.
		* It is recommended that clients call this when they are done with the port.
		* @throws IOException If an I/O error occurs
		*/
	@Override
	public void close() throws IOException {
    if (clientSocket != null) {
      clientSocket.close();
    }

    if (serverSocket != null) {
      serverSocket.close();
    }
	}

	@Override
	public void send(final OSCPacket packet) throws IOException, OSCSerializeException {
		// FIXME: OSCSerializer is implemented specific to a ByteBuffer. You
		// initialize it with a ByteBuffer (implying that there is max size, which
		// there isn't you're using TCP) and the signature of the `write` method is
		// `write(packet)` and it writes it to the ByteBuffer that you initialized
		// it with.
		//
		// TCP is a stream-oriented protocol; we want to write the bytes to a stream
		// instead.
		//
		// TODO: refactor OSCSerializer in some way to make this possible. Maybe the
		// `write` method could return a byte array instead? Or maybe OSCSerializer
		// could be made to work with streams at a lower level?
		//
		// As a workaround for now, we'll just use a byte buffer allocated to a
		// really big amount.
		ByteBuffer buffer = ByteBuffer.allocate(1000000);
		OSCSerializer serializer = serializerBuilder.buildSerializer(buffer);
		buffer.rewind();
		serializer.write(packet);
		buffer.flip();

    Socket clientSocket = getClientSocket();
    if (!clientSocket.isConnected()) {
      clientSocket.connect(remote);
    }

    // Closing the output stream is necessary in order for the receiving side to
    // know that it has received everything. When reading bytes off of the
    // stream, an EOF (-1) byte signals the end of the stream, and this doesn't
    // get sent until the output stream is closed.
    //
    // NB: Closing the output stream effectively closes the client socket as
    // well. The next time getClientSocket() is called, it will recognize that
    // the socket is closed and create a new one. So, every message sent uses a
    // new client socket.
    try (OutputStream out = clientSocket.getOutputStream()) {
      out.write(buffer.array(), 0, buffer.limit());
    }
	}

	// InputStream.readAllBytes is available as of Java 9. Implementing it here in
	// order to support Java 8.
	//
	// Source: https://stackoverflow.com/a/53347936/2338327
  //
  // I modified the source in that we don't want to close the input stream, as
  // it belongs to a ServerSocket and we don't want to close the stream because
  // that would also close the socket.
	private byte[] readAllBytes(InputStream inputStream) throws IOException {
    final int bufLen = 4 * 0x400; // 4KB
    byte[] buf = new byte[bufLen];
    int readLen;

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      while ((readLen = inputStream.read(buf, 0, bufLen)) != -1) {
        outputStream.write(buf, 0, readLen);
      }

      return outputStream.toByteArray();
    }
	}

	@Override
	public OSCPacket receive() throws IOException, OSCParseException {
    ServerSocket serverSocket = getServerSocket();

    // Unlike UDP, TCP involves connections, and a valid use case is for a
    // client socket to connect to a server socket simply to test whether the
    // server socket is listening.
    //
    // When this happens, an empty byte array is received. We recognize this
    // scenario and move onto the next connection, and keep doing this until we
    // receive a connection that sends > 0 bytes.
    while (true) {
      Socket dataSocket = serverSocket.accept();
      byte[] bytes = readAllBytes(dataSocket.getInputStream());

      if (bytes.length == 0) {
        continue;
      }

      return parser.convert(ByteBuffer.wrap(bytes));
    }
	}

	@Override
	public boolean isBlocking() {
		// Not relevant for TCP.
		return false;
	}

	@Override
	public String toString() {
		return String.format(
			"%s: local=%s, remote=%s", getClass().getSimpleName(), local, remote
		);
	}
}
