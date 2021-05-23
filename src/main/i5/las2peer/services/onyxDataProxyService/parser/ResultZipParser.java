package i5.las2peer.services.onyxDataProxyService.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.AssessmentResult;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.ItemResult;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentMetadata;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentUser;
import i5.las2peer.services.onyxDataProxyService.utils.ZipHelper;
import i5.las2peer.services.onyxDataProxyService.xApi.StatementBuilder;

public class ResultZipParser {
	
	/**
	 * Uses the given mapping and element information together with the downloaded zip (from secret link
	 * or given via HTTP request) to generate the xAPI statements that are returned.
	 * The zip should already be extracted to the tmp folder before calling this method.
	 * @param studentMappings
	 * @param am
	 * @return
	 */
	public static List<Pair<String, List<String>>> processResults(JSONArray studentMappings, AssessmentMetadata am,
			L2pLogger logger) {
		List<Pair<String, List<String>>> xApiStatements = new ArrayList<>();

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

					String assessmentResultStatement = StatementBuilder.createAssessmentResultStatement(ar, user, am).toString();
					// append users email to statement
					assessmentResultStatement += "*" + user.getEmail();
					List<String> itemResultStatements = new ArrayList<>();
					for (ItemResult ir : ar.getFilteredItemResults()) {
						String xApiStatement = StatementBuilder.createItemResultStatement(ir, user, am).toString();
						itemResultStatements.add(xApiStatement.toString() + "*" + user.getEmail());
					}
					xApiStatements.add(Pair.of(assessmentResultStatement, itemResultStatements));
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return xApiStatements;
	}

}
