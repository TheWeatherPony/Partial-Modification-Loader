package weatherpony.partial.api;

import java.util.Collection;
import java.util.List;

import weatherpony.partial.launch.PMLActualTweak;

/**
 * This is the primary mod class used in my loader. While it isn't an interface anymore, it still acts like one, so the 'I' remains in it's name.  
 * @author The_WeatherPony
 */
public abstract class IPMLMod{
	public final String modName;
	protected IPMLMod(String modName){
		this.modName = modName;
	}
	public ExternalLibraryDependancy[] externalDependancies(){
		return null;
	}
	public void givePMLMods(List<IPMLMod> mods){ }
	//public void giveFMLCoreMods(List<Object> mods){ }//not yet ready
	public abstract void init(IPMLModLoadAPI loadAPI);//this init is called before FML's preinit
	public void giveFMLMods(List<Object> mods){ }
	
	//PML Mod communications, just in case you need it. This only provides a means, not a manner. It's up to implementers to work out how to use it.
	protected abstract Object interpretMessage(IPMLMod fromMod, Object message);
	protected final Object sendMessage(IPMLMod toMod, Object message){
		return toMod.interpretMessage(this, message);
	}
}
