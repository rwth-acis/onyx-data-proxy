package i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(
		name = "assessmentItemRef")
@XmlAccessorType(XmlAccessType.FIELD)
public class AssessmentItemRef {
	@XmlAttribute(
			name = "identifier")
	private String identifier;

	@XmlAttribute(
			name = "fixed")
	private String fixed;

	@XmlAttribute(
			name = "href")
	private String href;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getFixed() {
		return fixed;
	}

	public void setFixed(String fixed) {
		this.fixed = fixed;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	@Override
	public String toString() {
		return "ClassPojo [identifier = " + identifier + ", fixed = " + fixed + ", href = " + href + "]";
	}
}
