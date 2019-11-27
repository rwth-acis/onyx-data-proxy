package i5.las2peer.services.onyxDataProxyService.pojo.misc;

import java.util.ArrayList;

public class AssessmentMetadata {
	private String id;
	private String title;
	private String description;
	private ArrayList<String> files;

	public AssessmentMetadata() {
		this.files = new ArrayList<String>();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ArrayList<String> getFiles() {
		return files;
	}

	public void setFiles(ArrayList<String> files) {
		this.files = files;
	}

	public void addFile(String file) {
		this.files.add(file);
	}
}
