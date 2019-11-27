package i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult;

import java.util.ArrayList;

public class TestResult {
	private String identifier;
	private String datestamp;
	private String currentItemIndex;
	private ArrayList<ResponseVariable> responseVariables;
	private ArrayList<OutcomeVariable> outcomeVariables;

	public TestResult() {
		this.responseVariables = new ArrayList<ResponseVariable>();
		this.outcomeVariables = new ArrayList<OutcomeVariable>();
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getDatestamp() {
		return datestamp;
	}

	public void setDatestamp(String datestamp) {
		this.datestamp = datestamp;
	}

	public String getCurrentItemIndex() {
		return currentItemIndex;
	}

	public void setCurrentItemIndex(String currentItemIndex) {
		this.currentItemIndex = currentItemIndex;
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

	public void setOutcomeVariable(ArrayList<OutcomeVariable> outcomeVariables) {
		this.outcomeVariables = outcomeVariables;
	}

	public void addOutcomeVariable(OutcomeVariable outcomeVariable) {
		this.outcomeVariables.add(outcomeVariable);
	}

	@Override
	public String toString() {
		return "ClassPojo [identifier = " + identifier + ", datestamp = " + datestamp + ", currentItemIndex = "
				+ currentItemIndex + ", responseVariables = " + responseVariables + ", outcomeVariables = "
				+ outcomeVariables + "]";
	}
}
