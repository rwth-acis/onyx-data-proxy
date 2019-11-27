package i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(
		name = "rubricBlock")
@XmlAccessorType(XmlAccessType.FIELD)
public class RubricBlock {
	@XmlAttribute(
			name = "view")
	private String view;

	public String getView() {
		return view;
	}

	public void setView(String view) {
		this.view = view;
	}

	@Override
	public String toString() {
		return "ClassPojo [view = " + view + "]";
	}
}