package fr.ritaly.svngraph;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SvnGraph {

	private static boolean isBranchPath(String string) {
		return string.matches(".+/branches/[^/]+$");
	}

	private static boolean isTagPath(String string) {
		return string.matches(".+/tags/[^/]+$");
	}

	private static boolean isTrunkPath(String string) {
		return string.matches(".+/trunk$");
	}

	private static boolean isInternalPath(String string) {
		return string.matches(".+/trunk/.+$") || string.matches(".+/(tags|branches)/[^/]+/.+$");
	}

	private static boolean isImportantPath(String string) {
		return isBranchPath(string) || isTagPath(string) || isTrunkPath(string);
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

		for (Revision revision : revisions) {
			if (revision.isSignificant()) {
				System.out.println(revision.getNumber() + " - " + revision.getMessage());

				for (Update update : revision.getSignificantUpdates()) {
					if (update.isCopy()) {
						final RevisionPath source = update.getCopySource();

						System.out.println(String.format("  > %s %s from %s@%d", update.getAction(), update.getPath(), source.getPath(), source.getRevision()));
					} else {
						System.out.println(String.format("  > %s %s", update.getAction(), update.getPath()));
					}
				}

				System.out.println();

				count++;
			}
		}

		System.out.println(String.format("Found %d significant revisions", count));

		System.out.println("Done");

//		// Remove all elements <path> pertaining to file entries
//		NodeList nodes = (NodeList) xpath.evaluate("/log/logentry/paths/path[@kind = 'file']", document.getDocumentElement(),
//				XPathConstants.NODESET);
//
//		for (int i = 0; i < nodes.getLength(); i++) {
//			final Element node = (Element) nodes.item(i);
//
//			node.getParentNode().removeChild(node);
//		}
//
//		// Remove all elements <path> pertaining to directory updates
//		nodes = (NodeList) xpath.evaluate("/log/logentry/paths/path[@kind = 'dir' and @action='M']",
//				document.getDocumentElement(), XPathConstants.NODESET);
//
//		for (int i = 0; i < nodes.getLength(); i++) {
//			final Element node = (Element) nodes.item(i);
//
//			node.getParentNode().removeChild(node);
//		}
//
//		// Remove all path entries pointing to internal directories
//		nodes = (NodeList) xpath.evaluate("/log/logentry/paths/path[@kind = 'dir']", document.getDocumentElement(),
//				XPathConstants.NODESET);
//
//		for (int i = 0; i < nodes.getLength(); i++) {
//			final Element node = (Element) nodes.item(i);
//
//			final String path = node.getTextContent();
//
//			if (isInternalPath(path)) {
//				node.getParentNode().removeChild(node);
//			} else {
//				System.out.println(path);
//			}
//		}
//
//		// Remove all <logentry> elements whose <paths> child element has no
//		// child
//		nodes = (NodeList) xpath.evaluate("/log/logentry/paths[not(child::*)]/..", document.getDocumentElement(),
//				XPathConstants.NODESET);
//
//		for (int i = 0; i < nodes.getLength(); i++) {
//			final Element node = (Element) nodes.item(i);
//
//			node.getParentNode().removeChild(node);
//		}
//
//		final DOMSource domSource = new DOMSource(document.getDocumentElement());
//		final StreamResult outputResult = new StreamResult(output);
//
//		TransformerFactory.newInstance().newTransformer().transform(domSource, outputResult);
//
//		System.out.println("Done");
	}
}
