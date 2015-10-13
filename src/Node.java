import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Vector;

/**
 * 
 * The Node class represents the actors/peers in the voting schema. It keeps track of all the available peers, 
 * all the peers waiting to hear back from it, and its electing power. It uses a MonitorLink to communicate between itself
 * and the MonitorClient.
 *
 */
public class Node implements Runnable{
	
	private static int DEFAULT_MONITOR_PORT = 19999;
	private static int DEFAULT_PORT = 20000;
	private static String DEFAULT_MONITOR_HOST = "localhost";

	private Vector<NodeClient> peers; //List of all peers in the system
	private Queue<NodeMessage> msgs; //Queue of all peers' messages waiting to be responded to
	
	private ServerSocket srv;
	private Thread thread;
	private MonitorLink monitor;
	private int totalNodes;
	private int electingPower;
	
	private boolean electing = false;
	
	//Current Node you have sent a request to
	private NodeClient currentOfferNode;
	private int currentOfferCount;
	
	//Information for next node to be contacted with
	private NodeClient nextOfferNode;
	private int nextOfferCount;
	
	//Whether or not this node was redirected to connect to a specific Node
	private boolean reffered = false;
	
	//Information about the last peer this node has passed votes to
	protected int backedPort;
	protected String backedHost;
	
	public Node() throws IOException{
		this(DEFAULT_PORT);
	}
	
	public Node(int port) throws IOException{
		this(port, DEFAULT_MONITOR_HOST, DEFAULT_MONITOR_PORT);
	}
	
	public Node(String host, int port) throws IOException{
		this(DEFAULT_PORT, host, port);
	}

	//Connection set up
	public Node(int port, String host, int hostport) throws IOException {
		this.srv = new ServerSocket(port);
		this.peers = new Vector<NodeClient>();
		this.msgs = new LinkedList<NodeMessage>();
		
		this.monitor = new MonitorLink(host, hostport, this);
		
		//Begin the thread that connects this node to all other nodes
		this.thread = new Thread(this);
		this.thread.start();
	}

	//Add all new peers to the peers list
	@Override
	public void run() {
		//System.out.println("Online");
		try {
			Socket s = null;
			while((s = srv.accept()) != null){
				this.peers.add(new NodeClient(s, this));
				//System.out.println("New Peer!");
			}
		} catch (IOException e) {
		}
	}

	//return this node's port
	public int getPort() {
		return this.srv.getLocalPort();
	}

	//Called by the monitor link, this method begins the election process
	public void start(int i) {
		this.totalNodes = i;
		this.electingPower = 1; //everyone begins with 1 vote
		this.electing = true; //this node is now in the electing phase
		
		//Create a new vector that keeps track of all the peers this node should connect to
		Vector<NodeClient> choices = new Vector<NodeClient>();
		for(NodeClient c : peers){
			c.resetMessageCount();
			choices.add(c);
		}
		
		//Election thread
		new Thread(new Runnable(){

			@Override
			public void run() {
				int nextPeer = 0;
				//System.out.println("Starting...");
				while(electing){
					
					//Make sure there are no pending requests to handle. If so, process those first
					while(!msgs.isEmpty()){
						
						NodeMessage msg = msgs.poll();
						if(msg == null)
							continue;
					
						String[] words = msg.getMessage().split(" ");
						
						switch(words[0]){
						case "VOTE": //peer wants to send votes
							int votes = Integer.parseInt(words[1]);
							if(electingPower > 0){ 
								//still have votes, so are still accepting votes. 
								if(currentOfferNode == null){
									//not currently processing any requests, take the votes
									electingPower += votes;
									msg.getClient().accept();
								}else{
									//currently in a different transaction. respond as busy
									msg.getClient().busy();
								}
							}else{
								//have no votes. do not accept more, but chain to the last node you sent votes to
								msg.getClient().reject(backedHost, backedPort);
							}
							break;
						case "ACCEPT": //peer accepted your votes
							if(msg.getClient().equals(currentOfferNode) && currentOfferCount > -1){
								//System.out.println(getPort() + ": power " + electingPower);
								//Subtract the offered votes from your count, reset your current node connections,
								//and record who this node you passed votes to is
								electingPower -= currentOfferCount;
								currentOfferCount = -1;
								backedHost = currentOfferNode.getHost();
								backedPort = currentOfferNode.getPort();
								currentOfferNode = null;
							}
							break;
						case "REJECT": //peer rejected offer because it has no more votes
							if(msg.getClient().equals(currentOfferNode) && currentOfferCount > -1){
								
								nextOfferNode = null;
								reffered = false;
								choices.remove(msg.getClient()); //remove it from choices, don't want to connect to it again
								
								//find the node it told you it sent votes to last and set that as the next node to send votes to
								for(NodeClient c : choices){
									if(c.getPort() == Integer.parseInt(words[2]) && c.getHost().equals(words[1])){
										nextOfferNode = c;
										reffered = true;
										nextOfferCount = currentOfferCount;
										break;
									}
								}
								
								if(nextOfferNode != currentOfferNode){
									choices.remove(msg.getClient());
								}
								currentOfferCount = -1;
								currentOfferNode = null;
							}
							break;
						case "BUSY": //peer is processing something else
							if(reffered){ 
								//were transfered to someone else, try connecting to that person next
								nextOfferCount = currentOfferCount;
								nextOfferNode = currentOfferNode;
							}
							currentOfferCount = -1;
							currentOfferNode = null;
							break;
						default:
							System.out.println("INVALID MESSAGE: " + msg.getMessage());
						}
						
					}
					
					//have more than the required amount of votes needed to be leader.
					//Notify monitor that you are the leader
					if(electingPower > totalNodes / 2){
						monitor.leader();
						break;
					}
					
					//you are still sending out votes to others. decide whom to vote for next
					if(electingPower > 0 && currentOfferNode == null){
						if(nextOfferNode != null && nextOfferCount > -1){
							//have been referred. Set this node as the next target
							currentOfferNode = nextOfferNode;
							nextOfferNode = null;
							currentOfferCount = nextOfferCount;
							nextOfferCount = -1;
						}else if(choices.size() > 0){
							//have not been referred. pick a node from the choices list
							if(nextPeer >= choices.size())
								nextPeer %= choices.size();
							currentOfferNode = choices.get(nextPeer);
							//currentOfferCount = new Random().nextInt(electingPower);
							currentOfferCount = electingPower; //send all the votes over
						}
						//vote for the next peer!
						if(currentOfferNode != null)
							currentOfferNode.vote(currentOfferCount);
					}
					//no requests to be processed currently. yield
					if(msgs.isEmpty())
						Thread.yield();
				}
			}
			
		}).start();
	}
	
	public String toString(){
		try {
			return InetAddress.getLocalHost().getHostAddress() + ":" + this.srv.getLocalPort();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	//Stop the election phase and report how many messages were sent by this node
	public void stop() {
		this.electing = false;
		int total = 0;
		for(NodeClient c : peers){
			total += c.getMessageCount();
		}
		this.monitor.reportMsgs(total);
	}

	//add node connection with given host and port to the peers list
	public void connect(String host, int port) {
		try {
			this.peers.addElement(new NodeClient(new Socket(host, port), this));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//add message to the queue to be processed
	public void msg(NodeMessage nodeMessage) {
		this.msgs.add(nodeMessage);
	}

}
