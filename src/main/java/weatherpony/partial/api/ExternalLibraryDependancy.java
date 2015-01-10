package weatherpony.partial.api;

public final class ExternalLibraryDependancy{
	public ExternalLibraryDependancy(String name, String version, String downloadURL){
		this.name = name;
		this.version = version;
		this.downloadURL = downloadURL;
	}
	public final String name;
	public final String version;
	public final String downloadURL;
}
