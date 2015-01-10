package weatherpony.partial.hook;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.HashMap;
import java.util.List;

import weatherpony.partial.CallWrapper;
import weatherpony.partial.HookListenerHelperPool;
import weatherpony.partial.PMLSecurityManager;
import weatherpony.partial.api.MethodHandleFetchData;
import weatherpony.partial.internal.ModHook;

public final class HookClassHelper{
	private Lookup myLookup;
	private Class self;
	private final String aboutClass;
	
	private final HashMap<String, HookListenerHelperPool> hooks;
	public HookClassHelper(String forClass){
		this.hooks = new HashMap();
		this.aboutClass = forClass;
	}
	public HookListenerHelperPool get(String tag){
		return hooks.get(tag);
	}
	public void giveLookup(Lookup lookup){
		final Class<?> caller = PMLSecurityManager.getStackClass(3);
		final Class<?> lookedup = lookup.lookupClass();
		if(!caller.equals(lookedup))
			throw new IllegalStateException();
		
		final String name = caller.getName();
		if(!this.aboutClass.equals(name))
			throw new IllegalStateException();
		
		if(this.myLookup != null)
				throw new IllegalStateException();
		
		this.myLookup = lookup;
		this.self = caller;
	}
	public void put(String sec, List<ModHook> remove, CallWrapper proxy){
		final HookListenerHelperPool pool = new HookListenerHelperPool(this, remove, proxy);
		this.hooks.put(sec, pool);
	}
	private HashMap<MethodHandleFetchData, MethodHandle> lookedUpSupers = new HashMap(0);
	public MethodHandle getMethodHandleForSuper(MethodHandleFetchData method) throws NoSuchMethodException, IllegalAccessException{
		if(!this.lookedUpSupers.containsKey(method))
			this.lookedUpSupers.put(method, this.getNewMethodHandleForSuper(method));
		return this.lookedUpSupers.get(method);
	}
	private MethodHandle getNewMethodHandleForSuper(MethodHandleFetchData method) throws NoSuchMethodException, IllegalAccessException{
		return this.myLookup.findSpecial(method.getOriginal(), method.getName(), method.getMethodType(), self);
	}
	
}
