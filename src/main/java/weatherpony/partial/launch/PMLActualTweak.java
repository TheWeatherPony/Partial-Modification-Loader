package weatherpony.partial.launch;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import cpw.mods.fml.common.Loader;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import weatherpony.partial.CallData;
import weatherpony.partial.HookListenerHelper;
import weatherpony.partial.HookRegistration;
import weatherpony.partial.ICallListener;
import weatherpony.partial.api.IHookRegistrar;
import weatherpony.partial.api.IObfuscationHelper;
import weatherpony.partial.api.IObfuscationRegistrar;
import weatherpony.partial.api.IPMLFactoryLoadAPI;
import weatherpony.partial.api.IPMLModLoadAPI;
import weatherpony.partial.internal.ClassData;
import weatherpony.partial.internal.GeneralHookManager;
import weatherpony.partial.internal.ObfuscationHelper3;
import weatherpony.partial.internal.OverridingManager;
import weatherpony.partial.modloading.PMLModManager;
import weatherpony.util.launchassist.LoadLastTweakSetup;
import weatherpony.util.structuring.MultiPathEnum_Plus;

public class PMLActualTweak implements ITweaker{
	public static final int PML_majorVersion = 4;
	public static final int PML_minorVersion = 16;
	
	
	public static PMLActualTweak instance;
	public static GeneralHookManager hookmanager;
	public static String MCVersion;
	public static String MCVersionShort;
	public PMLActualTweak(){
		System.out.println("PMLActualTweak being initialized");
		instance = this;
		
		classdata = new ClassData();
		overridingmanager = new OverridingManager();
		ObfuscationHelper3 obfhelper = new ObfuscationHelper3();
		this.obfhelp = obfhelper;
		this.obfreg = obfhelper;
		this.hookmanager = new GeneralHookManager(obfhelper);
		this.hookRegistrar = new HookRegistration();
	}
	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile){
		System.out.println("PMLTweaker accepting options");
		//FML sorts the tweaks when this gets called. I need to add my code after it, so I start after it's done
	}

	@Override
	public String[] getLaunchArguments() {
		return new String[0];
	}

	@Override
	public String getLaunchTarget() {
		return "net.minecraft.client.main.Main";
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classloader){
		System.out.println("PMLActualTweak injecting into ClassLoader");
		classloader.addTransformerExclusion("weatherpony.partial.");
		classloader.addTransformerExclusion("weatherpony.util.");
		
		//setup is only called by the mod that owns it. other mods use it, but don't own it.
		LoadLastTweakSetup.setup(null);//using null because it doesn't matter. changed requirements part-way, and didn't finish cleaning up :P
		
		LoadLastTweakSetup last = LoadLastTweakSetup.get(classloader);
		last.lasts.add("weatherpony.partial.asmedit.CircularAvoidanceStart");
		last.lasts.add("weatherpony.partial.asmedit.ExtentionListenerTransformer");//learn about class hierarchy
		
		
		VersionHelper versionhelper = new VersionHelper();
		this.MCVersion = versionhelper.MCVersion;
		this.MCVersionShort = versionhelper.MCVersionRoot;
		
		IPMLModLoadAPI modAPI = new ModLoadAPIImplementation(this.PML_majorVersion, this.PML_minorVersion, this.MCVersion, this.MCVersionShort, this.obfreg, this.obfhelp, this.hookRegistrar);
		IPMLFactoryLoadAPI factoryAPI = new FactoryLoadAPIImplementation(this.PML_majorVersion, this.PML_minorVersion, this.MCVersion, this.MCVersionShort, modAPI);
		modmanager = new PMLModManager(factoryAPI, modAPI);
		
		this.assertBranding();
		
		
		last.lasts.add("weatherpony.partial.asmedit.MethodInjectionAndSuperCorrectionTransformer");//combines the below 3, but makes them actually work
		/*
		last.lasts.add("weatherpony.partial.asmedit.OverrideInjectorTransformer");//add methods pointing to a fake super methods
		last.lasts.add("weatherpony.partial.asmedit.MethodFinderTransformer");//learn about method hierarchy
		last.lasts.add("weatherpony.partial.asmedit.SuperMethodCorrectorTransformer");//fix calls to super methods
		*/
		
		last.lasts.add("weatherpony.partial.asmedit.HookInjectorTransformer");//add hooks
		
		last.lasts.add("weatherpony.partial.asmedit.CircularAvoidanceEnd");
		last.start();
	}
	
	private void assertBranding(){
		ICallListener<String> vanilla = new VanillaBrandingAssertion();
		this.hookRegistrar.register("PML", 
				new CallData.CallDataFactory()
					.setClass("net.minecraft.server.MinecraftServer", MultiPathEnum_Plus.General)
					.setMethodName("getServerModName")
					.create(),
				vanilla);
		this.hookRegistrar.register("PML", 
				new CallData.CallDataFactory()
					.setClass("net.minecraft.client.ClientBrandRetriever", MultiPathEnum_Plus.General)
					.setMethodName("getClientModName")
					.create(),
				vanilla);
		GeneralHookManager.addMod("PML", true);
		//TODO - Forge branding assertion is added by a dumby FML mod. This mod also adds PML to the FML mod list
	}
	private class VanillaBrandingAssertion implements ICallListener<String>{
		@Override
		public String call2(HookListenerHelper<String> hooks) throws Throwable {
			String norm = hooks.callNext();
			if(norm.equals("vanilla"))
				return "PML";
			return norm + ",PML";
		}
	}
	
	public IObfuscationHelper getObfHelper(){
		return this.obfhelp;
	}

	private final IObfuscationRegistrar obfreg;
	private final IObfuscationHelper obfhelp;
	
	private final IHookRegistrar hookRegistrar;
	
	private ClassData classdata;
	private OverridingManager overridingmanager;
	private PMLModManager modmanager;
	private static String obfuscationHelperClass = "weatherpony.partial.obfhelp.ObfConnection"; 
}
