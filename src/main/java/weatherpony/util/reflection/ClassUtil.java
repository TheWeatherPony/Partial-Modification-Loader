package weatherpony.util.reflection;

import net.minecraft.launchwrapper.Launch;

import org.objectweb.asm.Type;


public class ClassUtil{
	public static Class forName(String name) throws ClassNotFoundException{
		if(name.length() > 1){
			return Class.forName(name, false, Launch.classLoader);
		}
		char c = name.charAt(0);
		switch(c){//this might not be needed, but it's good anyways.
		case 'V':
            return Void.TYPE;
        case 'Z':
            return Boolean.TYPE;
        case 'C':
            return Character.TYPE;
        case 'B':
            return Byte.TYPE;
        case 'S':
            return Short.TYPE;
        case 'I':
            return Integer.TYPE;
        case 'F':
            return Float.TYPE;
        case 'J':
            return Long.TYPE;
        case 'D':
            return Double.TYPE;
    	default:
    		throw new RuntimeException();
		}
	}
	public static Class forArray(Class base, int dimentions, boolean add) throws ClassNotFoundException{
		Type t = Type.getType(base);
		if(!add){
			if(t.getSort() == Type.ARRAY)
				t = t.getElementType();
		}
		String temp = "";
		for(int cur=0;cur<dimentions;cur++){
			temp+='[';
		}
		temp += t.getInternalName();
		
		return ClassUtil.forName(temp.replace('/', '.'));
	}
}
