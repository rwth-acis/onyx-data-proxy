package i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest;

import java.io.Serializable;

public class TestPart implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2278685288045975171L;

	private String identifier;

	private AssessmentSection assessmentSection;

	private String navigationMode;

	private String submissionMode;

	private ItemSessionControl itemSessionControl;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public AssessmentSection getAssessmentSection() {
		return assessmentSection;
	}

	public void setAssessmentSection(AssessmentSection assessmentSection) {
		this.assessmentSection = assessmentSection;
	}

	public String getNavigationMode() {
		return navigationMode;
	}

	public void setNavigationMode(String navigationMode) {
		this.navigationMode = navigationMode;
	}

	public String getSubmissionMode() {
		return submissionMode;
	}

	public void setSubmissionMode(String submissionMode) {
		this.submissionMode = submissionMode;
	}

	public ItemSessionControl getItemSessionControl() {
		return itemSessionControl;
	}

	public void setItemSessionControl(ItemSessionControl itemSessionControl) {
		this.itemSessionControl = itemSessionControl;
	}

	@Override
	public String toString() {
		return "ClassPojo [identifier = " + identifier + ", assessmentSection = " + assessmentSection
				+ ", navigationMode = " + navigationMode + ", submissionMode = " + submissionMode
				+ ", itemSessionControl = " + itemSessionControl + "]";
	}
}