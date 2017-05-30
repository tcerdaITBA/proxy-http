package tp.pdc.proxy.handler;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import tp.pdc.proxy.handler.interfaces.Handler;

public abstract class HttpHandler implements Handler {	
	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffer;
	private ByteBuffer processedBuffer;
	private SelectionKey connectedPeerKey;
	
	public HttpHandler(int readBufferSize, ByteBuffer writeBuffer, ByteBuffer processedBuffer) {
		this.readBuffer = ByteBuffer.allocate(readBufferSize);
		this.writeBuffer = writeBuffer;
		this.processedBuffer = processedBuffer;
	}
	
	abstract protected void processRead(SelectionKey key);
	abstract protected void process(ByteBuffer inputBuffer, SelectionKey key);
	abstract protected void processWrite(ByteBuffer inputBuffer, SelectionKey key);
	
	public ByteBuffer getReadBuffer() {
		return readBuffer;
	}
	
	public ByteBuffer getWriteBuffer() {
		return writeBuffer;
	}
	
	public ByteBuffer getProcessedBuffer() {
		return processedBuffer;
	}
	
	public SelectionKey getConnectedPeerKey() {
		return connectedPeerKey;
	}
	
	public void setWriteBuffer(ByteBuffer writeBuffer) {
		this.writeBuffer = writeBuffer;
	}
	
	public void setProcessedBuffer(ByteBuffer processedBuffer) {
		this.processedBuffer = processedBuffer;
	}
	
	public void setConnectedPeerKey(SelectionKey connectedPeerKey) {
		this.connectedPeerKey = connectedPeerKey;
	}
	
	public void handleRead(SelectionKey key) {
		processRead(key);
		
		if (key.isValid()) // in case of EOF was received
			processReadBuffer(key);
	}

	public void handleWrite(SelectionKey key) {
		writeBuffer.flip();
		processWrite(writeBuffer, key);
		writeBuffer.compact();
	}
	
	public void handleProcess(SelectionKey key) {
		if (key.isValid() && readBuffer.position() != 0) {
			processedBuffer.compact();
			processReadBuffer(key);
			
			if (processedBuffer != null)
				processedBuffer.flip();
		}
	}
	
	private void processReadBuffer(SelectionKey key) {
		readBuffer.flip();
		process(readBuffer, key);
		readBuffer.compact();
	}
	
}
