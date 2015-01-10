package weatherpony.partial.launch.tweak;

import java.io.File;
import java.net.URLClassLoader;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import com.google.common.base.Throwables;

public class PMLTweaker implements ITweaker {
	public PMLTweaker(){
		System.out.println("PMLTweaker being initialized");
		Launch.blackboard.put("ParentClassLoader", (/*AppClassLoader extends*/URLClassLoader) getClass().getClassLoader());
		try{
			ITweaker actuallyusefulTweak = (ITweaker) Class.forName("weatherpony.partial.launch.PMLActualTweak", true, Launch.classLoader).newInstance();
			//I'm needing to have my main initializer load from the LaunchClassLoader. An AppClassLoader, which is how this Tweak was loaded, is useless for my mod loading.
			((List<ITweaker>)(Launch.blackboard.get("Tweaks"))).add(actuallyusefulTweak);
			//I could just use Class.forName(String,boolean,ClassLoader), but this way works well enough for now.
		}catch(Exception e){
			throw Throwables.propagate(e);
		}
		System.out.println("PMLTweaker added PMLActualTweak");
	}
	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile){
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
	}
}
