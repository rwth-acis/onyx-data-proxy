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

	private final static int OPAL_DATA_STREAM_PERIOD = 60*60; // Every hour
	
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
	 * Maps the course ids to the list of elements that are available in Opal
	 * for this course.
	 */
	private static HashMap<Long, List<courseNodeVO>> courseElementsMap = new HashMap<>();
	
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
				courseElementsMap.put(courseID, api.getCourseElements(String.valueOf(courseID)));
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
		File[] directoryListing = dir.listFiles();
		
		// create JSONObject with information that will be returned at the end of the request
		JSONObject result = new JSONObject();
		result.put("assessments", 0);
		result.put("items", 0);
		
		AssessmentMetadata am = null;
		JSONArray studentMappings = null;
		try {
			JSONTokener tokener = new JSONTokener(new InputStreamReader(jsonInputStream, "UTF-8"));
			studentMappings = new JSONArray(tokener);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return Response.status(Status.BAD_REQUEST)
					.entity("Student mapping uses an unsupported Encoding: " + e1.getMessage()).build();
		} catch (JSONException e1) {
			e1.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity("Error while parsing student mapping: " + e1.getMessage())
					.build();
		}
		for (File child : directoryListing) {
			if (child.getName().endsWith(".zip")) {
				try {
					String assessmentTestFolder = "tmp/"
							+ child.getName().subSequence(0, child.getName().lastIndexOf("."));
					ZipHelper.extractFiles(new FileInputStream(child), assessmentTestFolder);
					File ims = new File(assessmentTestFolder + "/imsmanifest.xml");
					String xml = new String(Files.readAllBytes(ims.toPath()));
					am = AssessmentMetadataParser.parseMetadata(xml);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					return Response.status(Status.BAD_REQUEST).entity("Assessment meta data not found.").build();
				} catch (IOException e) {
					e.printStackTrace();
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
				} catch (Exception e) {
					e.printStackTrace();
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
				}
			}
		}
		for (Object mapping : studentMappings) {
			if (mapping instanceof JSONObject) {
				JSONObject mappingJSON = (JSONObject) mapping;
				String onyxResultFilename = mappingJSON.getString("onyxResultFilename");
				String email = mappingJSON.getString("email");
				File onyxFile = new File("tmp/" + onyxResultFilename);

				String xml;
				try {
					xml = new String(Files.readAllBytes(onyxFile.toPath()));
					AssessmentResult ar = AssessmentResultParser.parseAssessmentResult(xml);
					String nameWithoutPath = onyxFile.getAbsolutePath();
					nameWithoutPath = nameWithoutPath.substring(0, nameWithoutPath.lastIndexOf('.'));
					java.nio.file.Path htmlFile = (new File(nameWithoutPath + ".html")).toPath();
					List<String> lines = Files.readAllLines(htmlFile);
					String first = StringUtils.substringBetween(lines.get(205), "</td><td class=\"first\">", "</td>");
					String last = StringUtils.substringBetween(lines.get(205), "</td><td class=\"last\">", "</td>");
					AssessmentUser user = new AssessmentUser();
					user.setFirstName(first);
					user.setLastName(last);
					user.setEmail(email);

					JSONObject xApiStatement = StatementBuilder.createAssessmentResultStatement(ar,
							user, am);
					Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_1, xApiStatement.toString());
					result.put("assessments", result.getInt("assessments") + 1);
					for (ItemResult ir : ar.getItemResults()) {
						xApiStatement = StatementBuilder.createItemResultStatement(ir, user, am);
						Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2, xApiStatement.toString());
						result.put("items", result.getInt("items") + 1);
					}

				} catch (IOException e) {
					e.printStackTrace();
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
				} catch (Exception e) {
					e.printStackTrace();
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
				}
			}
		}

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
			// test with start date 0 to get all results
			lastChecked = 0;
			String lastCheckedStr = formatter.format(lastChecked);
			
			OpalAPI api = new OpalAPI(opalUsername, opalPassword, logger);
			
			for (long courseID : courses) {
				for(courseNodeVO courseNode : courseElementsMap.get(courseID)) {
					String nodeID = courseNode.id;
					
					logger.info("Getting updates for node " + nodeID + " in course " + courseID + " since " + lastCheckedStr);
					
					try {
						List<String> xApiStatements = api.getResultsAfter(String.valueOf(courseID), nodeID, lastChecked, courseElementsMap.get(courseID));
					    // TODO: monitor (therefore we need to differ between assessment results and item result statements
					} catch (OpalAPIException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						logger.severe("Error: " + e.getMessage());
					}
					
				}
			}
			
			lastChecked = now;
		}
	}

}
