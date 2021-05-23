package i5.las2peer.services.onyxDataProxyService.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXB;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.onyxDataProxyService.parser.ResultZipParser;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentMetadata;
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
	private static final String RESULT_STATE_COMPLETED = "Completed";

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

		String uri = OpalAPI.getCourseElementsURI(courseId);
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

	/**
	 * Fetches new assessment results since the given time (lastChecked) and generates
	 * xAPI statements for them.
	 * @param courseId Id of the course.
	 * @param nodeId Id of the node where assessment results should be fetched for.
	 * @param lastChecked Timestamp since when results should be fetched.
	 * @param courseElements 
	 * @return List containing Pair objects. Each of these Pair objects contains an assessment result
	 *         statement (left) and a list of its corresponding item result statements (right).
	 * @throws NodeNotAssessableException If the given node is not assessable or the course does not exist.
	 * @throws OpalAPIException If something else with the request to Opal API went wrong.
	 */
	public List<Pair<String, List<String>>> getResultsAfter(String courseId, String nodeId, long lastChecked,
			List<Pair<courseNodeVO, Boolean>> courseElements) throws NodeNotAssessableException, OpalAPIException {
		HttpClient c = login();
		
		// find course element
		courseNodeVO courseElement = null;
		for(Pair<courseNodeVO, Boolean> node : courseElements) {
			if(node.getLeft().id.equals(nodeId)) {
				courseElement = node.getLeft();
				break;
			}
		}
		
		if(courseElement == null) 
			throw new OpalAPIException("Given courseElements list does not contain an element with the nodeId " + nodeId);

		String uri = OpalAPI.getTimeResultsURI(courseId, nodeId, lastChecked);
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
				throw new NodeNotAssessableException();
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
					// there is at least one resultsVO item that also contains a resultVO
					for (resultVO resultVO : resultsVO.resultVO) {
						// check if result state is already completed
						// otherwise we do not want to generate a statement
						if(resultVO.state.equals(RESULT_STATE_COMPLETED)) {
							// there is a new item
							newItem = true;
							
						    studentMappings.put(getStudentMappingItem(nodeId, resultVO, resultsVO.userVO));
						}
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
					
					AssessmentMetadata am = new AssessmentMetadata();
					am.setId(courseElement.id);
					am.setDescription(courseElement.learningObjectives);
					am.setTitle(courseElement.shortTitle);
					
					return ResultZipParser.processResults(studentMappings, am, logger);
				} catch (IOException e) {
					e.printStackTrace();
					throw new OpalAPIException("Error downloading zip from secret link.");
				}
			}
		}
		// no new statements
		return new ArrayList<>();
	}
	
	/**
	 * Creates the JSON mapping which maps result files to the corresponding student.
	 * @param nodeId
	 * @param resultVO
	 * @param userVO
	 * @return
	 */
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
	
	/**
	 * Generates xAPI statements for access statistics of each node of the course.
	 * @param courseId
	 * @param courseElements Used to get the node titles (in human language).
	 * @param lastChecked
	 * @return List of xAPI statements as Strings.
	 * @throws OpalAPIException
	 */
	public List<String> getCourseAccessStatisticsAfter(String courseId, List<Pair<courseNodeVO, Boolean>> courseElements, 
			long lastChecked) throws OpalAPIException {	
		HttpClient c = login();
		
		String uri = OpalAPI.getTimeCourseAccessStatisticsURI(courseId, lastChecked);
		GetMethod method = new GetMethod(uri);
		method.getParams().setCookiePolicy(CookiePolicy.RFC_2109);

		int responseCode;
		String body;
		try {
			responseCode = c.executeMethod(method);
			body = method.getResponseBodyAsString();
		} catch (IOException e) {
			e.printStackTrace();
			throw new OpalAPIException("Error fetching course access statistics.");
		}

		if (responseCode != 200) {
			throw new OpalAPIException("Error fetching course access statistics. Status code: " + responseCode);
		}
		
		Object parseResult = parseXml(body, statisticVOes.class);
		statisticVOes res = (statisticVOes) parseResult;
		
		ArrayList<String> statements = new ArrayList<>();
		
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String lastCheckedStr = formatter.format(lastChecked);
		
		if(res.statisticVO != null) {
			for(statisticVO s : res.statisticVO) {
				String nodeId = s.id;
				
				// find course element
				courseNodeVO courseElement = null;
				for(Pair<courseNodeVO, Boolean> node : courseElements) {
					if(node.getLeft().id.equals(nodeId)) {
						courseElement = node.getLeft();
						break;
					}
				}
				
				int accesses = s.accesses;
				statements.add(StatementBuilder.createCourseNodeAccessStatisticStatement(
						courseId, nodeId, courseElement.shortTitle, accesses, lastCheckedStr).toString());
			}
		}
		return statements;
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
	
	private static String getTimeResultsURI(String courseId, String nodeId, long lastChecked) {
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		long now = System.currentTimeMillis();
		String endDate = formatter.format(now);
		String lastCheckedStr = formatter.format(lastChecked);
		
		return OpalAPI.getResultsURI(courseId, nodeId) + "?startdate=" + lastCheckedStr + "&enddate=" + endDate;
	}

	private static String getResultsURI(String courseId, String nodeId) {
		return OPAL_API_BASE_URL + "repo/courses/" + courseId + "/assessments/" + nodeId + "/results";
	}
	
	private static String getTimeCourseAccessStatisticsURI(String courseId, long lastChecked) {
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String lastCheckedStr = formatter.format(lastChecked);
		
		return OpalAPI.getCourseAccessStatisticsURI(courseId) + "?startdate=" + lastCheckedStr + "&enddate=" + lastCheckedStr;
	}
	
	private static String getCourseAccessStatisticsURI(String courseId) {
		return OPAL_API_BASE_URL + "repo/courses/" + courseId + "/statistic";
	}
	
	private static String getCourseElementsURI(String courseId) {
		return OPAL_API_BASE_URL + "repo/courses/" + courseId + "/elements";
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
	
	private static class statisticVO {
        public String id;
        public Integer accesses;
    }

    private static class statisticVOes {
        public List<statisticVO> statisticVO = new ArrayList<>();
    }

}
