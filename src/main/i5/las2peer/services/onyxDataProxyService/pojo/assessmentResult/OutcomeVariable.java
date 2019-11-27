package i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult;

public class OutcomeVariable {
	private String identifier;
	private String cardinality;
	private String manualScored;
	private String baseType;
	private String view;
	private Value value;

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

	public String getManualScored() {
		return manualScored;
	}

	public void setManualScored(String manualScored) {
		this.manualScored = manualScored;
	}

	public String getBaseType() {
		return baseType;
	}

	public void setBaseType(String baseType) {
		this.baseType = baseType;
	}

	public String getView() {
		return view;
	}

	public void setView(String view) {
		this.view = view;
	}

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
	}
}
