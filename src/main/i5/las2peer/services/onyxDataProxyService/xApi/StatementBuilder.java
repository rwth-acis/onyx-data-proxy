package i5.las2peer.services.onyxDataProxyService.xApi;

import org.json.JSONObject;

import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.AssessmentResult;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.ItemResult;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.OutcomeVariable;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.ResponseVariable;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.AssessmentTest;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentMetadata;
import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentUser;

public class StatementBuilder {

	public static JSONObject createActor(AssessmentUser user, String homePage) {
		JSONObject actor = new JSONObject();
		actor.put("name", user.getFirstName() + " " + user.getLastName());
		JSONObject account = new JSONObject();
		// TODO Fix identifier
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

	public static JSONObject createAssessmentResultStatement(AssessmentTest assessmentTest,
			AssessmentResult assessmentResult, AssessmentUser user, AssessmentMetadata metadata) {
		JSONObject xApiStatement = new JSONObject();
		JSONObject actor = StatementBuilder.createActor(user, "https://bildungsportal.sachsen.de/opal");
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

				score.put("raw", Integer.parseInt(ov.getValue().getValue()));
			} else if (ov.getIdentifier().equalsIgnoreCase("MAXSCORE")) {
				score.put("max", Integer.parseInt(ov.getValue().getValue()));
			} else if (ov.getIdentifier().equalsIgnoreCase("PASS")) {
				result.put("success", Boolean.parseBoolean(ov.getValue().getValue()));
			}
		}
		score.put("scaled", score.getDouble("raw") / score.getDouble("max"));

		result.put("score", score);

		xApiStatement.put("actor", actor);
		xApiStatement.put("verb", verb);
		xApiStatement.put("object", object);
		xApiStatement.put("result", result);
		xApiStatement.put("context", context);
		return xApiStatement;
	}

	public static JSONObject createItemResultStatement(AssessmentTest assessmentTest, ItemResult ir,
			AssessmentUser user, AssessmentMetadata metadata) {
		JSONObject xApiStatement = new JSONObject();
		JSONObject actor = StatementBuilder.createActor(user, "https://bildungsportal.sachsen.de/opal");
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
				score.put("raw", Integer.parseInt(ov.getValue().getValue()));
			} else if (ov.getIdentifier().equalsIgnoreCase("MAXSCORE")) {
				score.put("max", Integer.parseInt(ov.getValue().getValue()));
			} else if (ov.getIdentifier().equalsIgnoreCase("PASS")) {
				result.put("success", Boolean.parseBoolean(ov.getValue().getValue()));
			}
		}
		score.put("scaled", score.getDouble("raw") / score.getDouble("max"));
		result.put("score", score);

		xApiStatement.put("actor", actor);
		xApiStatement.put("verb", verb);
		xApiStatement.put("object", object);
		xApiStatement.put("result", result);
		xApiStatement.put("context", context);
		return xApiStatement;
	}
}
