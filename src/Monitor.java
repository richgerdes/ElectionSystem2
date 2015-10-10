import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;


public class Monitor implements Runnable{
	
	private static int DEFAULT_PORT = 19999;
	
	private Vector<MonitorClient> nodes;
	private ServerSocket srv;
	private Thread thread;
	private Thread threadConsole;
	
	public Monitor() throws IOException{
		this(DEFAULT_PORT);
	}
	
	public Monitor(int port) throws IOException{
		this.srv = new ServerSocket(port);
		this.nodes = new Vector<MonitorClient>();
		
		this.thread = new Thread(this);
		this.thread.start();
		
		this.threadConsole = new Thread(new Runnable(){

			@Override
			public void run() {
				try {
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
		
		this.threadConsole.start();
	}

	protected void start() {
		System.out.println("MONITOR: Starting...");
		for(MonitorClient c :  this.nodes){
			c.start(this.nodes.size());
		}
	}

	@Override
	public void run() {
		Socket s = null;
		try {
			while((s = srv.accept()) != null){
				MonitorClient client = new MonitorClient(s, this);
				nodes.add(client);
				for(MonitorClient c : this.nodes){
					if(client != c)
						c.annouce(client);
				}
				System.out.println("MONITOR: New Connection");
			}
		} catch (IOException e) {
			System.exit(1);
		}
	}

	public void leader(MonitorClient monitorClient) {
		System.out.println("MONITOR: New Leader: " + monitorClient);
		for(MonitorClient c :  this.nodes){
			c.stop();
		}
	}

	public void countMessages() {
		int total = 0;
		for(MonitorClient c : this.nodes){
			total += c.getMessageCount();
			if(c.getMessageCount() < 0)
				return;
		}
		
		System.out.println("MONITOR: Total Messages: " + total);
		
	}

	public void dead(MonitorClient monitorClient) {
		System.out.println("MONITOR: Connection Lost");
		this.nodes.remove(monitorClient);
	}

}
