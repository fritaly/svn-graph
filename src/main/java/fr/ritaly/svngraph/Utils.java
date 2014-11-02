/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.ritaly.svngraph;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
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

	public static boolean hasChild(Element root, String childName) {
		return (root.getElementsByTagName(childName).getLength() > 0);
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

	private static final Pattern MODULE_PATTERN = Pattern.compile(".*/([^/]+)/(trunk|branches|tags)/.*");

	public static boolean isBranchPath(String path) {
		return (getBranchName(path) != null);
	}

	public static String getBranchName(String path) {
		final Matcher matcher = BRANCH_PATTERN.matcher(path);

		return matcher.matches() ? matcher.group(1) : null;
	}

	public static String getBranchPath(String path) {
		final Pattern pattern = Pattern.compile("(.*/branches/([^/]+))(/.*)?");

		final Matcher matcher = pattern.matcher(path);

		return matcher.matches() ? matcher.group(1) : null;
	}

	public static boolean isTagPath(String path) {
		return (getTagName(path) != null);
	}

	public static String getTagName(String path) {
		final Matcher matcher = TAG_PATTERN.matcher(path);

		return matcher.matches() ? matcher.group(1) : null;
	}

	public static String getTagPath(String path) {
		final Pattern pattern = Pattern.compile("(.*/tags/([^/]+))(/.*)?");

		final Matcher matcher = pattern.matcher(path);

		return matcher.matches() ? matcher.group(1) : null;
	}

	public static String getTrunkPath(String path) {
		final Pattern pattern = Pattern.compile("(.*/trunk)(/.*)?");

		final Matcher matcher = pattern.matcher(path);

		return matcher.matches() ? matcher.group(1) : null;
	}

	public static String getModule(String path) {
		final Matcher matcher = MODULE_PATTERN.matcher(path);

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
		if (path.contains("/trunk/")) {
			return "trunk";
		}

		final Pattern pattern = Pattern.compile(".*/(branches|tags)/([^/]+)(/.*)?");

		final Matcher matcher = pattern.matcher(path);

		if (matcher.matches()) {
			return matcher.group(2);
		}

		return null;
	}

	public static String getRootPath(String path) {
		String result = getTrunkPath(path);

		if (result != null) {
			return result;
		}

		result = getBranchPath(path);

		if (result != null) {
			return result;
		}

		result = getTagPath(path);

		return result;
	}
}
