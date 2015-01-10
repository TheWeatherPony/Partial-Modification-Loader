package weatherpony.partial.launch.ext;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

//To use this in a Dev. enviornment, add the following line to end of the Client and Server run configurations, (in the JVM arguments). This will make FML load this coremod. You also need to make a file "pml.info" with two lines: the first with the fully-qualified name of your IPMLModFactory, the second blank.
//-Dfml.coreMods.load= weatherpony.partial.launch.PML_FMLLoadPlugin
//If you have two mods needing this, put the two class names together, separated by a comma (',')
public class PML_FMLLoadPlugin implements IFMLLoadingPlugin {
	public PML_FMLLoadPlugin(){
		System.out.println("loading PML");
	}
	@Override
	public String[] getASMTransformerClass() {
		//in Forge, this gets called before the coremod knows anything that's going on, which is the reverse of the launcher
		return null;
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return "weatherpony.partial.launch.ext.PML_FMLCallHook";
	}
	
	@Override
	public void injectData(Map<String, Object> data) {
		boolean deobfuscatedEnvironment = ((Boolean)(data.get("runtimeDeobfuscationEnabled"))).booleanValue();
		//I might do something with this later on
	}
	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
