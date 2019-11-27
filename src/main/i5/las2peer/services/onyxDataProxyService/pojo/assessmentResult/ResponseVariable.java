package i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult;

public class ResponseVariable {
	private String identifier;
	private String cardinality;
	private String baseType;
	private String choiceSequence;
	private CandidateResponse candidateResponse;
	private CorrectResponse correctResponse;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getCardinality() {
		return cardinality;
	}

	public void setCardinality(String cardinality) {
		this.cardinality = cardinality;
	}

	public String getBaseType() {
		return baseType;
	}

	public void setBaseType(String baseType) {
		this.baseType = baseType;
	}

	public CandidateResponse getCandidateResponse() {
		return candidateResponse;
	}

	public void setCandidateResponse(CandidateResponse candidateResponse) {
		this.candidateResponse = candidateResponse;
	}

	public CorrectResponse getCorrectResponse() {
		return correctResponse;
	}

	public void setCorrectResponse(CorrectResponse correctResponse) {
		this.correctResponse = correctResponse;
	}

	public String getChoiceSequence() {
		return choiceSequence;
	}

	public void setChoiceSequence(String choiceSequence) {
		this.choiceSequence = choiceSequence;
	}
}
