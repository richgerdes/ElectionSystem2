import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class NodeClient implements Runnable {
	
	private Socket sock;
	private String host;
	private int port;
	private Thread thread;
	private Node node;
	private PrintWriter out;
	
	private int messageCount = 0;

	public NodeClient(Socket s, Node n) throws IOException {
		this.sock = s;
		this.node = n;
		this.out = new PrintWriter(sock.getOutputStream(), true);
		this.out.println(this.node.getPort());
		
		String host = s.getRemoteSocketAddress().toString();
		this.host = host.substring(1, host.indexOf(":"));
		
		BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		this.port = Integer.parseInt(in.readLine());
		
		this.thread = new Thread(this);
		this.thread.start();
	}

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

	public void accept() {
		this.send("ACCEPT");
	}

	private void send(String string) {
		System.out.println(this.node + " >> " + this.host + ":" + this.port + ": " + string);
		messageCount++;
		this.out.println(string);
	}
	
	public String getHost(){
		return this.host;
	}
	
	public int getPort(){
		return this.port;
	}

	public void busy() {
		this.send("BUSY");
	}

	public void vote(int currentOfferCount) {
		this.send("VOTE " + currentOfferCount);
	}

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
