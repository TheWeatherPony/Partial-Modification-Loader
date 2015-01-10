package weatherpony.util.launchassist;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import com.google.common.base.Throwables;

/**
 * See LoadLastTweakSetup for "How to use" help.
 * 
 * This may seem incredibly stupid. However, it is important for transformers that must run after all others, 
 * no matter the cost. In a perfect world, this wouldn't be needed. However, this not a perfect world, and this, 
 * or something similar is needed, such as another design for the Launching code. As we cannot reorder the class 
 * transformers, and cannot blindly trust every other transformer to not mess with important already-manipulated 
 * stuff afterwards in a negatively impacting way, transformers that operate on a very high level of abstraction, 
 * and thus work best last or nearly-last, may not work at all if done earlier. This was made from the almost 
 * (but not completely) pointless need for late-registration. There <strong>is</strong> a built in limit to 
 * how long this will try to delay.</br>
 * </br>
 * I wrote this code for use with my new loader, PML. The idea behind it is that it dynamically makes an API by injecting 
 * small bits of code into methods. To work best, it should be the last to run on the impacted methods. Since, it's 
 * a dynamically made API, I don't know which methods will be acted on until runtime. Thus, it is easiest to ensure 
 * that it's class transformer is the last run on the influenced methods by ensuring it is the last to run overall.</br>
 * </br>
 * This code was already called "pathological" before I even thought of the idea. My philosophy is that code 
 * that works is better than code that doesn't. This extends to mean that needed code that works, but in a 
 * typically unacceptable way, is better than a lack of needed code, due to no commonly accepted way. I could 
 * either have made this code, or I could have written several wrappers/wrapper-wrappers. For the sake of 
 * compatibility with launchers, I have chosen this route.</br>
 * Don't bother complaining about how bad the concept of this is. I adapted to the situation. I also coded the limit 
 * before even starting on the rest of the code, to ensure it wouldn't be forgotten.
 * 
 *  @author The_WeatherPony
 */
public class LoadLastTweaker implements ITweaker{
	
	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile){
		return;
	}
	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader){
		//LoadLastTweakSetup.onDelay();//this is in charge of everything, which helps keep IO and RAM usage as low as possible.
		try {
			((Callable<Void>)Launch.blackboard.get("LoadLastTweakSetup")).call();
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}
	@Override
	public String getLaunchTarget(){
		return null;//won't be used, anyways.
	}
	@Override
	public String[] getLaunchArguments(){
		return new String[0];
	}
}
