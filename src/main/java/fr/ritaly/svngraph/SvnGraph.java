package fr.ritaly.svngraph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fr.ritaly.graphml4j.EdgeStyle;
import fr.ritaly.graphml4j.GraphMLWriter;
import fr.ritaly.graphml4j.NodeStyle;

public class SvnGraph {

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

			final NodeStyle nodeStyle = graphWriter.getNodeStyle();
			nodeStyle.setWidth(250.0f);

			graphWriter.setNodeStyle(nodeStyle);
			graphWriter.graph();

			final Map<String, String> nodeIdsPerLabel = new TreeMap<>();

			for (Revision revision : revisions) {
				if (revision.isSignificant()) {
					System.out.println(revision.getNumber() + " - " + revision.getMessage());

					// TODO Render also the deletion of branches
					// there should be only 1 significant update per revision (the one with action ADD)
					for (Update update : revision.getSignificantUpdates()) {
						if (update.isCopy()) {
							final RevisionPath source = update.getCopySource();

							System.out.println(String.format("  > %s %s from %s@%d", update.getAction(), update.getPath(), source.getPath(), source.getRevision()));

							final String sourceLabel = Utils.getRootName(source.getPath()) + "@" + source.getRevision();

							// create a node for the source (path, revision)
							final String sourceId;

							if (nodeIdsPerLabel.containsKey(sourceLabel)) {
								// retrieve the id of the existing node
								sourceId = nodeIdsPerLabel.get(sourceLabel);
							} else {
								// create the new node
								sourceId = graphWriter.node(sourceLabel);

								nodeIdsPerLabel.put(sourceLabel, sourceId);
							}

							// and another for the newly created directory
							final String targetLabel = Utils.getRootName(update.getPath());

							final String targetId = graphWriter.node(targetLabel);

							nodeIdsPerLabel.put(targetId, targetLabel);

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
