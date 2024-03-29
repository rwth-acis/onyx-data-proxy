package i5.las2peer.services.onyxDataProxyService.xApi;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.AssessmentResult;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.ItemResult;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.OutcomeVariable;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.ResponseVariable;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.TemplateVariable;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentMetadata;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentUser;
import i5.las2peer.services.onyxDataProxyService.utils.StoreManagementHelper;

public class StatementBuilder {
	
	/**
	 * Whether template variables (from Onyx) with a specific prefix should 
	 * be added to the xAPI statements automatically. The prefix can be
	 * configured by using the templateVariablesInStatementsPrefix value.
	 */
	public static boolean templateVariablesInStatements = false;
	
	/**
	 * If templateVariablesInStatements is set to true, then the prefix
	 * given here is used to filter the template variables (from Onyx) 
	 * that should be added to the xAPI statements automatically.
	 */
	public static String templateVariablesInStatementsPrefix;

	public static JSONObject createActor(AssessmentUser user, String homePage) {
		JSONObject actor = new JSONObject();
		actor.put("name", user.getFirstName() + " " + user.getLastName());
		JSONObject account = new JSONObject();
		account.put("name", user.getEmail());
		account.put("homePage", homePage);
		actor.put("account", account);
		return actor;
	}

	public static JSONObject createVerb() {
		// verb
		JSONObject verb = new JSONObject();
		verb.put("id", "http://activitystrea.ms/schema/1.0/complete");
		JSONObject display = new JSONObject();
		display.put("en-US", "completed");
		verb.put("display", display);
		return verb;
	}

	/**
	 * Creates a xAPI statement for the given assessment result which gets returned as a JSONObject.
	 * @param assessmentResult
	 * @param user
	 * @param metadata
	 * @param templateVariables All the template variables (from assessment result and from the item results)
	 * @return xAPI statement corresponding to the given assessment result as JSONObject.
	 */
	public static JSONObject createAssessmentResultStatement(AssessmentResult assessmentResult, AssessmentUser user,
			AssessmentMetadata metadata, ArrayList<TemplateVariable> templateVariables) {
		JSONObject xApiStatement = new JSONObject();
		JSONObject actor = StatementBuilder.createActor(user, "https://bildungsportal.sachsen.de/opal/");
		JSONObject verb = StatementBuilder.createVerb();
		JSONObject object = new JSONObject();
		JSONObject result = new JSONObject();
		JSONObject context = new JSONObject();

		// Object
		object.put("id", "http://adlnet.gov/expapi/activities/onyx/" + metadata.getId());
		JSONObject definition = new JSONObject();
		JSONObject dname = new JSONObject();
		dname.put("en-US", metadata.getTitle());
		JSONObject ddescription = new JSONObject();
		if (metadata.getDescription().length() > 0) {
			ddescription.put("en-US", metadata.getDescription());
		} else {
			ddescription.put("en-US", "No description.");
		}
		definition.put("name", dname);
		definition.put("description", ddescription);
		definition.put("type", "http://adlnet.gov/expapi/activities/cmi.interaction");
		definition.put("interactionType", "other");
		object.put("definition", definition);
		object.put("objectType", "Activity");

		// Result
		JSONObject score = new JSONObject();
		score.put("min", 0);

		for (ResponseVariable rv : assessmentResult.getTestResult().getResponseVariables()) {
			if (rv.getIdentifier().equalsIgnoreCase("duration")) {
				// TODO transform to ISO 8601 Duration
				result.put("duration", "PT" + rv.getCandidateResponse().getValue().getValue() + "S");
			}
		}
		for (OutcomeVariable ov : assessmentResult.getTestResult().getOutcomeVariables()) {
			if (ov.getIdentifier().equalsIgnoreCase("SCORE")) {
				score.put("raw", Double.parseDouble(ov.getValue().getValue()));
			} else if (ov.getIdentifier().equalsIgnoreCase("MAXSCORE")) {
				score.put("max", Double.parseDouble(ov.getValue().getValue()));
			} else if (ov.getIdentifier().equalsIgnoreCase("PASS")) {
				result.put("success", Boolean.parseBoolean(ov.getValue().getValue()));
			}
		}
		
		if(score.has("raw") && score.has("max") && score.getDouble("max") != 0) {
			score.put("scaled", score.getDouble("raw") / score.getDouble("max"));
		} else {
			score.put("scaled", 0);
		}

		result.put("score", score);
		
		JSONObject contextExtensions = StatementBuilder.getTemplateVariableContextExtensions(templateVariables);
		context.put("extensions", contextExtensions);

		xApiStatement.put("actor", actor);
		xApiStatement.put("verb", verb);
		xApiStatement.put("object", object);
		xApiStatement.put("result", result);
		xApiStatement.put("context", context);
		xApiStatement.put("timestamp", assessmentResult.getTestResult().getDatestamp() + "+02:00");
		return xApiStatement;
	}

	/**
	 * Creates a xAPI statement for the given item result which gets returned as a JSONObject.
	 * @param ir
	 * @param user
	 * @param metadata
	 * @param templateVariables All the template variables (from assessment result and from the item results)
	 * @return xAPI statement corresponding to the given item result as JSONObject.
	 */
	public static JSONObject createItemResultStatement(ItemResult ir, AssessmentUser user, AssessmentMetadata metadata,
			ArrayList<TemplateVariable> templateVariables) {
		JSONObject xApiStatement = new JSONObject();
		JSONObject actor = StatementBuilder.createActor(user, "https://bildungsportal.sachsen.de/opal/");
		JSONObject verb = StatementBuilder.createVerb();
		JSONObject object = new JSONObject();
		JSONObject result = new JSONObject();
		JSONObject context = new JSONObject();

		// Object
		object.put("id", "http://adlnet.gov/expapi/activities/onyx/" + metadata.getId() + "/" + ir.getIdentifier());
		JSONObject definition = new JSONObject();
		JSONObject dname = new JSONObject();
		dname.put("en-US", metadata.getTitle());
		JSONObject ddescription = new JSONObject();
		// TODO Adjust description
		if (metadata.getDescription().length() > 0) {
			ddescription.put("en-US", metadata.getDescription());
		} else {
			ddescription.put("en-US", "No description.");
		}
		definition.put("name", dname);
		definition.put("description", ddescription);
		definition.put("type", "http://adlnet.gov/expapi/activities/cmi.interaction");
		definition.put("interactionType", "other");
		object.put("definition", definition);
		object.put("objectType", "Activity");

		// Result
		JSONObject score = new JSONObject();
		score.put("min", 0);

		for (ResponseVariable rv : ir.getResponseVariables()) {
			if (rv.getIdentifier().equalsIgnoreCase("duration")) {
				// TODO transform to ISO 8601 Duration
				result.put("duration", "PT" + rv.getCandidateResponse().getValue().getValue() + "S");
			}
		}
		for (OutcomeVariable ov : ir.getOutcomeVariables()) {
			if (ov.getIdentifier().equalsIgnoreCase("SCORE")) {
				score.put("raw", Double.parseDouble(ov.getValue().getValue()));
			} else if (ov.getIdentifier().equalsIgnoreCase("MAXSCORE")) {
				score.put("max", Double.parseDouble(ov.getValue().getValue()));
			} else if (ov.getIdentifier().equalsIgnoreCase("PASS")) {
				result.put("success", Boolean.parseBoolean(ov.getValue().getValue()));
			}
		}
		
		if(score.has("raw") && score.has("max") && score.getDouble("max") != 0) {
			score.put("scaled", score.getDouble("raw") / score.getDouble("max"));
		} else {
			score.put("scaled", 0);
		}
		
		result.put("score", score);
		
		// result extensions
		JSONObject scoreExtensions = new JSONObject();
		scoreExtensions.put("https://tech4comp.de/xapi/context/extensions/sessionStatus", ir.getSessionStatus());
		result.put("extensions", scoreExtensions);
		
		JSONObject contextExtensions = StatementBuilder.getTemplateVariableContextExtensions(templateVariables);
		context.put("extensions", contextExtensions);

		xApiStatement.put("actor", actor);
		xApiStatement.put("verb", verb);
		xApiStatement.put("object", object);
		xApiStatement.put("result", result);
		xApiStatement.put("context", context);
		xApiStatement.put("timestamp", ir.getDateStamp() + "+02:00");
		return xApiStatement;
	}
	
	/**
	 * Creates a JSONObject that can be used as context extensions and adds the 
	 * studienID template variable and if configured also the ones starting with 
	 * the prefix set in templateVariablesInStatementsPrefix.
	 * @param templateVariables
	 */
	public static JSONObject getTemplateVariableContextExtensions(ArrayList<TemplateVariable> templateVariables) {
		JSONObject contextExtensions = new JSONObject();
		
		for (TemplateVariable tv : templateVariables) {
			String variableName = tv.getIdentifier();
			if(tv.getValue() == null) {
				continue;
			}
			String variableValue = String.valueOf(tv.getValue().getValue());
			
			if (variableName.equalsIgnoreCase("studienID")) {
				contextExtensions.put("https://tech4comp.de/xapi/context/extensions/studienID", variableValue);
			}
			
			if (templateVariablesInStatements) {
				// every template variable with the given prefix should be added to the statements
			    if (variableName.startsWith(templateVariablesInStatementsPrefix)) {
				    contextExtensions.put("https://tech4comp.de/xapi/context/extensions/" + variableName, variableValue);
			    }
			}
		}
		
		return contextExtensions;
	}
	
	public static JSONObject createCourseNodeAccessStatisticStatement(String courseId, String nodeId, String nodeName,
			int accesses, String date) {
		JSONObject xApiStatement = new JSONObject();
		JSONObject actor = createGroupForCourse(courseId, "https://bildungsportal.sachsen.de/opal/");
		JSONObject verb = createVerbViewed();
		
		JSONObject object = new JSONObject();
		object.put("id", "http://adlnet.gov/expapi/activities/onyx/" + courseId + "/elements/" + nodeId);
		
		// definition
		JSONObject definition = new JSONObject();
		JSONObject name = new JSONObject();
		name.put("en-US", nodeName);
		definition.put("name", name);
		
		// definition.interactionType -- new property based on the latest xAPI validation
		definition.put("interactionType", "other");
		
		object.put("definition", definition);
		
		JSONObject context = new JSONObject();
		
		JSONObject extensions = new JSONObject();
		JSONObject accessExtension = new JSONObject();
		accessExtension.put("accesses", accesses);
		extensions.put("https://tech4comp.de/xapi/context/extensions/nodeAccessStatistic", accessExtension);
		context.put("extensions", extensions);
		
		xApiStatement.put("actor", actor);
		xApiStatement.put("verb", verb);
		xApiStatement.put("object", object);
		xApiStatement.put("context", context);
		xApiStatement.put("timestamp", date);
		return xApiStatement;
	}
	
	private static JSONObject createGroupForCourse(String courseId, String homePage) {
		JSONObject actor = new JSONObject();
		actor.put("objectType", "Group");
		actor.put("name", "Members of course " + courseId);
		JSONObject account = new JSONObject();
		account.put("name", "Members of course " + courseId);
		account.put("homePage", homePage);
		actor.put("account", account);
		return actor;
	}
	
	public static JSONObject createVerbViewed() {
		JSONObject verb = new JSONObject();
		verb.put("id", "http://id.tincanapi.com/verb/viewed");
		JSONObject display = new JSONObject();
		display.put("en-US", "viewed");
		verb.put("display", display);
		return verb;
	}
	
	public static String attachTokens(JSONObject statement, String courseID, String userEmail) {
		JSONArray tokens = StatementBuilder.getStoreTokens(courseID, userEmail);
		
		JSONObject result = new JSONObject();
		result.put("statement", statement);
		result.put("tokens", tokens);
		return result.toString();
	}
	
	/**
	 * Helper function for determining the correct client tokens according to the store assignment.
	 *
	 * @param courseID Course to which the statement belongs
	 * @param userEmail Mail of the user / mail that should be used for the client, if no store is assigned.
	 * @return
	 */
	public static JSONArray getStoreTokens(String courseID, String userEmail) {
		ArrayList<String> tokens = new ArrayList<>();
		if (courseID != null && StoreManagementHelper.isStoreAssignmentEnabled() &&
				StoreManagementHelper.getAssignment(courseID) != null) {
			for (String storeId : StoreManagementHelper.getAssignment(courseID)) {
				tokens.add(storeId);
			}
		} else {
			tokens.add(userEmail);
		}
		return new JSONArray(tokens);
	}
}
