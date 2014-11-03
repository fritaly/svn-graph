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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

// TODO add javadoc
// TODO add unit tests based on svn log of a public open source project
public final class History {

	private final Map<Long, Revision> revisions = new TreeMap<>();

	private History(Collection<Revision> collection) {
		Validate.notNull(collection, "The given collection of revisions is null");

		for (Revision revision : collection) {
			this.revisions.put(revision.getNumber(), revision);
		}
	}

	public History(Document document) throws XPathExpressionException, ParseException {
		Validate.notNull(document, "The given document is null");

		final XPath xpath = XPathFactory.newInstance().newXPath();

		NodeList nodes = (NodeList) xpath.evaluate("/log/logentry", document.getDocumentElement(), XPathConstants.NODESET);

		for (int i = 0; i < nodes.getLength(); i++) {
			final Revision revision = new Revision((Element) nodes.item(i));

			revisions.put(revision.getNumber(), revision);
		}

		System.out.println(String.format("Parsed %d revisions", revisions.size()));
	}

	public int getRevisionCount() {
		return revisions.size();
	}

	public List<Revision> getRevisions() {
		return new ArrayList<>(revisions.values());
	}

	public History getHistory(String path) {
		Validate.notNull(path, "The given path is null");

		final List<Revision> list = new ArrayList<>();

		for (Revision revision : revisions.values()) {
			if (revision.isOnPath(path)) {
				list.add(revision);
			}
		}

		return new History(list);
	}

	public Set<String> getRootPaths() {
		final Set<String> set = new TreeSet<>();

		for (Revision revision : revisions.values()) {
			for (Update update : revision.getUpdates()) {
				final String path = Utils.getRootPath(update.getPath());

				if (path != null) {
					set.add(path);
				}
			}
		}

		return set;
	}

	public boolean isRootPath(String path) {
		Validate.notNull(path, "The given path is null");

		return getRootPaths().contains(path);
	}

	public Revision getRevision(long revision) {
		return revisions.get(new Long(revision));
	}

	public List<Revision> getSignificantRevisions() {
		final List<Revision> list = new ArrayList<>();

		for (Revision revision : revisions.values()) {
			if (revision.isSignificant()) {
				list.add(revision);
			}
		}

		return list;
	}
}