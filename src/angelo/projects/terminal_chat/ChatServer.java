package angelo.projects.terminal_chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ChatServer {
	AsynchronousServerSocketChannel serverSocket;
	ConcurrentLinkedQueue<AsynchronousSocketChannel> clientSockets;
	private final int PORT = 12345;
	
	private CompletionHandler<AsynchronousSocketChannel, Object> acceptHandler = new CompletionHandler<AsynchronousSocketChannel, Object>(){

		@Override
		public void completed(AsynchronousSocketChannel result, Object attachment) {
			//add connected client to list of clients
			clientSockets.add(result);
			
			try {
				System.out.println("\nConnection Successful by "+result.getRemoteAddress()+"...");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			//begin reading from socket
			read(result);
			
			//begin listening for connections again
			acceptConnections();
		}

		@Override
		public void failed(Throwable exc, Object attachment) {
			System.out.println("\nAccepting a connection failed...");
			System.err.println(exc);
		}
		
	};
	
	private CompletionHandler<Integer, MessageWrapper> readHandler = new CompletionHandler<Integer, MessageWrapper>(){

		@Override
		public void completed(Integer result, MessageWrapper attachment) {
			//check if client has disconnected
			if(result < 0) {
				try {
					System.out.println("\nClient from "+attachment.getSocket().getRemoteAddress()+" has disconnected. Closing connection...");
					disconnectClient(attachment.getSocket());
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			else {
				//process message
				ByteBuffer buffer = attachment.getBuffer();
				buffer.flip();
				try {
					System.out.print("Message from "+attachment.getSocket().getRemoteAddress()+": ");
					System.out.write(buffer.array());
					System.out.println();
					sendMessageToAll(new String(buffer.array()), attachment.getSocket());
				} catch (IOException e) {
					e.printStackTrace();
				}
				//begin reading again
				read(attachment.getSocket());
			}
			
		}

		@Override
		public void failed(Throwable exc, MessageWrapper attachment) {
			try {
				System.out.println("\nFailed to receive from "+attachment.getSocket().getRemoteAddress()+". Closing connection...");
				disconnectClient(attachment.getSocket());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	};
	
	public ChatServer() {
		clientSockets = new ConcurrentLinkedQueue<AsynchronousSocketChannel>();
	}
	
	/**Start the server connection and start concurrent task of listening for connections
	 * 
	 * @return true if connection was started successfully, otherwise returns false
	 */
	public boolean start() {
		try {
			serverSocket = AsynchronousServerSocketChannel.open();
			serverSocket.bind(new InetSocketAddress("127.0.0.1", PORT));
			acceptConnections();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**Stop the server's connections
	 * 
	 */
	public void stop() {
		try {
			serverSocket.close();
			clientSockets.forEach(sock -> {
				try {
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			clientSockets.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**Called to start accepting for connections
	 * 
	 */
	private void acceptConnections() {
		//begin listening for connections
		serverSocket.accept(null, acceptHandler);
	}
	
	/**Close the connection to the given socket
	 * @param client - socket to disconnect
	 */
	public void disconnectClient(AsynchronousSocketChannel client) {
		try {
			clientSockets.remove(client);
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**Start concurrent task of reading from a socket
	 * @param socket - socket to read from
	 */
	private void read(AsynchronousSocketChannel socket) {
		ByteBuffer buffer = ByteBuffer.allocate(64);
		socket.read(buffer, new MessageWrapper(buffer, socket), readHandler);
	}
	
	/**Send message to all clients except ignored client
	 * @param message - message to send to all clients
	 * @param ignoredClient - client to ignore, can be null if no ignored client
	 */
	public void sendMessageToAll(String message, AsynchronousSocketChannel ignoredClient) {
		ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
		clientSockets.forEach(socket -> {
			if(!socket.equals(ignoredClient)) {
				Future<Integer> future = socket.write(buffer);
				try {
					future.get(10, TimeUnit.SECONDS);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
	}
	
	public static void main(String[] args) {
		Scanner scn = new Scanner(System.in);
		ChatServer server = new ChatServer();
		server.start();
		while(true) {
			System.out.println("Enter 'exit' to close the program");
			String message = scn.nextLine();
			if(message.equals("exit")) {
				server.stop();
				break;
			}
		}
		scn.close();

	}
}
