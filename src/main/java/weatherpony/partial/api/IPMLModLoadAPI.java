package weatherpony.partial.api;


public interface IPMLModLoadAPI extends IPMLLoadAPI{
	//PML obfuscation registration
	public IObfuscationRegistrar getObfuscationRegistrar();
	//PML obfuscation help for reflection
	public IObfuscationHelper getObfuscationHelper();
	//PML dynamic hook registration
	public IHookRegistrar getHookRegistrar();
	//PML sub-class method override registration
}
