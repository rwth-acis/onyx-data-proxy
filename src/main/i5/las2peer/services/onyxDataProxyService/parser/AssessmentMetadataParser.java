package i5.las2peer.services.onyxDataProxyService.parser;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import i5.las2peer.services.onyxDataProxyService.pojo.misc.AssessmentMetadata;

public class AssessmentMetadataParser {

	public static AssessmentMetadata parseMetadata(String xml) throws Exception {
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		AssessmentMetadata assessmentMetadata = null;
		try {
			SAXParser saxParser = saxParserFactory.newSAXParser();
			AssessmentMetadataHandler handler = new AssessmentMetadataHandler();
			StringReader reader = new StringReader(xml);
			saxParser.parse(new InputSource(reader), handler);
			assessmentMetadata = handler.getAssessmentMetadata();

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}

		return assessmentMetadata;
	}
}
