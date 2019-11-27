package i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest;

public class BaseValue {
	private String content;

	private String baseType;

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getBaseType() {
		return baseType;
	}

	public void setBaseType(String baseType) {
		this.baseType = baseType;
	}

	@Override
	public String toString() {
		return "ClassPojo [content = " + content + ", baseType = " + baseType + "]";
	}
}