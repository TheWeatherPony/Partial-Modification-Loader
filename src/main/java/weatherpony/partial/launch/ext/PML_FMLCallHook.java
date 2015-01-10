package weatherpony.partial.launch.ext;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

import com.google.common.base.Throwables;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import cpw.mods.fml.relauncher.IFMLCallHook;

public class PML_FMLCallHook implements IFMLCallHook {

	@Override
	public Void call() throws Exception {
		return null;//seriously... what's the point in IFMLCallHook extending Callable<Void>?
	}

	@Override
	public void injectData(Map<String, Object> paramMap) {
		File myLocation = (File)paramMap.get("coremodLocation");
		URLClassLoader originalLoader = (URLClassLoader)Launch.class.getClassLoader();
		if(myLocation != null){
		try{
			Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
			method.setAccessible(true);
			method.invoke(originalLoader, new Object[]{myLocation.toURI().toURL()});
		}catch(Throwable e){
			throw Throwables.propagate(e);
		}
		}
		LaunchClassLoader classLoader = (LaunchClassLoader) paramMap.get("classLoader");
		
		((List) Launch.blackboard.get("TweakClasses")).add("weatherpony.partial.launch.tweak.PMLTweaker");
	}

}
