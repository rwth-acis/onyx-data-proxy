package i5.las2peer.services.onyxDataProxyService.parser;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.AssessmentResult;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.CandidateResponse;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.CorrectResponse;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.ItemResult;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.OutcomeVariable;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.ResponseVariable;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.TemplateVariable;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.TestResult;
import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.Value;

public class AssessmentResultHandler extends DefaultHandler {
	private AssessmentResult assessmentResult = null;
	private Stack<Object> currentParents = null;
	private StringBuilder data = null;

	public AssessmentResultHandler() {
		super();
		currentParents = new Stack<Object>();
	}

	// getter method
	public AssessmentResult getAssessmentResult() {
		return assessmentResult;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// System.out.println(qName);
		if (qName.equalsIgnoreCase("assessmentResult")) {
			// create a new Employee and put it in Map
			assessmentResult = new AssessmentResult();
		} else if (qName.equalsIgnoreCase("testResult")) {
			// create a new Employee and put it in Map
			TestResult testResult = new TestResult();
			testResult.setIdentifier(attributes.getValue("identifier"));
			testResult.setDatestamp(attributes.getValue("datestamp"));
			testResult.setCurrentItemIndex(attributes.getValue("currentItemIndex"));
			assessmentResult.setTestResult(testResult);
			currentParents.add(testResult);
		} else if (qName.equalsIgnoreCase("responseVariable")) {
			ResponseVariable responseVariable = new ResponseVariable();
			responseVariable.setIdentifier(attributes.getValue("identifier"));
			responseVariable.setCardinality(attributes.getValue("cardinality"));
			responseVariable.setBaseType(attributes.getValue("baseType"));
			responseVariable.setChoiceSequence(attributes.getValue("choiceSequence"));

			if (currentParents.peek() instanceof TestResult) {
				((TestResult) currentParents.peek()).addResponseVariable(responseVariable);
			} else if (currentParents.peek() instanceof ItemResult) {
				((ItemResult) currentParents.peek()).addResponseVariable(responseVariable);
			}
			currentParents.add(responseVariable);
		} else if (qName.equalsIgnoreCase("candidateResponse")) {
			CandidateResponse candidateResponse = new CandidateResponse();
			((ResponseVariable) currentParents.peek()).setCandidateResponse(candidateResponse);
			currentParents.add(candidateResponse);
		} else if (qName.equalsIgnoreCase("value")) {
			Value value = new Value();
			value.setBaseType(attributes.getValue("baseType"));
			if (currentParents.peek() instanceof CandidateResponse) {
				((CandidateResponse) currentParents.peek()).setValue(value);
			} else if (currentParents.peek() instanceof OutcomeVariable) {
				((OutcomeVariable) currentParents.peek()).setValue(value);
			} else if (currentParents.peek() instanceof CorrectResponse) {
				((CorrectResponse) currentParents.peek()).setValue(value);
			} else if (currentParents.peek() instanceof TemplateVariable) {
				((TemplateVariable) currentParents.peek()).setValue(value);
			}
			currentParents.add(value);
		} else if (qName.equalsIgnoreCase("outcomeVariable")) {
			OutcomeVariable outcomeVariable = new OutcomeVariable();
			outcomeVariable.setIdentifier(attributes.getValue("identifier"));
			outcomeVariable.setCardinality(attributes.getValue("cardinality"));
			outcomeVariable.setManualScored(attributes.getValue("manualScored"));
			outcomeVariable.setBaseType(attributes.getValue("baseType"));
			outcomeVariable.setView(attributes.getValue("view"));
			if (currentParents.peek() instanceof TestResult) {
				((TestResult) currentParents.peek()).addOutcomeVariable(outcomeVariable);
			}
			if (currentParents.peek() instanceof ItemResult) {
				((ItemResult) currentParents.peek()).addOutcomeVariable(outcomeVariable);
			}
			currentParents.add(outcomeVariable);
		} else if (qName.equalsIgnoreCase("templateVariable")) {
			TemplateVariable templateVariable = new TemplateVariable();
			templateVariable.setIdentifier(attributes.getValue("identifier"));
			templateVariable.setCardinality(attributes.getValue("cardinality"));
			templateVariable.setBaseType(attributes.getValue("baseType"));
			if (currentParents.peek() instanceof TestResult) {
				((TestResult) currentParents.peek()).addTemplateVariable(templateVariable);
			}
			if (currentParents.peek() instanceof ItemResult) {
				((ItemResult) currentParents.peek()).addTemplateVariable(templateVariable);
			}
			currentParents.add(templateVariable);
		} else if (qName.equalsIgnoreCase("itemResult")) {
			ItemResult itemResult = new ItemResult();
			itemResult.setIdentifier(attributes.getValue("identifier"));
			itemResult.setDateStamp(attributes.getValue("datestamp"));
			itemResult.setSessionStatus(attributes.getValue("sessionStatus"));
			itemResult.setSequenceIndex(attributes.getValue("sequenceIndex"));

			assessmentResult.addItemResult(itemResult);
			currentParents.add(itemResult);
		} else if (qName.equalsIgnoreCase("correctResponse")) {
			CorrectResponse correctResponse = new CorrectResponse();
			((ResponseVariable) currentParents.peek()).setCorrectResponse(correctResponse);
			currentParents.add(correctResponse);
		}
		// create the data container
		data = new StringBuilder();
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("testResult")) {
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("responseVariable")) {
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("candidateResponse")) {
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("value")) {
			((Value) currentParents.peek()).setValue(data.toString());
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("responseVariable")) {
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("outcomeVariable")) {
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("templateVariable")) {
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("itemResult")) {
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("correctResponse")) {
			currentParents.pop();
		}
	}

	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		data.append(new String(ch, start, length));
	}
}
