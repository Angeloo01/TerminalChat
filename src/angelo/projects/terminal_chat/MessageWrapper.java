package angelo.projects.terminal_chat;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

public class MessageWrapper {
	private ByteBuffer buffer;
	private AsynchronousSocketChannel socket;
	
	public MessageWrapper(ByteBuffer buffer, AsynchronousSocketChannel socket) {
		this.buffer = buffer;
		this.socket = socket;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

	public void setBuffer(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	public AsynchronousSocketChannel getSocket() {
		return socket;
	}

	public void setSocket(AsynchronousSocketChannel socket) {
		this.socket = socket;
	}
	
	
}
