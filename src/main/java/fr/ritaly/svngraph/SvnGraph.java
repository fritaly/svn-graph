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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fr.ritaly.graphml4j.GraphMLWriter;
import fr.ritaly.graphml4j.NodeStyle;

public class SvnGraph {

	private static Color randomColor() {
		return new Color(RandomUtils.nextInt(255), RandomUtils.nextInt(255), RandomUtils.nextInt(255));
	}

	private static String computeNodeLabel(String path, long revision) {
		final String label = String.format("%s@%d", path, revision);

		// TODO Remove this hard-coded value
		return label.replace("CALYPSO-GRADLE-PLUGINS_", "");
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

		final XPath xpath = XPathFactory.newInstance().newXPath();

		NodeList nodes = (NodeList) xpath.evaluate("/log/logentry", document.getDocumentElement(), XPathConstants.NODESET);

		final List<Revision> revisions = new ArrayList<>();

		for (int i = 0; i < nodes.getLength(); i++) {
			revisions.add(new Revision((Element) nodes.item(i)));
		}

		System.out.println(String.format("Parsed %d revisions", revisions.size()));

		int count = 0;

		FileWriter fileWriter = null;
		GraphMLWriter graphWriter = null;

		try {
			fileWriter = new FileWriter(output);

			graphWriter = new GraphMLWriter(fileWriter);

			final NodeStyle tagStyle = graphWriter.getNodeStyle();
			tagStyle.setFillColor(Color.WHITE);

			graphWriter.graph();

			// map associating node labels to their corresponding node id in the graph
			final Map<String, String> nodeIdsPerLabel = new TreeMap<>();

			// the node style associated to each branch
			final Map<String, NodeStyle> nodeStyles = new TreeMap<>();

			for (Revision revision : revisions) {
				if (revision.isSignificant()) {
					System.out.println(revision.getNumber() + " - " + revision.getMessage());

					// TODO Render also the deletion of branches
					// there should be only 1 significant update per revision (the one with action ADD)
					for (Update update : revision.getSignificantUpdates()) {
						if (update.isCopy()) {
							final RevisionPath source = update.getCopySource();

							System.out.println(String.format("  > %s %s from %s@%d", update.getAction(), update.getPath(), source.getPath(), source.getRevision()));

							final String sourceRoot = Utils.getRootName(source.getPath());

							if (sourceRoot == null) {
								// skip the revisions whose associated root is
								// null (happens whether a branch was created
								// outside the 'branches' directory for
								// instance)
								System.err.println(String.format("Skipped revision %d because of a null root", source.getRevision()));
								continue;
							}

							final String sourceLabel = computeNodeLabel(sourceRoot, source.getRevision());

							// create a node for the source (path, revision)
							final String sourceId;

							if (nodeIdsPerLabel.containsKey(sourceLabel)) {
								// retrieve the id of the existing node
								sourceId = nodeIdsPerLabel.get(sourceLabel);
							} else {
								// create the new node
								if (Utils.isTagPath(source.getPath())) {
									graphWriter.setNodeStyle(tagStyle);
								} else {
									if (!nodeStyles.containsKey(sourceRoot)) {
										final NodeStyle style = new NodeStyle();
										style.setFillColor(randomColor());

										nodeStyles.put(sourceRoot, style);
									}

									graphWriter.setNodeStyle(nodeStyles.get(sourceRoot));
								}

								sourceId = graphWriter.node(sourceLabel);

								nodeIdsPerLabel.put(sourceLabel, sourceId);
							}

							// and another for the newly created directory
							final String targetRoot = Utils.getRootName(update.getPath());

							if (targetRoot == null) {
								System.err.println(String.format("Skipped revision %d because of a null root", revision.getNumber()));
								continue;
							}

							final String targetLabel = computeNodeLabel(targetRoot, revision.getNumber());

							if (Utils.isTagPath(update.getPath())) {
								graphWriter.setNodeStyle(tagStyle);
							} else {
								if (!nodeStyles.containsKey(targetRoot)) {
									final NodeStyle style = new NodeStyle();
									style.setFillColor(randomColor());

									nodeStyles.put(targetRoot, style);
								}

								graphWriter.setNodeStyle(nodeStyles.get(targetRoot));
							}

							final String targetId;

							if (nodeIdsPerLabel.containsKey(targetLabel)) {
								// retrieve the id of the existing node
								targetId = nodeIdsPerLabel.get(targetLabel);
							} else {
								// create the new node
								if (Utils.isTagPath(update.getPath())) {
									graphWriter.setNodeStyle(tagStyle);
								} else {
									if (!nodeStyles.containsKey(targetRoot)) {
										final NodeStyle style = new NodeStyle();
										style.setFillColor(randomColor());

										nodeStyles.put(targetRoot, style);
									}

									graphWriter.setNodeStyle(nodeStyles.get(targetRoot));
								}

								targetId = graphWriter.node(targetLabel);

								nodeIdsPerLabel.put(targetLabel, targetId);
							}

							// create an edge between the 2 nodes
							graphWriter.edge(sourceId, targetId);
						} else {
							System.out.println(String.format("  > %s %s", update.getAction(), update.getPath()));
						}
					}

					System.out.println();

					count++;
				}
			}

			// Dispatch the revisions per corresponding branch
			final Map<String, Set<Long>> revisionsPerBranch = new TreeMap<>();

			for (String nodeLabel : nodeIdsPerLabel.keySet()) {
				if (nodeLabel.contains("@")) {
					final String branchName = StringUtils.substringBefore(nodeLabel, "@");
					final long revision = Long.parseLong(StringUtils.substringAfter(nodeLabel, "@"));

					if (!revisionsPerBranch.containsKey(branchName)) {
						revisionsPerBranch.put(branchName, new TreeSet<Long>());
					}

					revisionsPerBranch.get(branchName).add(revision);
				} else {
					throw new IllegalStateException(nodeLabel);
				}
			}

			// Recreate the missing edges between revisions from a same branch
			for (String branchName : revisionsPerBranch.keySet()) {
				final List<Long> branchRevisions = new ArrayList<>(revisionsPerBranch.get(branchName));

				for (int i = 0; i < branchRevisions.size() - 1; i++) {
					final String nodeLabel1 = String.format("%s@%d", branchName, branchRevisions.get(i));
					final String nodeLabel2 = String.format("%s@%d", branchName, branchRevisions.get(i+1));

					graphWriter.edge(nodeIdsPerLabel.get(nodeLabel1), nodeIdsPerLabel.get(nodeLabel2));
				}
			}

			graphWriter.closeGraph();

			System.out.println(String.format("Found %d significant revisions", count));
		} finally {
			if (graphWriter != null) {
				graphWriter.close();
			}
			if (fileWriter != null) {
				fileWriter.close();
			}
		}

		System.out.println("Done");
	}
}
