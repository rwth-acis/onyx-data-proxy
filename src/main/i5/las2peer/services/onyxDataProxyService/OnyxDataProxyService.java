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
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.services.onyxDataProxyService.api.NodeNotAssessableException;
import i5.las2peer.services.onyxDataProxyService.api.OpalAPI;
import i5.las2peer.services.onyxDataProxyService.api.OpalAPIException;
import i5.las2peer.services.onyxDataProxyService.api.OpalAPI.courseNodeVO;
import i5.las2peer.services.onyxDataProxyService.parser.AssessmentMetadataParser;
import i5.las2peer.services.onyxDataProxyService.parser.ResultZipParser;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentMetadata;
import i5.las2peer.services.onyxDataProxyService.utils.StoreManagementHelper;
import i5.las2peer.services.onyxDataProxyService.utils.StoreManagementParseException;
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
				version = "1.1.0",
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

	private int OPAL_DATA_STREAM_PERIOD = 30; // Every 30 minutes
	private int OPAL_STATISTICS_STREAM_PERIOD = 1; // every day
	
	/**
	 * How often the course elements map should be updated (in minutes).
	 */
	private int OPAL_COURSE_ELEMENTS_UPDATE_PERIOD = 60;
	
	private static ScheduledExecutorService dataStreamThread = null;
	private static ScheduledExecutorService statisticsStreamThread = null;
	private static ScheduledExecutorService courseElementsUpdateThread = null;
	private static Context context = null;
	
	private static long lastChecked = 0;
	private static long lastCheckedStatistics = 0;
	
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
	 * Whether the service is using the Opal API or not.
	 * Should be configured using the service properties file.
	 */
	private boolean apiEnabled;
	
	/**
	 * Whether personal user information (email, name) should be 
	 * hashed before sending it to MobSOS.
	 */
	private boolean pseudonymizationEnabled;
	
	/**
	 * Whether template variables (from Onyx) with a specific prefix should 
	 * be added to the xAPI statements automatically. The prefix can be
	 * configured by using the templateVariablesInStatementsPrefix value.
	 */
	private boolean templateVariablesInStatements;
	
	/**
	 * If templateVariablesInStatements is set to true, then the prefix
	 * given here is used to filter the template variables (from Onyx) 
	 * that should be added to the xAPI statements automatically.
	 */
	private String templateVariablesInStatementsPrefix;
	
	private static OpalAPI api;
	
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
		StatementBuilder.templateVariablesInStatements = this.templateVariablesInStatements;
		StatementBuilder.templateVariablesInStatementsPrefix = this.templateVariablesInStatementsPrefix;
		
		if(this.apiEnabled) {
			this.api = new OpalAPI(opalUsername, opalPassword, logger);
		    this.updateCourseList();
		    this.updateCourseElementsMap();
		
		    if(OnyxDataProxyService.lastChecked == 0) {
			    // Get current time
			    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
			    lastChecked = System.currentTimeMillis();
		    }
		    if(OnyxDataProxyService.lastCheckedStatistics == 0) {
			    // Get current time
			    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
			    lastCheckedStatistics = System.currentTimeMillis();
		    }
		}
		
		// check if store assignment is enabled (i.e. the file exists)
		if(StoreManagementHelper.isStoreAssignmentEnabled()) {
			logger.info("Found store assignment file, enabling assignment...");
			try {
				StoreManagementHelper.loadAssignments();
				logger.info("Store assignment is enabled.");
			} catch (IOException e) {
				logger.severe("An error occurred while loading the assignment from file.");
				e.printStackTrace();
			}
		} else {
			logger.info("Store assignment is not enabled.");
		}
	}

	/**
	 * Describe MobSOS log messages
	 */
	@Override
	public Map<String, String> getCustomMessageDescriptions() {
		Map<String, String> descriptions = new HashMap<>();
		descriptions.put("SERVICE_CUSTOM_MESSAGE_1", "Sent assessment result to lrs.");
		descriptions.put("SERVICE_CUSTOM_MESSAGE_2", "Sent item result of an assessment to lrs.");
		descriptions.put("SERVICE_CUSTOM_MESSAGE_3", "Sent course node access statistic to lrs.");
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
	private static void updateCourseElementsMap() {
		HashMap<Long, List<Pair<courseNodeVO, Boolean>>> courseElementsMapOld = (HashMap<Long, List<Pair<courseNodeVO, Boolean>>>) courseElementsMap.clone();
		
		courseElementsMap.clear();
		for(long courseID : courses) {
			try {
				logger.warning("Loading course elements for course " + courseID);
				// fetch course nodes from Opal API
				List<courseNodeVO> courseNodes = api.getCourseElements(String.valueOf(courseID));
				// add the assessable flag to every node
				// Note: here we just set every course node to be assessable, but this is later corrected
				// when the results for a non-assessable course are fetched and 404 is returned by the API
				List<Pair<courseNodeVO, Boolean>> courseNodesWithAssessableFlag = new ArrayList<>();
				for(courseNodeVO courseNode : courseNodes) {
					logger.info("Node: " + courseNode.id);
					boolean assessable = getAssessableFlagValue(courseElementsMapOld, courseID, courseNode);
					courseNodesWithAssessableFlag.add(new MutablePair<>(courseNode, assessable));
				}
				courseElementsMap.put(courseID, courseNodesWithAssessableFlag);
			} catch (OpalAPIException e) {
				logger.severe("Loading course elements for course " + courseID + " failed.");
				e.printStackTrace();
				
				courseElementsMap = courseElementsMapOld;
				break;
			} catch (Exception e) {
				logger.severe("Unknown error while updating course elements map: " + e.getMessage());
				e.printStackTrace();
				
				courseElementsMap = courseElementsMapOld;
				break;
			}
		}
	}
	
	/**
	 * Checks if the old courseElementsMap already contained the course and the courseNode.
	 * If the old map already contained the courseNode, then it returns the previously stored
	 * value of the assessable flag.
	 * Otherwise it returns true, because we do not know yet whether the node is assessable, but 
	 * after we fetch the results for it for the first time (in data stream thread) we will know it
	 * and the flag gets updated.
	 * @param courseElementsMapOld
	 * @param courseID
	 * @param courseNode
	 * @return
	 */
	private static boolean getAssessableFlagValue(HashMap<Long, List<Pair<courseNodeVO, Boolean>>> courseElementsMapOld, long courseID,
			courseNodeVO courseNode) {
		// check if course existed in courseElementsMap
		if(courseElementsMapOld.containsKey(courseID)) {
			// check if courseNode already existed previously
			List<Pair<courseNodeVO, Boolean>> courseNodesWithAssessableFlag = courseElementsMapOld.get(courseID);
			for(Pair<courseNodeVO, Boolean> pair : courseNodesWithAssessableFlag) {
				if(pair.getLeft().id.equals(courseNode.id)) {
					return pair.getRight();
				}
			}
			// courseNode is a new one (set flag to true, until we know if node is assessable)
			return true;
		} else {
			// the course is not part of courseElementsMapOld
			// every course node should be flagged as assessable first (then later flag might be set to false,
			// if we know that it is non-assessable)
			return true;
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
		List<Pair<String, List<String>>> xApiStatements = ResultZipParser.processResults(studentMappings, am, logger, pseudonymizationEnabled, null);
		// need to set context for monitoring
		context = Context.get();
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
					  @ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Access denied."),
					  @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Opal username or password is not configured or is empty.")})
	public Response initOnyxProxy() {
		if (Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
			return Response.status(HttpURLConnection.HTTP_UNAUTHORIZED).entity("Authorization required.").build();
		}
		
		// check if credentials for API are given
		if(this.opalUsername == null || this.opalPassword == null || this.opalUsername.isEmpty() || this.opalPassword.isEmpty()) {
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR)
					.entity("Opal username or password is not configured or is empty.").build();
		}
		
		// check if agent sending the request uses the same email address that is used for the Opal API
		UserAgentImpl u = (UserAgentImpl) Context.getCurrent().getMainAgent();
		String uEmail = u.getEmail();
		if(!uEmail.equals(this.opalUsername)) {
			return Response.status(Status.FORBIDDEN).entity("Access denied.").build();
		}
		
		if (dataStreamThread == null) {
			context = Context.get();
			dataStreamThread = Executors.newSingleThreadScheduledExecutor();
			// start dataStreamThread a bit later than statisticsStreamThread (otherwise we get login problems)
			dataStreamThread.scheduleAtFixedRate(new DataStreamThread(), 1, OPAL_DATA_STREAM_PERIOD, TimeUnit.MINUTES);
			statisticsStreamThread = Executors.newSingleThreadScheduledExecutor();
			statisticsStreamThread.scheduleAtFixedRate(new StatisticsStreamThread(), 0, OPAL_STATISTICS_STREAM_PERIOD, TimeUnit.DAYS);
			
		    // start thread to update the course elements map regularly
			courseElementsUpdateThread = Executors.newSingleThreadScheduledExecutor();
			courseElementsUpdateThread.scheduleAtFixedRate(new CourseElementsUpdateThread(), 
					OPAL_COURSE_ELEMENTS_UPDATE_PERIOD, OPAL_COURSE_ELEMENTS_UPDATE_PERIOD, TimeUnit.MINUTES);
			
			return Response.status(Status.OK).entity("Thread started.").build();
		} else {
			return Response.status(Status.BAD_REQUEST).entity("Thread already running.").build();
		}
	}
	
	/**
	 * Method for setting the assignments of Onyx courses to stores.
	 * Takes a properties file containing the course IDs and a comma-separated list of store client IDs as key-value
	 * pairs.
	 *
	 * @param storesInputStream Input stream of the passed properties file.
	 *
	 * @return Status message
	 */
	@POST
	@Path("/setStoreAssignment")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@ApiResponses(
			value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Updated store assignment."),
					@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Authorization required."),
					@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Access denied.") })
	public Response setStoreAssignment(@FormDataParam("storeAssignment") InputStream storesInputStream) {
	    if (Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
		    return Response.status(Status.UNAUTHORIZED).entity("Authorization required.").build();
	    }
	    
	    // check if agent sending the request uses the same email address that is used for the Opal API
	 	UserAgentImpl u = (UserAgentImpl) Context.getCurrent().getMainAgent();
	 	String uEmail = u.getEmail();
	 	if(!uEmail.equals(this.opalUsername)) {
	 		return Response.status(Status.FORBIDDEN).entity("Access denied.").build();
	 	}

		try {
			StoreManagementHelper.updateAssignments(storesInputStream);
			logger.info("Added store assignment.");
			return Response.status(200).entity("Added store assignment with " +
					StoreManagementHelper.numberOfAssignments() + " assignments.").build();
		} catch (StoreManagementParseException e) {
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e.getMessage()).build();
		}
	}

	/**
	 * Method to disable the store assignment.
	 *
	 * @return Status message
	 */
	@POST
	@Path("/disableStoreAssignment")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(
			value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Disabled store assignment."),
					@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Authorization required."),
					@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Access denied."),
					@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Unable to disable store assignment.")})
	public Response disableStoreAssignment() {
	    if (Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
		    return Response.status(Status.UNAUTHORIZED).entity("Authorization required.").build();
	    }
	    
	    // check if agent sending the request uses the same email address that is used for the Opal API
	 	UserAgentImpl u = (UserAgentImpl) Context.getCurrent().getMainAgent();
	 	String uEmail = u.getEmail();
	 	if(!uEmail.equals(this.opalUsername)) {
	 		return Response.status(Status.FORBIDDEN).entity("Access denied.").build();
	 	}

		boolean success = StoreManagementHelper.removeAssignmentFile();
		if(success) {
			StoreManagementHelper.resetAssignment();
			return Response.status(Status.OK).entity("Disabled store assignment.").build();
		} else {
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity("Unable to disable store assignment.").build();
		}
	}
	
	private class DataStreamThread implements Runnable {
		@Override
		public void run() {
			logger.info("running data stream thread");
			
			try {
				// Get current time
				TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
				long now = System.currentTimeMillis();

				DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				String lastCheckedStr = formatter.format(lastChecked);
				String nowStr = formatter.format(now);

				for (long courseID : courses) {
					for(Pair<courseNodeVO, Boolean> courseNode : courseElementsMap.get(courseID)) {
						// Note: initially every course node is flagged as assessable
						// but this will be corrected after we get a 404 for the results of these nodes
						boolean assessable = courseNode.getRight();
						if(assessable) {
							String nodeID = courseNode.getLeft().id;
							logger.warning("Getting updates for node " + nodeID + " in course " + courseID + 
									" between " + lastCheckedStr + " and " + nowStr);
							try {
								List<Pair<String, List<String>>> xApiStatements = api.getResultsAfter(String.valueOf(courseID), 
										nodeID, lastChecked, now, courseElementsMap.get(courseID), pseudonymizationEnabled);
								monitorResultStatements(xApiStatements);
							} catch (NodeNotAssessableException e) {
								// this course node is not assessable
								// we dont need to fetch the results for this node again
								logger.warning("Marking node " + nodeID + " as non-assessable.");
								courseNode.setValue(false);
							} catch (OpalAPIException e) {
								e.printStackTrace();
								logger.severe("Error: " + e.getMessage());
							} catch (Exception e) {
								e.printStackTrace();
								logger.severe("Unknown error in data stream thread:");
								logger.severe(e.getMessage());
							}
						}
					}
				}

				lastChecked = now;

			} catch (Exception e) {
				logger.severe("Unknwon error 2 in data stream thread: ");
				logger.severe(e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private class StatisticsStreamThread implements Runnable {
		@Override
		public void run() {
			logger.info("running statistics stream thread");
			
			// Get current time
			TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
			long now = System.currentTimeMillis();
			
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			String lastCheckedStr = formatter.format(lastCheckedStatistics);
			
			
			for (long courseID : courses) {
				logger.info("Getting statistic updates for course " + courseID + " since " + lastCheckedStr);
				try {
					List<String> xApiStatements = api.getCourseAccessStatisticsAfter(String.valueOf(courseID), 
							courseElementsMap.get(courseID), lastCheckedStatistics);
					monitorCourseAccessStatistics(xApiStatements, String.valueOf(courseID));
				} catch (OpalAPIException e) {
					e.printStackTrace();
					logger.severe("Error: " + e.getMessage());
				}
			}
			
			lastCheckedStatistics = now;
		}
	}
	
	private class CourseElementsUpdateThread implements Runnable {
		@Override
		public void run() {
			logger.warning("running course elements update thread");
			updateCourseElementsMap();
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
	    	context.monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_1, assessmentResultStatement);
	    	
	    	// monitor the item result statements
	    	List<String> itemResultStatements = result.getRight();
	    	for(String itemResultStatement : itemResultStatements) {
	    		logger.info("Item Result: ");
		    	logger.info(itemResultStatement);
	    		context.monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2, itemResultStatement);
	    	}
	    }
	}
	
	/**
	 * Sends the given course node access statistics to MobSOS.
	 * @param xApiStatements
	 */
	private void monitorCourseAccessStatistics(List<String> xApiStatements, String courseId) {
		for(String statement : xApiStatements) {
			JSONObject obj = new JSONObject(statement);
			statement = StatementBuilder.attachTokens(obj, courseId, this.opalUsername);
			logger.info("Course node statistic: ");
			logger.info(statement);
			context.monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_3, statement);
		}
	}

}
