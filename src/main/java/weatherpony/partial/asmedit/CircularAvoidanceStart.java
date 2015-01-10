package weatherpony.partial.asmedit;

import net.minecraft.launchwrapper.IClassTransformer;

public class CircularAvoidanceStart implements IClassTransformer {
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass){
		CircularAvoidance.start(transformedName);
		return basicClass;
	}
}
