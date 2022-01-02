package angelo.projects.terminal_chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChatClient {
	
	private AsynchronousSocketChannel socket;
	private String username;
	private InetSocketAddress serverAddress;
	
	private CompletionHandler<Integer, ByteBuffer> readHandler = new CompletionHandler<Integer, ByteBuffer>(){

		@Override
		public void completed(Integer result, ByteBuffer attachment) {
			//check if client has disconnected
			if(result < 0) {
				System.out.println("Connection closed. Disconnecting...");
				disconnect();
				return;
			}
			else {
				//process message
				ByteBuffer buffer = attachment;
				buffer.flip();
				try {
					//System.out.print("Message from server: " );
					System.out.write(buffer.array());
					System.out.println();
				} catch (IOException e) {
					e.printStackTrace();
				}
				//begin reading again
				read();
			}
			
		}

		@Override
		public void failed(Throwable exc, ByteBuffer attachment) {
			System.out.println("Failed to receive from server. Disconnecting...");
			disconnect();
			
		}
		
	};
	
	public ChatClient(String username, String hostname) {
		this.username = username;
		this.serverAddress = new InetSocketAddress(hostname, 12345);
	}
	
	/**Start connection to the supplied hostname
	 * @return if connection was successful
	 */
	public boolean connect() {
		try {
			socket = AsynchronousSocketChannel.open();
			Future<Void> future = socket.connect(serverAddress);
			future.get(60, TimeUnit.SECONDS);
			read();
			return true;
		}
		catch(TimeoutException e)
		{
			System.out.println("\nAttempt to connect timed out...");
			return false;
		}
		catch(Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
			return false;
		}
	}
	
	/**Disconnect from the server
	 * 
	 */
	public void disconnect() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**Send message to server
	 * @param message - message to send to server
	 * @return true if message was sent successfully, false otherwise and disconnect
	 */
	public boolean sendMessage(String message) {
		ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
		Future<Integer> result = socket.write(buffer);
		
		try {
			result.get();
			return true;
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			disconnect();
			return false;
		}
		
	}
	
	/**Send message with username attached to server
	 * @param message - message to send
	 * @return true if message was sent successfully, false otherwise and disconnect
	 */
	public boolean sendMessageAsUser(String message) {
		return sendMessage(username+": "+message);
	}
	
	/**Start reading concurrently
	 * 
	 */
	private void read() {
		ByteBuffer buffer = ByteBuffer.allocate(64);
		socket.read(buffer, buffer, readHandler);
	}

	public static void main(String[] args) {
		Scanner scn = new Scanner(System.in);
		//get username
		System.out.println("Please enter your username:");
		String username = scn.nextLine();
		
		ChatClient client = new ChatClient(username, "localhost");
		
		
		if(client.connect()) {
			System.out.println("Connected to server...");
			System.out.println("Enter 'exit' to close program");
			while(true) {
				String message = scn.nextLine();
				if(message.equals("exit")) {
					client.disconnect();
					break;
				}
				client.sendMessageAsUser(message);
			}
		}
		
		scn.close();

	}

}
