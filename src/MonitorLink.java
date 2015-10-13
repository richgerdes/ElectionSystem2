import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * 
 * The MonitorLink class controls the actual communication between a Node and its MonitorClient
 *
 */
public class MonitorLink implements Runnable {
	
	private Socket socket;
	private Thread thread;
	private PrintWriter out;
	private Node node;

	//Constructor to initiate the link
	public MonitorLink(String host, int port, Node n) throws UnknownHostException, IOException {
		this.node = n;
		this.socket = new Socket(host, port);
		this.out = new PrintWriter(this.socket.getOutputStream(), true);
		
		this.out.println(n.getPort());
		
		//Begin listening to the MonitorClient to tell the Node what actions to perform
		this.thread = new Thread(this);
		this.thread.start();
		
	}

	//Read commands given from the MonitorClient to the MonitorLink to control the Node
	@Override
	public void run() {
		
		this.send(this.node.getPort() + "");
		
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			String str = null;
			while((str = in.readLine()) != null){
				//System.out.println("<<("+ node + "): " + str);
				String[] words = str.split(" ");
				switch(words[0]){
				case "START": //start the election with Integer.parseInt(words[1]) peers
					this.node.start(Integer.parseInt(words[1]));
					break;
				case "STOP": //stop election
					this.node.stop();
					break;
				case "CONNECT": //connect to Node on given address and port number
					this.node.connect(words[1], Integer.parseInt(words[2]));
					break;
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	//Report to the MonitorClient how many messages were sent by the node
	public void reportMsgs(int i) {
		this.send("MESSAGES " + i);
	}

	private void send(String string) {
		//System.out.println(">>("+ node + "): " + string);
		this.out.println(string);
	}

	//Notify MonitorClient that this Node was elected leader
	public void leader() {
		this.send("LEADER");
	}

}
