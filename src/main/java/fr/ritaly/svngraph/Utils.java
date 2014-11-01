package fr.ritaly.svngraph;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Utils {

	public static void validateElement(Element element, String name) {
		Validate.notNull(element, "The given DOM element is null");
		Validate.isTrue(
				element.getNodeName().equals(name),
				String.format("The name of the given element isn't valid (Expected: '%s', Actual: '%s'", name,
						element.getNodeName()));
	}

	public static Element getChild(Element root, String childName) {
		final NodeList children = root.getElementsByTagName(childName);

		if (children.getLength() == 1) {
			return (Element) children.item(0);
		}

		throw new IllegalArgumentException(String.format("The given element defines %d node(s) named '%s'", children.getLength(),
				childName));
	}

	public static boolean isTrunkPath(String path) {
		// no trailing slash expected when pointing directly to a directory
		return path.endsWith("/trunk");
	}

	// no trailing slash expected when pointing directly to a directory
	private static final Pattern BRANCH_PATTERN = Pattern.compile(".*/branches/([^/]+)");

	// no trailing slash expected when pointing directly to a directory
	private static final Pattern TAG_PATTERN = Pattern.compile(".*/tags/([^/]+)");

	public static boolean isBranchPath(String path) {
		return (getBranchName(path) != null);
	}

	public static String getBranchName(String path) {
		final Matcher matcher = BRANCH_PATTERN.matcher(path);

		return matcher.matches() ? matcher.group(1) : null;
	}

	public static boolean isTagPath(String path) {
		return (getTagName(path) != null);
	}

	public static String getTagName(String path) {
		final Matcher matcher = TAG_PATTERN.matcher(path);

		return matcher.matches() ? matcher.group(1) : null;
	}

	public static String getRootName(String path) {
		if (isTrunkPath(path)) {
			return "trunk";
		}
		if (isBranchPath(path)) {
			return getBranchName(path);
		}
		if (isTagPath(path)) {
			return getTagName(path);
		}

		return null;
	}
}
