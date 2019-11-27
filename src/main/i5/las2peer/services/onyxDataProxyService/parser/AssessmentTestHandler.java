package i5.las2peer.services.onyxDataProxyService.parser;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.AssessmentItemRef;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.AssessmentSection;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.AssessmentTest;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.BaseValue;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.DefaultValue;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.Gte;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.ItemSessionControl;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.OutcomeCondition;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.OutcomeDeclaration;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.OutcomeElse;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.OutcomeIf;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.OutcomeProcessing;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.RubricBlock;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.SetOutcomeValue;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.Sum;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.TestPart;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.TestVariables;

public class AssessmentTestHandler extends DefaultHandler {

	private AssessmentTest assessmentTest = null;
	private OutcomeDeclaration currentOutcomeDeclaration = null;
	private OutcomeCondition outcomeCondition = null;
	private AssessmentSection assessmentSection = null;
	private TestPart testPart = null;
	private Stack<Object> currentParents = null;
	private SetOutcomeValue setOutcomeValue = null;
	private Sum currentSum = null;
	private BaseValue currentBaseValue = null;

	private StringBuilder data = null;

	private int ifPosition = 0;
	private int gtePosition = 0;

	public AssessmentTestHandler() {
		super();
		currentParents = new Stack<Object>();
	}

	// getter method
	public AssessmentTest getAssessmentTest() {
		return assessmentTest;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// System.out.println(qName);
		if (qName.equalsIgnoreCase("assessmentTest")) {
			// create a new Employee and put it in Map
			assessmentTest = new AssessmentTest();
			assessmentTest.setIdentifier(attributes.getValue("identifier"));
			assessmentTest.setTitle(attributes.getValue("title"));
		} else if (qName.equalsIgnoreCase("outcomeDeclaration")) {
			OutcomeDeclaration outcome = new OutcomeDeclaration();
			outcome.setIdentifier(attributes.getValue("identifier"));
			outcome.setCardinality(attributes.getValue("cardinality"));
			outcome.setBaseType(attributes.getValue("baseType"));
			assessmentTest.addOutcomeDeclaration(outcome);
			currentOutcomeDeclaration = outcome;
		} else if (qName.equalsIgnoreCase("defaultValue")) {
			DefaultValue value = new DefaultValue();
			currentOutcomeDeclaration.setDefaultValue(value);
			currentParents.push(value);
		} else if (qName.equalsIgnoreCase("testPart")) {
			testPart = new TestPart();
			testPart.setIdentifier(attributes.getValue("identifier"));
			testPart.setNavigationMode(attributes.getValue("navigationMode"));
			testPart.setSubmissionMode(attributes.getValue("submissionMode"));
			assessmentTest.setTestPart(testPart);
		} else if (qName.equalsIgnoreCase("itemSessionControl")) {
			ItemSessionControl itemSessionControl = new ItemSessionControl();
			itemSessionControl.setAllowComment(attributes.getValue("allowComment"));
			itemSessionControl.setMaxAttempts(attributes.getValue("maxAttempts"));
			testPart.setItemSessionControl(itemSessionControl);
		} else if (qName.equalsIgnoreCase("assessmentSection")) {
			assessmentSection = new AssessmentSection();
			assessmentSection.setIdentifier(attributes.getValue("identifier"));
			assessmentSection.setFixed(attributes.getValue("fixed"));
			assessmentSection.setTitle(attributes.getValue("title"));
			assessmentSection.setVisible(attributes.getValue("visible"));
			testPart.setAssessmentSection(assessmentSection);
		} else if (qName.equalsIgnoreCase("rubricBlock")) {
			RubricBlock rubricBlock = new RubricBlock();
			rubricBlock.setView(attributes.getValue("view"));
			assessmentSection.setRubricBlock(rubricBlock);
		} else if (qName.equalsIgnoreCase("assessmentItemRef")) {
			AssessmentItemRef assessmentItemRef = new AssessmentItemRef();
			assessmentItemRef.setIdentifier(attributes.getValue("identifier"));
			assessmentItemRef.setHref(attributes.getValue("href"));
			assessmentItemRef.setFixed(attributes.getValue("fixed"));
			assessmentSection.addAssessmentItemRef(assessmentItemRef);
		} else if (qName.equalsIgnoreCase("outcomeProcessing")) {
			assessmentTest.setOutcomeProcessing(new OutcomeProcessing());
			currentParents.push(assessmentTest.getOutcomeProcessing());
		} else if (qName.equalsIgnoreCase("setOutcomeValue")) {
			setOutcomeValue = new SetOutcomeValue();
			setOutcomeValue.setIdentifier(attributes.getValue("identifier"));
			Object currentParent = currentParents.peek();
			if (currentParent instanceof OutcomeProcessing) {
				assessmentTest.getOutcomeProcessing().setSetOutcomeValue(setOutcomeValue);
			} else if (currentParent instanceof OutcomeElse) {
				((OutcomeElse) currentParent).setSetOutcomeValue(setOutcomeValue);
			} else if (currentParent instanceof OutcomeIf) {
				if (ifPosition == 0) {
					((OutcomeIf) currentParent).setFirst(setOutcomeValue);
					ifPosition++;
				} else {
					((OutcomeIf) currentParent).setSecond(setOutcomeValue);
					ifPosition = 0;
				}
			}
			currentParents.push(setOutcomeValue);
		} else if (qName.equalsIgnoreCase("sum")) {
			currentSum = new Sum();
			Object currentParent = currentParents.peek();
			if (currentParent instanceof SetOutcomeValue) {
				((SetOutcomeValue) currentParent).setValue(currentSum);
			} else if (currentParent instanceof Gte) {
				if (gtePosition == 0) {
					((Gte) currentParent).setFirst(currentSum);
					gtePosition++;
				} else {
					((Gte) currentParent).setSecond(currentSum);
					gtePosition = 0;
				}
			}
		} else if (qName.equalsIgnoreCase("testVariables")) {
			TestVariables testVariables = new TestVariables();
			testVariables.setVariableIdentifier(attributes.getValue("variableIdentifier"));
			currentSum.setTestVariables(testVariables);
		} else if (qName.equalsIgnoreCase("outcomeCondition")) {
			outcomeCondition = new OutcomeCondition();
			assessmentTest.getOutcomeProcessing().setOutcomeCondition(outcomeCondition);
		} else if (qName.equalsIgnoreCase("outcomeIf")) {
			OutcomeIf outcomeIf = new OutcomeIf();
			outcomeCondition.setOutcomeIf(outcomeIf);
			currentParents.push(outcomeIf);
		} else if (qName.equalsIgnoreCase("outcomeElse")) {
			OutcomeElse outcomeElse = new OutcomeElse();
			outcomeCondition.setOutcomeElse(outcomeElse);
			currentParents.push(outcomeElse);
		} else if (qName.equalsIgnoreCase("gte")) {
			Gte gte = new Gte();
			Object currentParent = currentParents.peek();
			if (currentParent instanceof OutcomeIf) {
				System.out.println(ifPosition);
				if (ifPosition == 0) {
					((OutcomeIf) currentParent).setFirst(gte);
					ifPosition++;
				} else {
					((OutcomeIf) currentParent).setSecond(gte);
					ifPosition = 0;
				}
			}
			currentParents.push(gte);
		} else if (qName.equalsIgnoreCase("baseValue")) {
			currentBaseValue = new BaseValue();
			currentBaseValue.setBaseType(attributes.getValue("baseType"));
			Object currentParent = currentParents.peek();
			if (currentParent instanceof Gte) {
				if (gtePosition == 0) {
					((Gte) currentParent).setFirst(currentBaseValue);
					gtePosition++;
				} else {
					((Gte) currentParent).setSecond(currentBaseValue);
					gtePosition = 0;
				}
			} else if (currentParent instanceof SetOutcomeValue) {
				((SetOutcomeValue) currentParent).setValue(currentBaseValue);
			}
		}
		// create the data container
		data = new StringBuilder();
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("value")) {
			((DefaultValue) currentParents.peek()).setValue(data.toString());
		} else if (qName.equalsIgnoreCase("baseValue")) {
			currentBaseValue.setContent(data.toString());
		} else if (qName.equalsIgnoreCase("defaultValue")) {
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("outcomeProcessing")) {
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("setOutcomeValue")) {
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("outcomeIf")) {
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("outcomeElse")) {
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("gte")) {
			currentParents.pop();
		}
	}

	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		data.append(new String(ch, start, length));
	}
}