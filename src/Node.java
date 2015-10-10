import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Vector;


public class Node implements Runnable{
	
	private static int DEFAULT_MONITOR_PORT = 19999;
	private static int DEFAULT_PORT = 20000;
	private static String DEFAULT_MONITOR_HOST = "localhost";

	private Vector<NodeClient> peers;
	private Queue<NodeMessage> msgs;
	
	private ServerSocket srv;
	private Thread thread;
	private MonitorLink monitor;
	private int totalNodes;
	private int electingPower;
	
	private boolean electing = false;
	
	private NodeClient currentOfferNode;
	private int currentOfferCount;
	private NodeClient nextOfferNode;
	private int nextOfferCount;
	private boolean reffered = false;
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

	public Node(int port, String host, int hostport) throws IOException {
		this.srv = new ServerSocket(port);
		this.peers = new Vector<NodeClient>();
		this.msgs = new LinkedList<NodeMessage>();
		
		this.monitor = new MonitorLink(host, hostport, this);
		
		this.thread = new Thread(this);
		this.thread.start();
	}

	@Override
	public void run() {
		System.out.println("Online");
		try {
			Socket s = null;
			while((s = srv.accept()) != null){
				this.peers.add(new NodeClient(s, this));
				System.out.println("New Peer!");
			}
		} catch (IOException e) {
		}
	}

	public int getPort() {
		return this.srv.getLocalPort();
	}

	public void start(int i) {
		this.totalNodes = i;
		this.electingPower = 1;
		this.electing = true;
		Vector<NodeClient> choices = new Vector<NodeClient>();
		for(NodeClient c : peers){
			c.resetMessageCount();
			choices.add(c);
		}
		new Thread(new Runnable(){

			@Override
			public void run() {
				int nextPeer = 0;
				System.out.println("Starting...");
				while(electing){
					
					while(!msgs.isEmpty()){
						
						NodeMessage msg = msgs.poll();
						if(msg == null)
							continue;
					
						String[] words = msg.getMessage().split(" ");
						
						switch(words[0]){
						case "VOTE":
							int votes = Integer.parseInt(words[1]);
							if(electingPower > 0){
								if(currentOfferNode == null){
									electingPower += votes;
									msg.getClient().accept();
								}else{
									msg.getClient().busy();
								}
							}else{
								msg.getClient().reject(backedHost, backedPort);
							}
							break;
						case "ACCEPT":
							if(msg.getClient().equals(currentOfferNode) && currentOfferCount > -1){
								System.out.println(getPort() + ": power " + electingPower);
								electingPower -= currentOfferCount;
								currentOfferCount = -1;
								backedHost = currentOfferNode.getHost();
								backedPort = currentOfferNode.getPort();
								currentOfferNode = null;
							}
							break;
						case "REJECT":
							if(msg.getClient().equals(currentOfferNode) && currentOfferCount > -1){

								nextOfferNode = null;
								
								if(msg.getClient().getPort() == Integer.parseInt(words[2]) && msg.getClient().getHost().equals(words[1])){
									try {
										if(msg.getClient().getPort() == srv.getLocalPort() && msg.getClient().getHost().equals(InetAddress.getLocalHost().getHostAddress())){
											choices.remove(msg.getClient());
										}else{
											for(NodeClient c : choices){
												if(c.getPort() == Integer.parseInt(words[2]) && c.getHost().equals(words[1])){
													nextOfferNode = c;
													reffered = true;
													nextOfferCount = currentOfferCount;
													break;
												}
											}
										}
									} catch (UnknownHostException e) {
										System.err.println(e.getLocalizedMessage());
									}
								}
								if(nextOfferNode != currentOfferNode){
									choices.remove(msg.getClient());
								}
								currentOfferCount = -1;
								currentOfferNode = null;
							}
							break;
						case "BUSY":
							if(reffered){
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
					
					if(electingPower > totalNodes / 2){
						monitor.leader();
						break;
					}
					
					if(electingPower > 0 && currentOfferNode == null){
						if(nextOfferNode != null && nextOfferCount > -1){
							currentOfferNode = nextOfferNode;
							nextOfferNode = null;
							currentOfferCount = nextOfferCount;
							nextOfferCount = -1;
						}else if(choices.size() > 0){
							if(nextPeer >= choices.size())
								nextPeer %= choices.size();
							currentOfferNode = choices.get(nextPeer);
							//currentOfferCount = new Random().nextInt(electingPower);
							currentOfferCount = electingPower;
						}
						if(currentOfferNode != null)
							currentOfferNode.vote(currentOfferCount);
					}
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

	public void stop() {
		this.electing = false;
		int total = 0;
		for(NodeClient c : peers){
			total += c.getMessageCount();
		}
		this.monitor.reportMsgs(total);
	}

	public void connect(String host, int port) {
		try {
			this.peers.addElement(new NodeClient(new Socket(host, port), this));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void msg(NodeMessage nodeMessage) {
		this.msgs.add(nodeMessage);
	}

}
