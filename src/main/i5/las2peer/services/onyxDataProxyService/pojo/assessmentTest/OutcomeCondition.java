package i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest;

public class OutcomeCondition {
	private OutcomeIf outcomeIf;

	private OutcomeElse outcomeElse;

	public OutcomeIf getOutcomeIf() {
		return outcomeIf;
	}

	public void setOutcomeIf(OutcomeIf outcomeIf) {
		this.outcomeIf = outcomeIf;
	}

	public OutcomeElse getOutcomeElse() {
		return outcomeElse;
	}

	public void setOutcomeElse(OutcomeElse outcomeElse) {
		this.outcomeElse = outcomeElse;
	}

	@Override
	public String toString() {
		return "ClassPojo [outcomeIf = " + outcomeIf + ", outcomeElse = " + outcomeElse + "]";
	}
}