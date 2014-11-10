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

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.math.RandomUtils;
import org.w3c.dom.Document;

import com.github.fritaly.graphml4j.EdgeStyle;
import com.github.fritaly.graphml4j.GroupStyles;
import com.github.fritaly.graphml4j.NodeStyle;
import com.github.fritaly.graphml4j.Renderer;
import com.github.fritaly.graphml4j.datastructure.Edge;
import com.github.fritaly.graphml4j.datastructure.Graph;
import com.github.fritaly.graphml4j.datastructure.Node;


public class SvnGraph {

	private static final class CustomRenderer implements Renderer {
		@Override
		public boolean isGroupOpen(Node node) {
			return true;
		}

		@Override
		public NodeStyle getNodeStyle(Node node) {
			return new NodeStyle();
		}

		@Override
		public String getNodeLabel(Node node) {
			final RevisionPath data = (RevisionPath) node.getData();

			return String.format("%s@%d", data.getPath(), data.getRevision());
		}

		@Override
		public GroupStyles getGroupStyles(Node node) {
			return new GroupStyles();
		}

		@Override
		public EdgeStyle getEdgeStyle(Edge edge) {
			return new EdgeStyle();
		}
	}

	private static Color randomColor() {
		return new Color(RandomUtils.nextInt(255), RandomUtils.nextInt(255), RandomUtils.nextInt(255));
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println(String.format("%s <input-file> <output-file>", SvnGraph.class.getSimpleName()));
			System.exit(1);
		}

		final File input = new File(args[0]);

		if (!input.exists()) {
			throw new IllegalArgumentException(String.format("The given file '%s' doesn't exist", input.getAbsolutePath()));
		}

		final File output = new File(args[1]);

		final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);

		final History history = new History(document);

		final Set<String> rootPaths = history.getRootPaths();

		System.out.println(rootPaths);

		for (String path : rootPaths) {
			System.out.println(path);
			System.out.println(history.getHistory(path).getRevisions());
			System.out.println();
		}

		int count = 0;

		final Graph graph = new Graph();

		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(output);

			for (Revision revision : history.getSignificantRevisions()) {
				System.out.println(revision.getNumber() + " - " + revision.getMessage());

				// TODO Render also the deletion of branches
				// there should be only 1 significant update per revision (the one with action ADD)
				for (Update update : revision.getSignificantUpdates()) {
					if (update.isCopy()) {
						// a merge is also considered a copy
						final RevisionPath source = update.getCopySource();

						System.out.println(String.format("  > %s %s from %s@%d", update.getAction(), update.getPath(), source.getPath(), source.getRevision()));

						final String sourceRoot = Utils.getRootName(source.getPath());

						if (sourceRoot == null) {
							// skip the revisions whose associated root is
							// null (happens whether a branch was created
							// outside the 'branches' directory for
							// instance)
							System.err.println(String.format("Skipped an update for revision %d because of a null source root", source.getRevision()));
							continue;
						}

						final RevisionPath sourceRP = new RevisionPath(sourceRoot, source.getRevision());

						// create a node for the source (path, revision)
						Node sourceNode = graph.getNodeByData(sourceRP);

						if (sourceNode == null) {
							sourceNode = graph.addNode(sourceRP);
						}

						// and another for the newly created directory
						final String targetRoot = Utils.getRootName(update.getPath());

						if (targetRoot == null) {
							System.err.println(String.format("Skipped an update for revision %d because of a null target root", revision.getNumber()));
							continue;
						}

						final RevisionPath targetRP = new RevisionPath(targetRoot, revision.getNumber());

						Node targetNode = graph.getNodeByData(targetRP);

						if (targetNode == null) {
							targetNode = graph.addNode(targetRP);
						}

						// create an edge between the 2 nodes
						graph.addEdge(null, sourceNode, targetNode);
					} else {
						System.out.println(String.format("  > %s %s", update.getAction(), update.getPath()));
					}
				}

				System.out.println();

				count++;
			}

			// Dispatch the revisions per corresponding branch
			final Map<String, Set<Long>> revisionsPerBranch = new TreeMap<>();

			for (Node node : graph.getNodes()) {
				final RevisionPath revisionPath = (RevisionPath) node.getData();

				final String branchName = revisionPath.getPath();

				if (!revisionsPerBranch.containsKey(branchName)) {
					revisionsPerBranch.put(branchName, new TreeSet<Long>());
				}

				revisionsPerBranch.get(branchName).add(revisionPath.getRevision());
			}

			// Recreate the missing edges between revisions from a same branch
			for (String branchName : revisionsPerBranch.keySet()) {
				final List<Long> branchRevisions = new ArrayList<>(revisionsPerBranch.get(branchName));

				for (int i = 0; i < branchRevisions.size() - 1; i++) {
					final RevisionPath sourceData = new RevisionPath(branchName, branchRevisions.get(i));
					final RevisionPath targetData = new RevisionPath(branchName, branchRevisions.get(i+1));

					graph.addEdge(null, graph.getNodeByData(sourceData), graph.getNodeByData(targetData));
				}
			}

			graph.toGraphML(fileWriter, new CustomRenderer());

			System.out.println(String.format("Found %d significant revisions", count));
		} finally {
			if (fileWriter != null) {
				fileWriter.close();
			}
		}

		System.out.println("Done");
	}
}
