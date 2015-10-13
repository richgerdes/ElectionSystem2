import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

/**
 * 
 * Monitor class used to begin the election, be notified who the new leader is, and collect the number of messages
 * sent by each peer.
 * 
 * The monitor does not act as a CRM since it does not actually regulate the election and voting. It simply notifies
 * the other peers of who is online and steps back until a node tells it that it is the leader, at which point the monitor
 * waits to gather the number of messages sent by each individual node.
 *
 */
public class Monitor implements Runnable{
	
	private static int DEFAULT_PORT = 19999;
	
	private Vector<MonitorClient> nodes; //List of all the connects to each peer
	private ServerSocket srv;
	private Thread thread;
	private Thread threadConsole;
	
	//Constructor: assign the monitor the default port
	public Monitor() throws IOException{
		this(DEFAULT_PORT);
	}
	
	//Constructor: Initialize all variables and create a thread to begin the election
	public Monitor(int port) throws IOException{
		this.srv = new ServerSocket(port);
		this.nodes = new Vector<MonitorClient>();
		
		//starts the thread that collects all the peers
		this.thread = new Thread(this); 
		this.thread.start();
		
		this.threadConsole = new Thread(new Runnable(){

			@Override
			public void run() {
				//Wait for 'start' to be inputed to begin the election.
				try {
					System.out.print("Connections Initialized.\nType 'start' to begin the election: ");
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					String s = null;
					while((s = br.readLine())!=null){
						switch(s){
						case "start":
							start();
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		});
		
		this.threadConsole.start(); //Runs the thread that will wait to see if elections should be started
	}
	
	//Notifies all the nodes to start voting 
	protected void start() {
		//System.out.println("MONITOR: Starting...");
		for(MonitorClient c :  this.nodes){
			c.start(this.nodes.size());
		}
	}

	//Connect to all peers and notify them of all other peers already connected
	@Override
	public void run() {
		Socket s = null;
		try {
			while((s = srv.accept()) != null){ //listen for any new peers
				//create a connection for the peer and add it to the list of nodes
				MonitorClient client = new MonitorClient(s, this); 
				nodes.add(client);
				//notify all existing connections of the new connection
				for(MonitorClient c : this.nodes){
					if(client != c)
						c.annouce(client);
				}
				//System.out.println("MONITOR: New Connection");
			}
		} catch (IOException e) {
			System.exit(1);
		}
	}

	// Records who the leader is (as notified by the monitorClient passed here)
	// and stops all connections
	public void leader(MonitorClient monitorClient) {
		System.out.println("MONITOR: New Leader: " + monitorClient);
		for(MonitorClient c :  this.nodes){
			c.stop();
		}
	}
	
	// Goes through all the connections and counts up how many messages each connection sent.
	public void countMessages() {
		int total = 0;
		for(MonitorClient c : this.nodes){
			total += c.getMessageCount();
			if(c.getMessageCount() < 0)
				return;
		}
		
		System.out.println("MONITOR: Total Messages: " + total);
		
	}

	// In the case a monitor connection fails, it is removed from the list
	public void dead(MonitorClient monitorClient) {
		System.out.println("MONITOR: Connection Lost");
		this.nodes.remove(monitorClient);
	}

}
