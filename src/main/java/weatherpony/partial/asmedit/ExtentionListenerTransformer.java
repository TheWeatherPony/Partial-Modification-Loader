package weatherpony.partial.asmedit;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.objectweb.asm.ClassReader;

import weatherpony.partial.internal.ClassData;

import com.google.common.base.Throwables;

public class ExtentionListenerTransformer implements IClassTransformer{

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		if(bytes != null && LaunchLoaderCallCheck.fromLaunchLoader()){
			ClassReader cr = new ClassReader(bytes);
			String internalName = cr.getClassName();
			
			if(!internalName.replace('/', '.').equals(transformedName)){
				throw new RuntimeException();
			}
			String extending = cr.getSuperName().replace('/', '.');
			String[] interfaces = cr.getInterfaces();
			if(interfaces == null)
				interfaces = new String[0];
			int interfacesLength = interfaces.length;
			List<String> interfacesTransformed = new ArrayList(interfacesLength);
			for(int cur=0;cur<interfacesLength;cur++){
				interfacesTransformed.add(interfaces[cur].replace('/', '.'));
			}
			this.registerClassInfo(transformedName, extending, interfacesTransformed);
			
			//now to go through the parents' info. This will generate the full tree for the current class.
			//while(true){
				try{
					
					Class extendingClass = Class.forName(extending, false, Launch.classLoader);
					
					for(String eachI : interfacesTransformed){
						Class.forName(eachI, false, Launch.classLoader);
					}
				}catch(Exception e){
					//well... what should i do?
					throw Throwables.propagate(e);
				}
			//}
		}
		return bytes;
	}
	
	void registerClassInfo(String forClass, String Extends, List<String> interfaces){
		ClassData.ASM_giveExtendsAndImplements(forClass, Extends, interfaces);
	}
}
