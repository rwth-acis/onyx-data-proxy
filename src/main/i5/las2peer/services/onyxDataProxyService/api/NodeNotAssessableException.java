package i5.las2peer.services.onyxDataProxyService.api;

public class NodeNotAssessableException extends OpalAPIException {
	
	public NodeNotAssessableException() {
		super("Node is not assessable (or the course does not exist).");
	}

}
