import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class MonitorClient implements Runnable{
	
	private Socket socket;
	private Monitor monitor;
	private PrintWriter out;
	
	private int msgCount = -1;
	
	private int listeningPort;
	
	private String host;
	private Thread thread;

	public MonitorClient(Socket s, Monitor monitor) throws IOException {
		this.socket = s;
		this.monitor = monitor;
		this.out = new PrintWriter(socket.getOutputStream(), true);
		
		this.listeningPort = Integer.parseInt(new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine());
		this.host = this.socket.getInetAddress().getHostAddress();
		
		this.thread= new Thread(this);
		this.thread.start();
	}

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
					System.out.println("MONITOR: " + this + " message count: " + this.msgCount);
					monitor.countMessages();
					break;
				}
			}
		} catch (IOException e) {
			monitor.dead(this);
		}
	}
	
	public void start(int nodeCount){
		this.msgCount = -1;
		this.send("START " + nodeCount);
	}
	
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
