package i5.las2peer.services.onyxDataProxyService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;

import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.services.onyxDataProxyService.parser.AssessmentMetadataParser;
import i5.las2peer.services.onyxDataProxyService.parser.AssessmentResultParser;
import i5.las2peer.services.onyxDataProxyService.parser.AssessmentTestParser;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.AssessmentResult;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.ItemResult;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.AssessmentTest;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentMetadata;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentUser;
import i5.las2peer.services.onyxDataProxyService.utils.ZipHelper;
import i5.las2peer.services.onyxDataProxyService.xApi.StatementBuilder;
import io.swagger.annotations.Api;
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
 * This service creates xAPI statement out of Onyx data.
 * 
 */
@ManualDeployment
@ServicePath("onyx")
public class OnyxDataProxyService extends RESTService {

	/**
	 * 
	 * Constructor of the Service.
	 * 
	 */
	public OnyxDataProxyService() {
		setFieldValues(); // This sets the values of the configuration file
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
	 * Add an assessment.
	 *
	 * @param fileInputStream zip with xml files of the assessment.
	 * @param fileFormDataContentDisposition zip with xml files of the assessment.
	 * @return JSON encoded assessment object.
	 */
	@POST
	@Path("/assessments")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	// @RolesAllowed("authenticated")
	public Response addAssessment(@FormDataParam("uploadFile") InputStream fileInputStream,
			@FormDataParam("uploadFile") FormDataContentDisposition fileFormDataContentDisposition) {
		ZipHelper.extractFiles(fileInputStream, "tmp");
		File dir = new File("tmp");
		File[] directoryListing = dir.listFiles();
		JSONObject result = new JSONObject();
		result.put("assessments", 0);
		result.put("items", 0);
		AssessmentMetadata am = null;
		AssessmentTest assessmentTest = null;
		for (File child : directoryListing) {
			if (child.getName().endsWith(".zip")) {
				try {
					String assessmentTestFolder = "tmp/"
							+ child.getName().subSequence(0, child.getName().lastIndexOf("."));
					ZipHelper.extractFiles(new FileInputStream(child), assessmentTestFolder);
					File ims = new File(assessmentTestFolder + "/imsmanifest.xml");
					String xml = new String(Files.readAllBytes(ims.toPath()));
					am = AssessmentMetadataParser.parseMetadata(xml);
					File assessmentRawFile = new File(assessmentTestFolder + "/" + am.getFiles().get(0));
					String assessmentRaw = new String(Files.readAllBytes(assessmentRawFile.toPath()));
					assessmentTest = AssessmentTestParser.parseAssessmentTest(assessmentRaw);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		for (File child : directoryListing) {
			if (child.getName().endsWith(".xml")) {
				String xml;
				try {
					xml = new String(Files.readAllBytes(child.toPath()));
					AssessmentResult ar = AssessmentResultParser.parseAssessmentResult(xml);
					try {
						String nameWithoutPath = child.getAbsolutePath();
						nameWithoutPath = nameWithoutPath.substring(0, nameWithoutPath.lastIndexOf('.'));
						java.nio.file.Path htmlFile = (new File(nameWithoutPath + ".html")).toPath();
						List<String> lines = Files.readAllLines(htmlFile);
						String first = StringUtils.substringBetween(lines.get(205), "</td><td class=\"first\">",
								"</td>");
						String last = StringUtils.substringBetween(lines.get(205), "</td><td class=\"last\">", "</td>");
						AssessmentUser user = new AssessmentUser();
						user.setFirstName(first);
						user.setLastName(last);

						JSONObject xApiStatement = StatementBuilder.createAssessmentResultStatement(assessmentTest, ar,
								user, am);
						Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_1, xApiStatement.toString());
						result.put("assessments", result.getInt("assessments") + 1);
						for (ItemResult ir : ar.getItemResults()) {
							xApiStatement = StatementBuilder.createItemResultStatement(assessmentTest, ir, user, am);
							Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2,
									xApiStatement.toString());
							result.put("items", result.getInt("items") + 1);
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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

}
