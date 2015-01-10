package weatherpony.partial.launch;

import java.util.List;

public class PMLTweak_FMLHelper {
	private static boolean FML_Found = false;
	private static List<Object> FML_CoreMods = null;//this would be a List<FMLPluginWrapper>, but I don't want to require FML to compile.
	static List<Object> lookForCoreModsAndFMLInGeneral(){
		try{
			Class coremodmanager = Class.forName("cpw.mods.fml.relauncher.CoreModManager");
			if(coremodmanager == null){
				return null;
			}
			FML_Found =true;
			//coremodmanager
			//FML_CoreMods = (List<Object>) coremodmanager.getField("loadPlugins").get(null);
		}catch(Exception e){
			return null;
		}
		return null;
	}
}
