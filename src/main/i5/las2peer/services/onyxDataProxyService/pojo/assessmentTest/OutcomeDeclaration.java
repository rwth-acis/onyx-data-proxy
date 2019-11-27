package i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest;

public class OutcomeDeclaration {
	private String identifier;

	private DefaultValue defaultValue;

	private String cardinality;

	private String baseType;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public DefaultValue getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(DefaultValue defaultValue) {
		this.defaultValue = defaultValue;
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

	@Override
	public String toString() {
		return "ClassPojo [identifier = " + identifier + ", defaultValue = " + defaultValue + ", cardinality = "
				+ cardinality + ", baseType = " + baseType + "]";
	}
}