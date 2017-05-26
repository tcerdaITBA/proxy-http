package tp.pdc.proxy.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.handler.interfaces.Handler;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.ServerMetricImpl;
import tp.pdc.proxy.parser.interfaces.CrazyProtocolParser;
import tp.pdc.proxy.parser.protocol.CrazyProtocolParserImpl;

public class ProtocolHandler implements Handler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolHandler.class);
	
	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffer;
	private CrazyProtocolParser parser;
	
	public ProtocolHandler(int bufferSize) {
		readBuffer = ByteBuffer.allocate(bufferSize);
		writeBuffer = ByteBuffer.allocate(bufferSize);
		parser = new CrazyProtocolParserImpl(ClientMetricImpl.getInstance(), ServerMetricImpl.getInstance());
	}
	
	@Override
	public void handleRead(SelectionKey key) {
		final SocketChannel socketChannel = (SocketChannel) key.channel();
		
		try {
			int bytesRead = socketChannel.read(readBuffer);
			LOGGER.info("Read {} bytes from protocol client", bytesRead);
		} catch (IOException e) {
			LOGGER.warn("Failed to read from protocol client: {}", e.getMessage());
			// TODO: agregar -end al final
		}
		
		// TODO: analizar si debe estar dentro del try
		readBuffer.flip();
		process(key);
		readBuffer.compact();		
	}

	private void process(SelectionKey key) {
		try {
			parser.parse(readBuffer, writeBuffer);
		} catch (ParserFormatException e) {
			LOGGER.warn("Non-Recuperable error while parsing protocol: {}", e.getMessage());
			key.interestOps(SelectionKey.OP_WRITE);
		}
		
		if (parser.hasFinished()) {
			LOGGER.debug("Registering protocol handler for write: protocol parser has finished");
			key.interestOps(SelectionKey.OP_WRITE);
		}
		else if (!readBuffer.hasRemaining()) {
			LOGGER.debug("Registering protocol handler for write: read buffer full");
			key.interestOps(SelectionKey.OP_WRITE);			
		}
		else if (readBuffer.hasRemaining()) {
			LOGGER.debug("Registering protocol handler for write and read: parsed but not finished");
			key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
		}
	}
	
	@Override
	public void handleWrite(SelectionKey key) {
		writeBuffer.flip();
		write(key);
		writeBuffer.compact();
		
		readBuffer.flip();
		if (!parser.hasFinished() && readBuffer.hasRemaining())
			process(key);
		readBuffer.compact();
	}

	private void write(SelectionKey key) {
		final SocketChannel socketChannel = (SocketChannel) key.channel();
		
		try {
			int bytesWritten = socketChannel.write(writeBuffer);
			LOGGER.info("Wrote {} bytes to protocol client", bytesWritten);
			
			if (parser.hasFinished()) {
				if (!writeBuffer.hasRemaining()) {
					LOGGER.debug("Closing connection to protocol client: parser finished and no bytes left in writeBuffer");
					socketChannel.close();
				}
				else {
					LOGGER.debug("Registering protocol client for write: parser finished but not all bytes sent");
					key.interestOps(SelectionKey.OP_WRITE);
				}
			}
			else {
				if (!writeBuffer.hasRemaining()) {
					LOGGER.debug("Registering protocol client for read: all bytes sent but parser not finished");
					key.interestOps(SelectionKey.OP_READ);
				}
				else {
					LOGGER.debug("Registering protocol client for read and write: not all bytes sent and parser not finished");
					key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);					
				}
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to write to protocol client: {}", e.getMessage());
			try {
				socketChannel.close();
			} catch (IOException e1) {
				LOGGER.error("Failed to close socket to protocol client: {}", e1.getMessage());
				e1.printStackTrace();
			}
		}
	}
}