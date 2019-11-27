package i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest;

public class OutcomeProcessing {
	private SetOutcomeValue setOutcomeValue;

	private OutcomeCondition outcomeCondition;

	public SetOutcomeValue getSetOutcomeValue() {
		return setOutcomeValue;
	}

	public void setSetOutcomeValue(SetOutcomeValue setOutcomeValue) {
		this.setOutcomeValue = setOutcomeValue;
	}

	public OutcomeCondition getOutcomeCondition() {
		return outcomeCondition;
	}

	public void setOutcomeCondition(OutcomeCondition outcomeCondition) {
		this.outcomeCondition = outcomeCondition;
	}

	@Override
	public String toString() {
		return "ClassPojo [setOutcomeValue = " + setOutcomeValue + ", outcomeCondition = " + outcomeCondition + "]";
	}
}