package fr.ritaly.svngraph;

public final class RevisionPath {

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
}