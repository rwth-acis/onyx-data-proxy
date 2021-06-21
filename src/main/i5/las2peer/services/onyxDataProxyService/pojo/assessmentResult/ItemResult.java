package i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult;

import java.util.ArrayList;

public class ItemResult {
	private String identifier;
	private String dateStamp;
	private String sessionStatus;
	private String sequenceIndex;
	private ArrayList<ResponseVariable> responseVariables;
	private ArrayList<OutcomeVariable> outcomeVariables;
	private ArrayList<TemplateVariable> templateVariables;

	public ItemResult() {
		this.responseVariables = new ArrayList<ResponseVariable>();
		this.outcomeVariables = new ArrayList<OutcomeVariable>();
		this.templateVariables = new ArrayList<TemplateVariable>();
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getDateStamp() {
		return dateStamp;
	}

	public void setDateStamp(String dateStamp) {
		this.dateStamp = dateStamp;
	}

	public String getSessionStatus() {
		return sessionStatus;
	}

	public void setSessionStatus(String sessionStatus) {
		this.sessionStatus = sessionStatus;
	}

	public String getSequenceIndex() {
		return sequenceIndex;
	}

	public void setSequenceIndex(String sequenceIndex) {
		this.sequenceIndex = sequenceIndex;
	}

	public ArrayList<ResponseVariable> getResponseVariables() {
		return responseVariables;
	}

	public void setResponseVariables(ArrayList<ResponseVariable> responseVariables) {
		this.responseVariables = responseVariables;
	}

	public void addResponseVariable(ResponseVariable responseVariable) {
		this.responseVariables.add(responseVariable);
	}

	public ArrayList<OutcomeVariable> getOutcomeVariables() {
		return outcomeVariables;
	}

	public void setOutcomeVariables(ArrayList<OutcomeVariable> outcomeVariables) {
		this.outcomeVariables = outcomeVariables;
	}

	public void addOutcomeVariable(OutcomeVariable outcomeVariable) {
		this.outcomeVariables.add(outcomeVariable);
	}
	
	public ArrayList<TemplateVariable> getTemplateVariables() {
		return templateVariables;
	}
	
	public void setTemplateVariables(ArrayList<TemplateVariable> templateVariables) {
		this.templateVariables = templateVariables;
	}
	
	public void addTemplateVariable(TemplateVariable templateVariable) {
		this.templateVariables.add(templateVariable);
	}
}
