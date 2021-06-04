package i5.las2peer.services.onyxDataProxyService.pojo.assessmentResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
	
	/**
	 * The assessment result xml file may contain item results with the same 
	 * identifier (and the same sequence index) multiple times. As an example,
	 * if the solution to the question with sequence index 0 gets assessed manually
	 * by a teacher, then there will be two item results with sequence index 0.
	 * The first one is already present once the solution is submitted, the second 
	 * one appears after the manual grading. The list of item results returned by this
	 * method only contains one item result per sequence index (the latest one).
	 * @return Filtered list of item results (latest version of each item result is taken).
	 */
	public ArrayList<ItemResult> getFilteredItemResults() {
		ArrayList<ItemResult> filtered = new ArrayList<>();
		
		// map ItemResults by sequence index
		HashMap<Integer, List<ItemResult>> map = new HashMap<>();
		for(ItemResult ir : itemResults) {
			int sequenceIndex = Integer.parseInt(ir.getSequenceIndex());
			if(map.containsKey(sequenceIndex)) {
				map.get(sequenceIndex).add(ir);
			} else {
				ArrayList<ItemResult> list = new ArrayList<>();
				list.add(ir);
				map.put(sequenceIndex, list);
			}
		}
		
		// for each sequence index, use the latest item result that could be found
		for(int sequenceIndex : map.keySet()) {
			List<ItemResult> list = map.get(sequenceIndex);
			if(list.size() > 0) {
			    // use the latest element
				filtered.add(list.get(list.size()-1));
			}
		}
		
		return filtered;
	}

	@Override
	public String toString() {
		return "ClassPojo [testResult = " + testResult + ", itemResults = " + itemResults + "]";
	}
}
