package weatherpony.partial.internal;

import weatherpony.partial.CallWrapper;

public class ModHook<Type>{
	public /**/ ModHook(String mod, CallWrapper<Type> call){
		this.modName = mod;
		this.call = call;
	}
	String modName;
	CallWrapper<Type> call;
	boolean notwanted = false;
	public CallWrapper<Type> getIfWarented(){
		if(notwanted)
			return null;
		if(GeneralHookManager.INSTANCE.isModActivated(modName))
			return call;
		return null;
	}
	
}
