package i5.las2peer.services.onyxDataProxyService.parser;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult.AssessmentResult;

public class AssessmentResultParser {
	public static AssessmentResult parseAssessmentResult(String xml) throws Exception {
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		AssessmentResult assessmentResult = null;
		try {
			SAXParser saxParser = saxParserFactory.newSAXParser();
			AssessmentResultHandler handler = new AssessmentResultHandler();
			StringReader reader = new StringReader(xml);
			saxParser.parse(new InputSource(reader), handler);
			assessmentResult = handler.getAssessmentResult();

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}

		return assessmentResult;
	}
}
