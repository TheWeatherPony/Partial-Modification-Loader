package weatherpony.partial.api;

import java.util.List;

import weatherpony.partial.CallData;
import weatherpony.partial.CallWrapper;
import weatherpony.partial.ICallListener;

public interface IHookRegistrar {

	public void register(String mod, CallData data, ICallListener call);

	/**
	 * This is to register calls that are pre-rigged to a CallWrapper. This allows mod makers to connect however they wish.
	 * @param mod - the name of your mod
	 * @param cw - an instance of {@link#CallWrapper}
	 */
	public void register(String mod, CallWrapper cw);

	/**
	 * This is to register calls that are pre-rigged to CallWrappers, en-mass. 
	 * @param mod - the name of your mod
	 * @param cws - a list of {@link#CallWrapper} instances
	 */
	public void register(String mod, List<CallWrapper> cws);
	
	/**
	 * This is to register for PML to override a super method in the given class. The definition of the created method is such that it calls it's super method and returns the result.</br>
	 * While this may not sound very useful, it is intended to be used in combination with the dynamic hook registrations that are also provided in this interface.
	 * @param inClass - the class that the method should be injected
	 * @param method - the name of the method to be overriden
	 * @param desc - the description tag of the method to be overriden
	 * @param expectedToOverrideFrom - your best guess as to what the last class to have this method/method description pair is. It doesn't matter if someone else overrides the method in between - this is just a best guess. If you don't know, then just put where the method comes from (or the first super class it's implemented in, if it's from an interface).
	 * @param originalSource - the place where this method originally comes from. This can be an interface, as it's just used to find the right obfuscations for the method
	 */
	public void registerForSuperOverride(String inClass, String method, String desc, String expectedToOverrideFrom, String originalSource);
}