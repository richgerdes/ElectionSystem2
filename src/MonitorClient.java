import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 
 * The MonitorClient class is the socket used for communication between the Monitor and the individual nodes.
 * 
 */
public class MonitorClient implements Runnable{
	
	private Socket socket;
	private Monitor monitor;
	private PrintWriter out;
	
	private int msgCount = -1;
	
	private int listeningPort;
	
	private String host;
	private Thread thread;

	//Set up the socket and all related connections
	public MonitorClient(Socket s, Monitor monitor) throws IOException {
		this.socket = s;
		this.monitor = monitor;
		this.out = new PrintWriter(socket.getOutputStream(), true);
		
		this.listeningPort = Integer.parseInt(new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine());
		this.host = this.socket.getInetAddress().getHostAddress();
		
		//Begin thread to listen for the node
		this.thread= new Thread(this);
		this.thread.start();
	}

	//Listen for the node to tell the monitor if the node is the leader and/or 
	// to send the number of messages sent by the node to the monitor to tally
	@Override
	public void run() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			String s = null;
			while((s = br.readLine())!=null){
				String[] w = s.split(" ");
				switch(w[0]){
				case "LEADER":
					monitor.leader(this);
					break;
				case "MESSAGES":
					this.msgCount = Integer.parseInt(w[1]);
					//System.out.println("MONITOR: " + this + " message count: " + this.msgCount);
					monitor.countMessages();
					break;
				}
			}
		} catch (IOException e) {
			monitor.dead(this);
		}
	}
	
	// Initialize the connection in case the election is started twice in one run.
	// Notify the user that this connection has been started.
	public void start(int nodeCount){
		this.msgCount = -1;
		this.send("START " + nodeCount);
	}
	
	// Indicate that the connection has stopped
	public void stop(){
		this.send("STOP");
	}
	
	public void send(String str){
		this.out.println(str);
	}
	
	public String toString(){
		return this.host + " " + this.listeningPort;
	}
	
	public int getMessageCount(){
		return this.msgCount;
	}

	public void annouce(MonitorClient c) {
		this.send("CONNECT " + c);
	}

}
