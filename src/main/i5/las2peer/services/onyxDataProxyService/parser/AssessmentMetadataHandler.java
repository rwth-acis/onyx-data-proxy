package i5.las2peer.services.onyxDataProxyService.parser;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentMetadata;

public class AssessmentMetadataHandler extends DefaultHandler {
	private AssessmentMetadata assessmentMetadata = null;
	private Stack<Object> currentParents = null;
	private StringBuilder data = null;

	public AssessmentMetadataHandler() {
		super();
		this.assessmentMetadata = new AssessmentMetadata();
		this.currentParents = new Stack<Object>();
	}

	// getter method
	public AssessmentMetadata getAssessmentMetadata() {
		return assessmentMetadata;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase("title")) {
			currentParents.add(qName);
		} else if (qName.equalsIgnoreCase("description")) {
			currentParents.add(qName);
		} else if (qName.equalsIgnoreCase("file")) {
			assessmentMetadata.addFile(attributes.getValue("href"));
		}

		// create the data container
		data = new StringBuilder();
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("entry")) {
			assessmentMetadata.setId(data.toString());
		} else if (qName.equalsIgnoreCase("string")) {
			String parent = (String) currentParents.peek();
			if (parent.equalsIgnoreCase("title")) {
				assessmentMetadata.setTitle(data.toString());
			} else if (parent.equalsIgnoreCase("description")) {
				assessmentMetadata.setDescription(data.toString());
			}
		} else if (qName.equalsIgnoreCase("title")) {
			currentParents.pop();
		} else if (qName.equalsIgnoreCase("description")) {
			currentParents.pop();
		}
	}

	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		data.append(new String(ch, start, length));
	}
}
