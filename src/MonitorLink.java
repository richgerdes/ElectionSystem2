import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;


public class MonitorLink implements Runnable {
	
	private Socket socket;
	private Thread thread;
	private PrintWriter out;
	private Node node;

	public MonitorLink(String host, int port, Node n) throws UnknownHostException, IOException {
		this.node = n;
		this.socket = new Socket(host, port);
		this.out = new PrintWriter(this.socket.getOutputStream(), true);
		
		this.out.println(n.getPort());
		
		this.thread = new Thread(this);
		this.thread.start();
		
	}

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
				case "START":
					this.node.start(Integer.parseInt(words[1]));
					break;
				case "STOP":
					this.node.stop();
					break;
				case "CONNECT":
					this.node.connect(words[1], Integer.parseInt(words[2]));
					break;
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void reportMsgs(int i) {
		this.send("MESSAGES " + i);
	}

	private void send(String string) {
		//System.out.println(">>("+ node + "): " + string);
		this.out.println(string);
	}

	public void leader() {
		this.send("LEADER");
	}

}
