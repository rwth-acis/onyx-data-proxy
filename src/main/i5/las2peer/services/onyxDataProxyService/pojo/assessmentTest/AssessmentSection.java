package i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest;

import java.util.ArrayList;

public class AssessmentSection {
	private String identifier;

	private String visible;

	private RubricBlock rubricBlock;

	private String fixed;

	private String title;

	private ArrayList<AssessmentItemRef> assessmentItemRefs;

	public AssessmentSection() {
		super();
		this.assessmentItemRefs = new ArrayList<AssessmentItemRef>();
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getVisible() {
		return visible;
	}

	public void setVisible(String visible) {
		this.visible = visible;
	}

	public RubricBlock getRubricBlock() {
		return rubricBlock;
	}

	public void setRubricBlock(RubricBlock rubricBlock) {
		this.rubricBlock = rubricBlock;
	}

	public String getFixed() {
		return fixed;
	}

	public void setFixed(String fixed) {
		this.fixed = fixed;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ArrayList<AssessmentItemRef> getAssessmentItemRefs() {
		return assessmentItemRefs;
	}

	public void setAssessmentItemRefs(ArrayList<AssessmentItemRef> assessmentItemRefs) {
		this.assessmentItemRefs = assessmentItemRefs;
	}

	public void addAssessmentItemRef(AssessmentItemRef assessmentItemRef) {
		this.assessmentItemRefs.add(assessmentItemRef);
	}

	@Override
	public String toString() {
		return "ClassPojo [identifier = " + identifier + ", visible = " + visible + ", rubricBlock = " + rubricBlock
				+ ", fixed = " + fixed + ", title = " + title + ", assessmentItemRef = " + assessmentItemRefs + "]";
	}

}