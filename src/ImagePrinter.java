
public class ImagePrinter implements TagProcessor {
	private String lastTag = "";
	private String lastAttribute = "";
	
	@Override
	public void tagFound(String name) {
		this.lastTag = name;
	}

	@Override
	public void attributeFound(String attribute) {
		this.lastAttribute = attribute;
	}

	@Override
	public void attributeValueFound(String value) {
		if ("img".equals(lastTag) && "src".equals(lastAttribute)) {
			System.out.println(value);
		}
	}
}
