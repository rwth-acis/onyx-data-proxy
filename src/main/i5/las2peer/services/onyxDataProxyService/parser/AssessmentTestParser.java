package i5.las2peer.services.onyxDataProxyService.parser;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest.AssessmentTest;

public class AssessmentTestParser {

	public static AssessmentTest parseAssessmentTest(String xml) throws Exception {
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		AssessmentTest assessmentTest = null;
		try {
			SAXParser saxParser = saxParserFactory.newSAXParser();
			AssessmentTestHandler handler = new AssessmentTestHandler();
			StringReader reader = new StringReader(xml);
			saxParser.parse(new InputSource(reader), handler);
			assessmentTest = handler.getAssessmentTest();

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}

		return assessmentTest;
	}

}
