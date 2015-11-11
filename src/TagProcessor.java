
public interface TagProcessor {
	public void tagFound(String name);
	public void attributeFound(String attribute);
	public void attributeValueFound(String value);
}
