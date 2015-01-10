package weatherpony.partial.api;

//To use this loader in a Dev. enviornment, add the following line to end of the Client and Server run configurations, (in the JVM arguments). This will make FML load this coremod. You also need to make a file "pml.info" with two lines: the first with the fully-qualified name of your IPMLModFactory, the second blank.
//-Dfml.coreMods.load="weatherpony.partial.launch.PML_FMLLoadPlugin"
//If you have two mods needing this, put the two class names together, separated by a comma (',')
//You will also need to be using Java 7 (or higher)
public interface IPMLLoadAPI{
	//PML API version number. Make sure this is what you're expecting before doing anything else. This is different for the factory and mod load APIs, but uses this same method
	public int apiVersionNumber();
	//PML API version compatibility check. This is for if the above isn't what you're expecting. You could also just check here first.
	public boolean isDirectlyCompatible(int otherAPINumber);
	
	//PML version information. You shouldn't need to care about this, but it's here just in case.
	public int majorVersionNumber();
	public int minorVersionNumber();
	
	//Minecraft version information
	public String minecraftVersionNumber();//the full version name (#.# or #.#.#)
	public String minecraftCoreVersionNumber();//the first two parts of the version name (#.#). This may be the same thing as the full version
}
