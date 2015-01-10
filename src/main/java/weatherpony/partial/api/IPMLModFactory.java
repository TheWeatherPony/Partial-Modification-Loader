package weatherpony.partial.api;

import java.io.File;

import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * This is the starting point for mods using my loader. This class is responsible for creating the mod objects themselves, technically allowing for self-updating mods.
 * @author The_WeatherPony
 */
public abstract class IPMLModFactory{
	public abstract void preinit(IPMLFactoryLoadAPI loadAPI);
	public ExternalLibraryDependancy[] externalDependancies(){
		return null;
	}
	public abstract File[] loadModFrom(File loadedThisFrom);//this lets the mod load from another file, allowing for self-updating mods, if the main part of the mod is in another zip
	public abstract String[] noASMOn_prefixs();
	public abstract IPMLMod[] loadMod();
}
