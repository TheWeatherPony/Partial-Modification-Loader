package weatherpony.partial.asmedit;

import net.minecraft.launchwrapper.LaunchClassLoader;
import weatherpony.partial.PMLSecurityManager;

public class LaunchLoaderCallCheck{
	private static final Class loader = LaunchClassLoader.class;
	public static boolean fromLaunchLoader(){
		return loader == PMLSecurityManager.getStackClass(2);
	}
}
