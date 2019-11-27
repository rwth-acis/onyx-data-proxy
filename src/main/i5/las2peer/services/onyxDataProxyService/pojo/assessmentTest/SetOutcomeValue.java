package i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest;

public class SetOutcomeValue {
	private String identifier;

	private Object value;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "ClassPojo [identifier = " + identifier + ", value = " + value + "]";
	}
}
