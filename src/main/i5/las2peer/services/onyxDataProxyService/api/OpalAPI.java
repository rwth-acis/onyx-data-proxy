package i5.las2peer.services.onyxDataProxyService.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXB;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONObject;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.onyxDataProxyService.parser.AssessmentResultParser;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.AssessmentResult;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.ItemResult;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentMetadata;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentUser;
import i5.las2peer.services.onyxDataProxyService.utils.ZipHelper;
import i5.las2peer.services.onyxDataProxyService.xApi.StatementBuilder;

/**
 * Helper class for usage of Opal API.
 * Uses the credentials given through the constructor to access Opal API.
 * @author Philipp
 *
 */
public class OpalAPI {

	private static final String OPAL_API_BASE_URL = "https://bildungsportal.sachsen.de/opal/restapi/";

	/**
	 * Username of the account used to access the Opal API.
	 */
	private String username;
	
	/**
	 * Password of the account used to access the Opal API.
	 */
	private String password;
	
	private L2pLogger logger;

	/**
	 * Main constructor that gets called when using the API.
	 * @param username Username of the account used to access the Opal API.
	 * @param password Password of the account used to access the Opal API.
	 * @param logger
	 */
	public OpalAPI(String username, String password, L2pLogger logger) {
		this.username = username;
		this.password = password;
		this.logger = logger;
	}
	
	/**
	 * Fetches the /repo/courses/{courseId}/elements endpoint.
	 * Returns a list of all course nodes that exist for the course with the given id.
	 * @param courseId Id of the course for which the elements should be loaded from Opal API.
	 * @return List of course nodes/elements.
	 * @throws OpalAPIException If sending the GET request failed or the response code is not 200.
	 */
	public List<courseNodeVO> getCourseElements(String courseId) throws OpalAPIException {
		HttpClient c = login();

		String uri = OPAL_API_BASE_URL + "repo/courses/" + courseId + "/elements";
		logger.info("Sending request to: " + uri);

		GetMethod method = new GetMethod(uri);
		method.getParams().setCookiePolicy(CookiePolicy.RFC_2109);

		int responseCode;
		String body;
		try {
			responseCode = c.executeMethod(method);
			body = method.getResponseBodyAsString();
		} catch (IOException e) {
			e.printStackTrace();
			throw new OpalAPIException("Error fetching course elements.");
		}

		if (responseCode != 200) {
			throw new OpalAPIException("Error fetching course elements. Status code: " + responseCode);
		}

		// parse XML result to courseNodeVOes object which then contains the list of courseNodeVO objects
		Object parseResult = parseXml(body, courseNodeVOes.class);
		return ((courseNodeVOes) parseResult).courseNodeVO;
	}

	public List<String> getResultsAfter(String courseId, String nodeId, long lastChecked,
			List<courseNodeVO> courseElements) throws OpalAPIException {
		HttpClient c = login();
		
		// find course element
		courseNodeVO courseElement = null;
		for(courseNodeVO node : courseElements) {
			if(node.id.equals(nodeId)) {
				courseElement = node;
				break;
			}
		}
		
		if(courseElement == null) 
			throw new OpalAPIException("Given courseElements list does not contain an element with the nodeId " + nodeId);

		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		long now = System.currentTimeMillis();
		String endDate = formatter.format(now);
		String lastCheckedStr = formatter.format(lastChecked);

		String uri = OpalAPI.getResultsURI(courseId, nodeId) + "?startdate=" + lastCheckedStr + "&enddate=" + endDate;
		logger.info("Sending request to: " + uri);
		GetMethod method = new GetMethod(uri);
		method.getParams().setCookiePolicy(CookiePolicy.RFC_2109);

		int responseCode;
		String body;
		try {
			responseCode = c.executeMethod(method);
			body = method.getResponseBodyAsString();
		} catch (IOException e) {
			e.printStackTrace();
			throw new OpalAPIException("Error fetching results.");
		}

		if (responseCode != 200) {
			if (responseCode == 404) 
				throw new OpalAPIException("Course " + courseId + " or node " + nodeId + " could not be found or node is no assessment.");
			throw new OpalAPIException("Error fetching results. Status code: " + responseCode);
		}

		Object parseResult = parseXml(body, resultsVOes.class);
		resultsVOes res = (resultsVOes) parseResult;

		if (res.resultsVO.size() > 0) {
			// there are resultsVO items (but maybe they only contain the userVO)
			boolean newItem = false;
			JSONArray studentMappings = new JSONArray();
			for (resultsVO resultsVO : res.resultsVO) {
				if (resultsVO.resultVO != null && resultsVO.resultVO.size() > 0) {
					// there is a new item
					newItem = true;

					for (resultVO resultVO : resultsVO.resultVO) {
						studentMappings.put(getStudentMappingItem(nodeId, resultVO, resultsVO.userVO));
					}
				}
			}

			if (newItem) {
				// there are new results
				// get secret link from last resultsVO item
				resultsVO secretLinkResultsVO = res.resultsVO.get(res.resultsVO.size() - 1);
				String secretLink = secretLinkResultsVO.data;
				if (secretLink == null) {
					throw new OpalAPIException("There are new results but secret link is not the last item.");
				}
				logger.info("Secret link is: " + secretLinkResultsVO.data);

				try {
					InputStream inputStream = new URL(secretLink).openStream();
					ZipHelper.extractFiles(inputStream, "tmp");
					return this.processResults(studentMappings, courseElement);
				} catch (IOException e) {
					e.printStackTrace();
					throw new OpalAPIException("Error downloading zip from secret link.");
				}
			}
		}
		// no new statements
		return new ArrayList<>();
	}
	
	private JSONObject getStudentMappingItem(String nodeId, resultVO resultVO, userVO userVO) {
		String email = userVO.email;
		String firstName = userVO.firstName;
		String lastName = userVO.lastName;
		
		JSONObject mappingItem = new JSONObject();
		mappingItem.put("onyxResultFilename", nodeId + "v" + resultVO.key + ".xml");
		mappingItem.put("email", email);
		mappingItem.put("firstName", firstName);
		mappingItem.put("lastName", lastName);
		return mappingItem;
	}

	private List<String> processResults(JSONArray studentMappings, courseNodeVO elementInfo) {
		List<String> xApiStatements = new ArrayList<>();
		
		File dir = new File("tmp");
		File[] directoryListing = dir.listFiles();

		for (Object mapping : studentMappings) {
			if (mapping instanceof JSONObject) {
				JSONObject mappingJSON = (JSONObject) mapping;
				String onyxResultFilename = mappingJSON.getString("onyxResultFilename");
				String email = mappingJSON.getString("email");
				String firstName = mappingJSON.getString("firstName");
				String lastName = mappingJSON.getString("lastName");

				AssessmentUser user = new AssessmentUser();
				user.setFirstName(firstName);
				user.setLastName(lastName);
				user.setEmail(email);

				File onyxFile = new File("tmp/" + onyxResultFilename);
				if(!onyxFile.exists()) {
					logger.info("File " + onyxFile.getAbsolutePath() + " does not exist.");
					// .xml file does not exist
					// sometimes it is the case, that a .zip file with the same name exists instead
					// this is the case e.g. if the students could upload/attach something to the test
					// check if a zip file with the same name exists
					String nameWithoutExtension = onyxFile.getAbsolutePath();
					nameWithoutExtension = nameWithoutExtension.substring(0, nameWithoutExtension.lastIndexOf('.'));
					File onyxFileZip = new File(nameWithoutExtension + ".zip");
					if(onyxFileZip.exists()) {
						// the zip file exists, it should contain a file named result.xml which we can use
						try {
							ZipHelper.extractFiles(new FileInputStream(onyxFileZip), nameWithoutExtension);
							onyxFile = new File(nameWithoutExtension, "result.xml");
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

				String xml;
				try {
					xml = new String(Files.readAllBytes(onyxFile.toPath()));
					AssessmentResult ar = AssessmentResultParser.parseAssessmentResult(xml);

					AssessmentMetadata am = new AssessmentMetadata();
					am.setId(elementInfo.id);
					am.setDescription(elementInfo.learningObjectives);
					am.setTitle(elementInfo.shortTitle);

					JSONObject xApiStatement = StatementBuilder.createAssessmentResultStatement(ar, user, am);
					xApiStatements.add(xApiStatement.toString());
					// Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_1,
					// xApiStatement.toString());
					for (ItemResult ir : ar.getItemResults()) {
						xApiStatement = StatementBuilder.createItemResultStatement(ir, user, am);
						xApiStatements.add(xApiStatement.toString());
						
						// Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2,
						// xApiStatement.toString());
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return xApiStatements;
	}

	/**
	 * Returns a HttpClient object that already called the auth endpoint and is logged in.
	 * @return HttpClient object that already called the auth endpoint and is logged in.
	 * @throws OpalAPIException If executing the GET request failed or if the response code is not 200.
	 */
	private HttpClient login() throws OpalAPIException {
		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod(OPAL_API_BASE_URL + "auth/" + username + "?password=" + password);
		method.getParams().setCookiePolicy(CookiePolicy.RFC_2109);

		int responseCode;
		try {
			responseCode = client.executeMethod(method);
		} catch (IOException e) {
			e.printStackTrace();
			throw new OpalAPIException("Executing GET request for login failed.");
		}

		if (responseCode != 200)
			throw new OpalAPIException("Login failed.");

		return client;
	}

	private static String getResultsURI(String courseId, String nodeId) {
		return OPAL_API_BASE_URL + "repo/courses/" + courseId + "/assessments/" + nodeId + "/results";
	}

	@SuppressWarnings("unchecked")
	private Object parseXml(String body, Class c) {
		return JAXB.unmarshal(new StringReader(body), c);
	}

	private static class property {
		public String name;
		public String value;
	}

	private static class properties {
		public List<property> property = new ArrayList<>();
	}

	private static class userVO {
		public Long key;
		public String login;
		public String password;
		public String firstName;
		public String lastName;
		public String email;
		public properties properties;
	}

	private static class resultVO {
		public String key;
		public Date startDate;
		public Date endDate;
		public Long duration;
		public Double maxScore;
		public Float score;
		public Boolean passed;
		public String state;
		public Boolean scored;
	}

	private static class resultsVO {
		public userVO userVO;
		public List<resultVO> resultVO = new ArrayList<>();
		public String data;
	}

	private static class resultsVOes {
		public List<resultsVO> resultsVO = new ArrayList<>();
	}

	public static class courseNodeVO {
		public String id;
		public String shortTitle;
		public String learningObjectives; // this is the description
	}

	public static class courseNodeVOes {
		public List<courseNodeVO> courseNodeVO = new ArrayList<>();
	}

}
