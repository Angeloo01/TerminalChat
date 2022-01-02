package angelo.projects.terminal_chat;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChatClient {
	
	private AsynchronousSocketChannel socket;
	private String username;
	private InetSocketAddress serverAddress;
	
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
	
	public void sendMessage(String message) {
		
	}

	public static void main(String[] args) {
		ChatClient client = new ChatClient("user", "localhost");
		if(client.connect()) {
			System.out.println("Connected to server...");
			while(true) {
				try {
					TimeUnit.SECONDS.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
