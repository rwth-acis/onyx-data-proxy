package i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult;

import java.util.ArrayList;

public class AssessmentResult {
	private TestResult testResult;
	private ArrayList<ItemResult> itemResults;

	public AssessmentResult() {
		this.itemResults = new ArrayList<ItemResult>();
	}

	public TestResult getTestResult() {
		return testResult;
	}

	public void setTestResult(TestResult testResult) {
		this.testResult = testResult;
	}

	public ArrayList<ItemResult> getItemResults() {
		return itemResults;
	}

	public void setItemResults(ArrayList<ItemResult> itemResult) {
		this.itemResults = itemResult;
	}

	public void addItemResult(ItemResult itemresult) {
		this.itemResults.add(itemresult);
	}

	@Override
	public String toString() {
		return "ClassPojo [testResult = " + testResult + ", itemResults = " + itemResults + "]";
	}
}
