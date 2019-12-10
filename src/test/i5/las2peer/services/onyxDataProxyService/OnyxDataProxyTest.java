package i5.las2peer.services.onyxDataProxyService;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.LocalNodeManager;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;

public class OnyxDataProxyTest {
	private static LocalNode node;

	private static UserAgentImpl testAgent;
	private static final String testPass = "adamspass";
	private static final ServiceNameVersion snv = new ServiceNameVersion(OnyxDataProxyService.class.getName(), "1.0.0");

	/**
	 * Called before a test starts.
	 * 
	 * Sets up the node, initializes connector and adds user agent that can be used throughout the test.
	 * 
	 * @throws Exception
	 */
	@Before
	public void startServer() throws Exception {
		// start node
		node = new LocalNodeManager().newNode();
		node.launch();

		// add agent to node
		testAgent = MockAgentFactory.getAdam();
		testAgent.unlock(testPass); // agents must be unlocked in order to be stored
		node.storeAgent(testAgent);

		// start service
		// during testing, the specified service version does not matter
		node.startService(snv, "a pass");

	}

	/**
	 * Called after the test has finished. Shuts down the server and prints out the connector log file for reference.
	 * 
	 * @throws Exception
	 */
	@After
	public void shutDownServer() throws Exception {

		if (node != null) {
			node.shutDown();
			node = null;
		}

	}

	/**
	 * 
	 * Test to get menus for some available canteens.
	 * 
	 */
	@Test
	public void testCorrectAssessments() {
		try {
			testAgent.unlock(testPass);
			// TODO
			// node.invoke(testAgent, snv, "addAssessment",
			// new Serializable[] { (Serializable) new FileInputStream(new File("")), });
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}
}
