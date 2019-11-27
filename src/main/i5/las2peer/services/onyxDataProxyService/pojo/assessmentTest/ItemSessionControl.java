package i5.las2peer.services.onyxDataProxyService.pojo.assessmentTest;

public class ItemSessionControl {
	private String maxAttempts;

	private String allowComment;

	public String getMaxAttempts() {
		return maxAttempts;
	}

	public void setMaxAttempts(String maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	public String getAllowComment() {
		return allowComment;
	}

	public void setAllowComment(String allowComment) {
		this.allowComment = allowComment;
	}

	@Override
	public String toString() {
		return "ClassPojo [maxAttempts = " + maxAttempts + ", allowComment = " + allowComment + "]";
	}
}