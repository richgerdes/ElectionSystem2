
public class NodeMessage {

	private NodeClient client;
	private String msg;
	
	public NodeMessage(NodeClient nodeClient, String str) {
		this.client = nodeClient;
		this.msg = str;
	}
	
	public NodeClient getClient(){
		return this.client;
	}
	
	public String getMessage(){
		return this.msg;
	}

}
