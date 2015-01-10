package weatherpony.partial.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import weatherpony.partial.internal.ClassData.ExtentionData;

public class SuperMethodCorrectionManager{
	protected static final SuperMethodCorrectionManager INSTANCE = new SuperMethodCorrectionManager();;
	private SuperMethodCorrectionManager(){
	}
	public static String _ASM_getLastSeenClass(String startClass, String method, String desc, String bestGuess){
		return INSTANCE.lastFrom(startClass, method, desc, bestGuess);
	}
	
	public static void _ASM_addMethodData(String inClass, String method, String desc){
		INSTANCE.addMethodData(inClass, method, desc);
	}
	private void addMethodData(String inClass, String method, String desc){
		HashSet<MethodData> classMethods = methods.get(inClass);
		if(classMethods == null){
			methods.put(inClass, (classMethods = new HashSet()));
		}
		classMethods.add(new MethodData(method, desc));
	}
	HashMap<String, HashSet<MethodData>> methods = new HashMap();
	static class MethodData{
		MethodData(String method, String desc){
			this.method = method;
			this.desc = desc;
			this.hash = (method+desc).hashCode();
		}
		final String method, desc;
		final int hash;
		@Override
		public int hashCode(){
			return this.hash;
		}
		@Override
		public boolean equals(Object o){
			if(o instanceof MethodData){
				MethodData data = (MethodData)o;
				return this.method.equals(data.method) && this.desc.equals(data.desc);
			}else if(o instanceof CharSequence){
				return (this.method+this.desc).equals(((CharSequence)o).toString());
			}
			return false;
		}
	}
	protected String lastFrom(String basedOnClass, String method, String desc, String normallyGiven){
		HashMap<String, ExtentionData> map = ClassData.INSTANCE.classMap;
		String lastSeen = null;
		String nextTest = basedOnClass;
		boolean foundGiven = false;
		MethodData lookingFor = new MethodData(method, desc);
		if(normallyGiven.equals("java.lang.Object"))
			return normallyGiven;
		while(nextTest != null && !nextTest.equals("java.lang.Object")){
			ExtentionData data = map.get(nextTest);
			if(data == null)
				break;//no data. 
			String extending = data.extend;
			if(extending.equals(normallyGiven))
				foundGiven = true;
			HashSet<MethodData> methods = this.methods.get(extending);
			if(methods != null && methods.contains(lookingFor)){
				lastSeen = extending;
				break;
			}
			nextTest = extending;
		}
		if(lastSeen == null){
			if(foundGiven)
				return null;//we're screwed anyways in this case. :)
			return normallyGiven;//hope for the best.
		}
		return lastSeen;
	}
}
