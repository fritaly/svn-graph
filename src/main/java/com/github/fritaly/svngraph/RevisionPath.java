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
package com.github.fritaly.svngraph;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public final class RevisionPath implements Comparable<RevisionPath> {

	private final String path;

	private final long revision;

	RevisionPath(String path, long revision) {
		this.path = path;
		this.revision = revision;
	}

	public String getPath() {
		return path;
	}

	public long getRevision() {
		return revision;
	}

	@Override
	public int compareTo(RevisionPath other) {
		if (!this.path.equals(other.path)) {
			// compare first on the path
			return this.path.compareTo(other.path);
		}

		// then the revision
		final long delta = this.revision - other.revision;

		return (delta < 0) ? -1 : (delta > 0) ? +1 : 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof RevisionPath) {
			final RevisionPath other = (RevisionPath) obj;

			return (this.revision == other.revision) && StringUtils.equals(this.path, other.path);
		}

		return false;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("path", path).append("revision", revision)
				.toString();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(11, 31).append(path).append(revision).toHashCode();
	}
}