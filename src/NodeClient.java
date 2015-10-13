import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 
 * The NodeClient is the class used to connect between each node and its multiple peers.
 * A new NodeClient connection is made for each of the peers in the system.
 * It keeps track of the number of messages sent to the given peer. (Only outgoing, not incoming)
 *
 */
public class NodeClient implements Runnable {
	
	private Socket sock; 
	private String host; //Peer's host and port
	private int port;
	private Thread thread;
	private Node node;
	private PrintWriter out;
	
	private int messageCount = 0;

	//Connection set up
	public NodeClient(Socket s, Node n) throws IOException {
		this.sock = s;
		this.node = n;
		this.out = new PrintWriter(sock.getOutputStream(), true);
		this.out.println(this.node.getPort());
		
		String host = s.getRemoteSocketAddress().toString();
		this.host = host.substring(1, host.indexOf(":"));
		
		BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		this.port = Integer.parseInt(in.readLine());
		
		//Begin the thread that takes care of sending messages to the peer
		this.thread = new Thread(this);
		this.thread.start();
	}

	//Reads the responses from the peer and stores them in the node's message queue
	@Override
	public void run() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));
			String str = null;
			while((str = br.readLine())!= null){
				//System.out.println(this.node + " << " + this.host + ":" + this.port + ": " + str);
				this.node.msg(new NodeMessage(this, str));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Tell peer you accepted their votes
	public void accept() {
		this.send("ACCEPT");
	}

	//Send the peer the message and increment your message count
	private void send(String string) {
		//System.out.println(this.node + " >> " + this.host + ":" + this.port + ": " + string);
		messageCount++;
		this.out.println(string);
	}
	
	public String getHost(){
		return this.host;
	}
	
	public int getPort(){
		return this.port;
	}

	//Tell the peer you are currently busy processing a different request
	public void busy() {
		this.send("BUSY");
	}

	//Tell the peer you want to send it currentOfferCount votes
	public void vote(int currentOfferCount) {
		this.send("VOTE " + currentOfferCount);
	}

	//Tell the peer you have no votes so you are rejecting, 
	//but that they should try sending their votes to the last person you sent votes to
	public void reject(String backedHost, int backedPort) {
		this.send("REJECT " + backedHost + " " + backedPort);
	}
	
	public int getMessageCount(){
		return this.messageCount;
	}
	
	public void resetMessageCount(){
		this.messageCount = 0;
	}

}
