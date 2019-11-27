package i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest;

import java.util.ArrayList;

public class AssessmentTest {

	private String identifier;

	private String xmlns;

	private TestPart testPart;

	public TestPart getTestPart() {
		return testPart;
	}

	public void setTestPart(TestPart testPart) {
		this.testPart = testPart;
	}

	private ArrayList<OutcomeDeclaration> outcomeDeclarations;

	private OutcomeProcessing outcomeProcessing;

	private String title;

	public AssessmentTest() {
		super();
		outcomeDeclarations = new ArrayList<OutcomeDeclaration>();
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getXmlns() {
		return xmlns;
	}

	public void setXmlns(String xmlns) {
		this.xmlns = xmlns;
	}

	public OutcomeProcessing getOutcomeProcessing() {
		return outcomeProcessing;
	}

	public void setOutcomeProcessing(OutcomeProcessing outcomeProcessing) {
		this.outcomeProcessing = outcomeProcessing;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ArrayList<OutcomeDeclaration> getOutcomeDeclarations() {
		return outcomeDeclarations;
	}

	public void setOutcomeDeclarations(ArrayList<OutcomeDeclaration> outcomeDeclarations) {
		this.outcomeDeclarations = outcomeDeclarations;
	}

	public void addOutcomeDeclaration(OutcomeDeclaration outcomeDeclaration) {
		this.outcomeDeclarations.add(outcomeDeclaration);
	}

	@Override
	public String toString() {
		return "ClassPojo [identifier = " + identifier + ", xmlns = " + xmlns + ", testPart = " + testPart
				+ ", outcomeDeclarations = " + outcomeDeclarations + ", outcomeProcessing = " + outcomeProcessing
				+ ", title = " + title + "]";
	}
}