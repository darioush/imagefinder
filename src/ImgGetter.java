import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ImgGetter {
	
	public void getTagsFromURL(String location, TagProcessor processor) throws IOException {
		String contents = readUrlToString(location);
		getTagsFromString(contents, processor);
	}
	
	private void getTagsFromString(String contents, TagProcessor processor) {
		int startPos = 0;
		Matcher tag = Pattern.compile("(?:<\\s*(\\w+)(\\s+|/?>))|(?:<!--)").matcher(contents);
		Matcher endComment = Pattern.compile("-->").matcher(contents);
		Matcher attribute = Pattern.compile("\\s*(\\w+)\\s*(=|\\s+|/?>)").matcher(contents);
		Matcher endOfTag = Pattern.compile("\\s*/?>").matcher(contents);
		while (startPos < contents.length() && tag.find(startPos)) {
			if ("<!--".equals(tag.group())) {
				boolean endCommentFound = endComment.find(tag.end());
				if (!endCommentFound) {
					throw new RuntimeException("Cannot find end of HTML comment");
				}
				startPos = endComment.end();
			} else {
				String tagName = tag.group(1);
				String endString = tag.group(2);
				processor.tagFound(tagName);
				startPos = tag.end();
				
				while (!endString.endsWith(">")  && attribute.find(startPos)) {
					String attributeName = attribute.group(1);
					endString = attribute.group(2);
					processor.attributeFound(attributeName);
					startPos = attribute.end();			
					if ("=".equals(endString)) {
						startPos = readValue(contents, startPos, processor);
					}
					endOfTag.find(startPos);
					if (endOfTag.start() == startPos) {
						startPos = endOfTag.end();
						break;
					}
				}
			}
		}
	}
	
	private int readValue(String contents, int startPos, TagProcessor processor) {
		Matcher matcher = null;
		if ('"' == contents.charAt(startPos)) {
			matcher = Pattern.compile("\"([^\"]*)\"").matcher(contents);
		} else if ('\'' == contents.charAt(startPos)) {
			matcher = Pattern.compile("'([^']*)'").matcher(contents);
		} else {
			matcher = Pattern.compile("(\\S+)").matcher(contents);
			
		}
		matcher.find(startPos);
		processor.attributeValueFound(attributeHtmlUnescape(matcher.group(1)));
		return matcher.end();
	}
	
	private String attributeHtmlUnescape(String value) {
		return value.replaceAll("&quot;", "\"");
	}

	
	private String readUrlToString(String location) throws IOException {
		URL url = new URL(location);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setInstanceFollowRedirects(false);  // so we can handle redirects ourselves
		conn.connect();
		if (conn.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM ||
				conn.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
			conn.disconnect();
			URL nextUrl = new URL(url, conn.getHeaderField("Location"));  // deal with relative urls
			return readUrlToString(nextUrl.toExternalForm());
		}
		InputStream instream = conn.getInputStream();
		InputStreamReader inreader = new InputStreamReader(instream);
		BufferedReader reader = new BufferedReader(inreader);
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		reader.close();
		inreader.close();
		instream.close();
		conn.disconnect();
		return sb.toString();
	}
	
	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("usage: java ImgGetter <url>");
			return;
		}
		try {
			new ImgGetter().getTagsFromURL(args[0], new ImagePrinter());
		} catch (MalformedURLException e) {
			System.err.println("Provided url is malformed.");
		} catch (IOException e) {
			System.err.println("I/O Error while reading data from given url.");
		}
	}
}
