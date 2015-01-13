package weatherpony.partial.asmedit;

import net.minecraft.launchwrapper.IClassTransformer;

public class CircularAvoidanceEnd implements IClassTransformer {
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass){
		if(LaunchLoaderCallCheck.fromLaunchLoader())
			CircularAvoidance.stop(transformedName);
		return basicClass;
	}
}
