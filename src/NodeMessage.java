/**
 * 
 * The NodeMessage class is used to give a structure to the messages sent between peers and nodes
 * in order to maintain standard formatting.
 *
 */
public class NodeMessage {

	private NodeClient client; //who is sending the message
	private String msg; //what is the message
	
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
