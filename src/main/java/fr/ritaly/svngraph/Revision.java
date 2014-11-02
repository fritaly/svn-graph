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

import static fr.ritaly.svngraph.Utils.getChild;
import static fr.ritaly.svngraph.Utils.hasChild;
import static fr.ritaly.svngraph.Utils.validateElement;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class Revision {

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

	private final long number;

	private final String author;

	private final Date date;

	private final String message;

	private final List<Update> updates = new ArrayList<>();

	public Revision(Element element) throws ParseException {
		validateElement(element, "logentry");

		this.number = Long.parseLong(element.getAttribute("revision"));
		this.author = hasChild(element, "author") ? getChild(element, "author").getTextContent() : null;
		this.date = DATE_FORMAT.parse(getChild(element, "date").getTextContent());
		this.message = hasChild(element, "msg") ? getChild(element, "msg").getTextContent() : null;

		// Parse the affected paths
		final NodeList nodes = getChild(element, "paths").getElementsByTagName("path");

		for (int i = 0; i < nodes.getLength(); i++) {
			this.updates.add(new Update((Element) nodes.item(i)));
		}
	}

	public List<Update> getUpdates() {
		return Collections.unmodifiableList(updates);
	}

	public List<Update> getSignificantUpdates() {
		final List<Update> list = new ArrayList<>();

		for (Update update : updates) {
			if (update.isSignificant()) {
				list.add(update);
			}
		}

		return list;
	}

	public long getNumber() {
		return number;
	}

	public String getAuthor() {
		return author;
	}

	public Date getDate() {
		return new Date(date.getTime());
	}

	public String getMessage() {
		return message;
	}

	public boolean isSignificant() {
		// a revision is significant if one of its attached updates is significant
		for (Update update : updates) {
			if (update.isSignificant()) {
				return true;
			}
		}

		return false;
	}
}
