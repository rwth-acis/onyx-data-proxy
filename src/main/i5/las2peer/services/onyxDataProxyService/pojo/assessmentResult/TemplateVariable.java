package i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult;

public class TemplateVariable {
	private String identifier;
	private String cardinality;
	private String baseType;
	private Value value;
	
	public String getIdentifier() {
		return this.identifier;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	public String getCardinality() {
		return this.cardinality;
	}
	
	public void setCardinality(String cardinality) {
		this.cardinality = cardinality;
	}
	
	public String getBaseType() {
		return this.baseType;
	}
	
	public void setBaseType(String baseType) {
		this.baseType = baseType;
	}
	
	public Value getValue() {
		return this.value;
	}
	
	public void setValue(Value value) {
		this.value = value;
	}

}
