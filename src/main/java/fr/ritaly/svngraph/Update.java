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

import static fr.ritaly.svngraph.Utils.validateElement;
import static fr.ritaly.svngraph.Utils.*;

import java.text.ParseException;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

public final class Update {

	public static enum Kind {
		FILE, DIR;

		public static Kind getKind(String value) {
			if ("file".equals(value)) {
				return FILE;
			} else if ("dir".equals(value)) {
				return DIR;
			}

			throw new IllegalArgumentException(String.format("Invalid kind '%s'", value));
		}
	}

	public static enum Action {
		ADD, DELETE, MODIFY, REPLACE;

		public static Action getAction(String value) {
			if ("A".equals(value)) {
				return ADD;
			} else if ("D".equals(value)) {
				return DELETE;
			} else if ("M".equals(value)) {
				return MODIFY;
			} else if ("R".equals(value)) {
				return REPLACE;
			}

			throw new IllegalArgumentException(String.format("Invalid action '%s'", value));
		}
	}

	private final Kind kind;

	private final Action action;

	private final String path;

	private String copyFromPath;

	private long copyFromRev = -1;

	private boolean merge;

	public Update(Element element) throws ParseException {
		validateElement(element, "path");

		this.kind = Kind.getKind(element.getAttribute("kind"));
		this.action = Action.getAction(element.getAttribute("action"));
		this.path = element.getTextContent();

		if (element.hasAttribute("copyfrom-path")) {
			this.copyFromPath = element.getAttribute("copyfrom-path");
		}
		if (element.hasAttribute("copyfrom-rev")) {
			this.copyFromRev = Long.parseLong(element.getAttribute("copyfrom-rev"));
		}
		if (element.hasAttribute("text-mods")) {
			this.merge = Boolean.parseBoolean(element.getAttribute("text-mods"));
		}
	}

	public boolean isMerge() {
		return merge;
	}

	public boolean isCopy() {
		return (this.copyFromPath != null) && (this.copyFromRev != -1);
	}

	public RevisionPath getCopySource() {
		if (isCopy()) {
			return new RevisionPath(this.copyFromPath, this.copyFromRev);
		}

		return null;
	}

	public String getPath() {
		return path;
	}

	public Kind getKind() {
		return kind;
	}

	public Action getAction() {
		return action;
	}

	// tells whether the given path is updated by this instance
	public boolean isOnPath(String path) {
		Validate.notNull(path, "The given path is null");

		return this.path.startsWith(path);
	}

	public boolean isSignificant() {
		// a significant update is one affecting a key directory (a branch, a
		// tag, the trunk) with a valid action (not MODIFY or DELETE)
		if (getKind() == Kind.FILE) {
			if (isMerge()) {
				return true;
			}
		}

		if (getKind() != Kind.DIR) {
			return false;
		}
		if ((getAction() == Action.MODIFY) || (getAction() == Action.DELETE)) {
			return false;
		}

		return isTrunkPath(this.path) || isBranchPath(this.path) || isTagPath(this.path);
	}
}
