package i5.las2peer.services.onyxDataProxyService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.services.onyxDataProxyService.api.NodeNotAssessableException;
import i5.las2peer.services.onyxDataProxyService.api.OpalAPI;
import i5.las2peer.services.onyxDataProxyService.api.OpalAPIException;
import i5.las2peer.services.onyxDataProxyService.api.OpalAPI.courseNodeVO;
import i5.las2peer.services.onyxDataProxyService.parser.AssessmentMetadataParser;
import i5.las2peer.services.onyxDataProxyService.parser.AssessmentResultParser;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.AssessmentResult;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.ItemResult;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentMetadata;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentUser;
import i5.las2peer.services.onyxDataProxyService.utils.ZipHelper;
import i5.las2peer.services.onyxDataProxyService.xApi.StatementBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;

@Api
@SwaggerDefinition(
		info = @Info(
				title = "Onyx Data Proxy Service",
				version = "1.0.0",
				description = "A proxy for onyx data",
				contact = @Contact(
						name = "Alexander Tobias Neumann",
						email = "neumann@dbis.rwth-aachen.de")))

/**
 * 
 * This service creates xAPI statements out of Onyx data.
 * It can be used in two different ways:
 * 1) Using the Opal API to fetch new assessment results regularly and automatically.
 * 2) Using the /assessments endpoint one can send data to be processed manually.
 * 
 */
@ManualDeployment
@ServicePath("onyx")
public class OnyxDataProxyService extends RESTService {
	
	private final static L2pLogger logger = L2pLogger.getInstance(OnyxDataProxyService.class.getName());

	private final static int OPAL_DATA_STREAM_PERIOD = 60; // Every minute
	
	private static ScheduledExecutorService dataStreamThread = null;
	private static Context context = null;
	
	private static long lastChecked = 0;
	
	/**
	 * Course list can be set using the service properties file.
	 */
	private String courseList;
	private static HashSet<Long> courses = new HashSet<Long>();
	
	/**
	 * Username of the account used to access the Opal API.
	 * Should be configured using the service properties file.
	 */
	private String opalUsername;
	
	/**
	 * Password of the account used to access the Opal API.
	 * Should be configured using the service properties file.
	 */
	private String opalPassword;
	
	/**
	 * Maps every course id to a list of Pair objects containing the 
	 * course nodes and a boolean value determining whether the node is 
	 * assessable or not.
	 * Note: In the beginning, the assessable value might be true, even if the 
	 * node is not assessable. After the first request of results for a node,
	 * the assessable value will be correct.
	 */
	private static HashMap<Long, List<Pair<courseNodeVO, Boolean>>> courseElementsMap = new HashMap<>();
	
	/**
	 * 
	 * Constructor of the Service.
	 * 
	 */
	public OnyxDataProxyService() {
		setFieldValues(); // This sets the values of the configuration file
		this.updateCourseList();
		this.updateCourseElementsMap();
		
		if(OnyxDataProxyService.lastChecked == 0) {
			// Get current time
			TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
			lastChecked = System.currentTimeMillis();
		}
		
		// testing:
		/*lastChecked = 0;
		dataStreamThread = Executors.newSingleThreadScheduledExecutor();
		dataStreamThread.scheduleAtFixedRate(new DataStreamThread(), 0, OPAL_DATA_STREAM_PERIOD, TimeUnit.SECONDS);*/
	}

	/**
	 * Describe MobSOS log messages
	 */
	@Override
	public Map<String, String> getCustomMessageDescriptions() {
		Map<String, String> descriptions = new HashMap<>();
		descriptions.put("SERVICE_CUSTOM_MESSAGE_1", "Sent assessment result to lrs.");
		descriptions.put("SERVICE_CUSTOM_MESSAGE_2", "Sent item result of an assessment to lrs.");
		descriptions.put("SERVICE_CUSTOM_ERROR_1", "Cannot delete processed files.");
		return descriptions;
	}
	
	/**
	 * Reads the course list from the service properties file.
	 */
	private void updateCourseList() {
		courses.clear();
		if (courseList != null && courseList.length() > 0) {
			try {
				logger.info("Reading courses from provided list.");
				String[] idStrings = courseList.split(",");
				for (String courseid : idStrings) {
					courses.add(Long.parseLong(courseid));
				}
				logger.info("Updating course list was successful: " + courses);
				return;
			} catch (Exception e) {
				logger.severe("Reading course list failed");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Fetches the course elements from Opal API for every course ID and updates
	 * the courseElementsMap.
	 */
	private void updateCourseElementsMap() {
		courseElementsMap.clear();
		OpalAPI api = new OpalAPI(opalUsername, opalPassword, logger);
		for(long courseID : courses) {
			try {
				logger.info("Loading course elements for course " + courseID);
				// fetch course nodes from Opal API
				List<courseNodeVO> courseNodes = api.getCourseElements(String.valueOf(courseID));
				// add the assessable flag to every node
				// Note: here we just set every course node to be assessable, but this is later corrected
				// when the results for a non-assessable course are fetched and 404 is returned by the API
				List<Pair<courseNodeVO, Boolean>> courseNodesWithAssessableFlag = new ArrayList<>();
				for(courseNodeVO courseNode : courseNodes) {
					logger.info("Node: " + courseNode.id);
					courseNodesWithAssessableFlag.add(new MutablePair<>(courseNode, true));
				}
				courseElementsMap.put(courseID, courseNodesWithAssessableFlag);
			} catch (OpalAPIException e) {
				logger.severe("Loading course elements for course " + courseID + " failed.");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Add an assessment.
	 *
	 * @param zipInputStream zip with xml files of the assessment.
	 * @param jsonInputStream json with email to xml mapping.
	 * @return JSON object with parsing statistics.
	 */
	@POST
	@Path("/assessments")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	// @RolesAllowed("authenticated")
	public Response addAssessment(@FormDataParam("assessment") InputStream zipInputStream,
			@FormDataParam("mapping") InputStream jsonInputStream) {
		// extract given zip file to tmp folder
		ZipHelper.extractFiles(zipInputStream, "tmp");
		File dir = new File("tmp");
		
		JSONArray studentMappings = null;
		try {
			JSONTokener tokener = new JSONTokener(new InputStreamReader(jsonInputStream, "UTF-8"));
			studentMappings = new JSONArray(tokener);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST)
					.entity("Student mapping uses an unsupported Encoding: " + e.getMessage()).build();
		} catch (JSONException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity("Error while parsing student mapping: " + e.getMessage())
					.build();
		}
		
		// get zip containing test metadata (named qtitest.zip)
		File metadataZip = new File(dir, "qtitest.zip");
		File metadataDir;
		try {
			ZipHelper.extractFiles(new FileInputStream(metadataZip), "tmp/qtitest");
			metadataDir = new File(dir, "qtitest");
			if(!metadataDir.exists()) throw new FileNotFoundException("tmp/qtitest dir does not exist.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST)
					.entity("Given zip does not contain qtitest.zip or it could not be extracted: " + e.getMessage()).build();
		}
		
		// parse assessment metadata from file
		File ims = new File(metadataDir, "imsmanifest.xml");
		AssessmentMetadata am = null;
		try {
			String xmlMetadata = new String(Files.readAllBytes(ims.toPath()));
			am = AssessmentMetadataParser.parseMetadata(xmlMetadata);
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		
		// we need to update the student mappings
		// the given json mapping only contains onyxResultFilename and email
		// but we also need the first and last name of the student
		for (Object mapping : studentMappings) {
			if (mapping instanceof JSONObject) {
				JSONObject mappingJSON = (JSONObject) mapping;
				String onyxResultFilename = mappingJSON.getString("onyxResultFilename");
				
				String lastName = onyxResultFilename.split("_")[0];
				String firstName = onyxResultFilename.split("_")[1].split("_")[0];
				
				mappingJSON.put("firstName", firstName);
				mappingJSON.put("lastName", lastName);
			}
		}
		
		// generate xAPI statements
		List<Pair<String, List<String>>> xApiStatements = OpalAPI.processResults(studentMappings, am, logger);
		// send statements to MobSOS
		monitorResultStatements(xApiStatements);
		
		// count assessment statements and item result statements
		int assessments = 0;
		int items = 0;
		for(Pair<String, List<String>> p : xApiStatements) {
			assessments++;
			items += p.getRight().size();
		}
		
		// create JSONObject with information that will be returned
		JSONObject result = new JSONObject();
	    result.put("assessments", assessments);
	    result.put("items", items);

		try {
			// Try to delete content of tmp directory
			FileUtils.cleanDirectory(dir.getAbsoluteFile());
		} catch (IOException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_ERROR_1, e.getMessage());
		}
		return Response.ok().entity(result.toString()).build();
	}
	
	@POST
	@Path("/")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(
			value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Thread started."),
					  @ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Authorization required."),
					  @ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Thread already running."),
					  @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Opal username or password is not configured or is empty.")})
	public Response initOnyxProxy() {
		/*if (Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
			return Response.status(HttpURLConnection.HTTP_UNAUTHORIZED).entity("Authorization required.").build();
		}*/
		
		// check if credentials for API are given
		if(this.opalUsername == null || this.opalPassword == null || this.opalUsername.isEmpty() || this.opalPassword.isEmpty()) {
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR)
					.entity("Opal username or password is not configured or is empty.").build();
		}
		
		if (dataStreamThread == null) {
			context = Context.get();
			dataStreamThread = Executors.newSingleThreadScheduledExecutor();
			dataStreamThread.scheduleAtFixedRate(new DataStreamThread(), 0, OPAL_DATA_STREAM_PERIOD, TimeUnit.SECONDS);
			return Response.status(Status.OK).entity("Thread started.").build();
		} else {
			return Response.status(Status.BAD_REQUEST).entity("Thread already running.").build();
		}
	}
	
	private class DataStreamThread implements Runnable {
		@Override
		public void run() {
			logger.info("running data stream thread");
			
			// Get current time
			TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
			long now = System.currentTimeMillis();
			
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			String lastCheckedStr = formatter.format(lastChecked);
			
			OpalAPI api = new OpalAPI(opalUsername, opalPassword, logger);
			
			for (long courseID : courses) {
				for(Pair<courseNodeVO, Boolean> courseNode : courseElementsMap.get(courseID)) {
					// Note: initially every course node is flagged as assessable
					// but this will be corrected after we get a 404 for the results of these nodes
					boolean assessable = courseNode.getRight();
					if(assessable) {
						String nodeID = courseNode.getLeft().id;
						logger.info("Getting updates for node " + nodeID + " in course " + courseID + " since " + lastCheckedStr);
						try {
							List<Pair<String, List<String>>> xApiStatements = api.getResultsAfter(String.valueOf(courseID), nodeID, lastChecked, courseElementsMap.get(courseID));
						    monitorResultStatements(xApiStatements);
						} catch (NodeNotAssessableException e) {
							// this course node is not assessable
							// we dont need to fetch the results for this node again
							logger.info("Marking node " + nodeID + " as non-assessable.");
							courseNode.setValue(false);
						} catch (OpalAPIException e) {
							e.printStackTrace();
							logger.severe("Error: " + e.getMessage());
						}
					}
				}
			}
			
			lastChecked = now;
		}
	}
	
	/**
	 * Sends the given assessment result statements and item result statements to MobSOS.
	 * @param xApiStatements List of Pair objects, where the first entry is an assessment result statement
	 *                       and the second entry is a list of item result statements that belong to this assessment
	 *                       result.
	 */
	private void monitorResultStatements(List<Pair<String, List<String>>> xApiStatements) {
		for(Pair<String, List<String>> result : xApiStatements) {
			// monitor the assessment result statement
	    	String assessmentResultStatement = result.getLeft();
	    	logger.info("Assessment Result: ");
	    	logger.info(assessmentResultStatement);
	    	//context.monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_1, assessmentResultStatement);
	    	
	    	// monitor the item result statements
	    	List<String> itemResultStatements = result.getRight();
	    	for(String itemResultStatement : itemResultStatements) {
	    		logger.info("Item Result: ");
		    	logger.info(itemResultStatement);
	    		//context.monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2, itemResultStatement);
	    	}
	    }
	}

}
