package weatherpony.partial.asmedit;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.launchwrapper.IClassTransformer;

public class CircularAvoidance{
	private static Collection<String> classesCurrentlyBeingLoaded = new ArrayList();
	public static void start(String className){
		if(classesCurrentlyBeingLoaded.contains(className))
			throw new ClassCircularityError(className);
		classesCurrentlyBeingLoaded.add(className);
	}
	public static void stop(String className){
		classesCurrentlyBeingLoaded.remove(className);
	}
	public static boolean isLoading(String className){
		return classesCurrentlyBeingLoaded.contains(className);
	}
}
