package weatherpony.partial.internal;

import java.lang.invoke.MethodHandles.Lookup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Type;

import weatherpony.partial.CallData;
import weatherpony.partial.CallWrapper;
import weatherpony.partial.HookListenerHelperPool;
import weatherpony.partial.hook.HookClassHelper;
import weatherpony.partial.internal.ObfuscationHelper3.InstanceData;
import weatherpony.partial.internal.ObfuscationHelper3.MethodData;
import weatherpony.util.lists.CombinationIterator;
import weatherpony.util.structuring.MultiPathEnum_Plus;
import weatherpony.util.structuring.MultiPathMapPlus;

public class GeneralHookManager{
	public GeneralHookManager(ObfuscationHelper3 obfhelp){
		INSTANCE = this;
		this.obfhelp = obfhelp;
	}
	protected static GeneralHookManager INSTANCE;
	private ObfuscationHelper3 obfhelp;
	public static boolean ASM_prep(String inClass, String method, String desc){
		return INSTANCE._ASM_prep_(inClass, method, desc);
	}
	private boolean _ASM_prep_(String inClass, String method, String desc){
		currentClass = inClass;
		this.inClass = inClass;this.method = method;this.desc = desc;
		List<ModHook> newhooklist = condense(getHookList(inClass, method, desc));
		if(!newhooklist.isEmpty()){
			readies.put(inClass+method+desc, newhooklist);
			return true;
		}
		return false;
	}
	public static String inClass, method, desc;
	private String currentClass;
	private HashMap<String,List<ModHook>> readies = new HashMap();
	public static void ASM_giveProxy(String inClass, String method, String desc, CallWrapper proxy){
		INSTANCE._ASM_giveProxy_(inClass, method, desc, proxy);
	}
	private void _ASM_giveProxy_(String inClass, String method, String desc, CallWrapper proxy){
		/*if(!currentClass.equals(inClass))
			throw new RuntimeException("expected "+currentClass+ " but got "+inClass);*/
		HookClassHelper genInner = generated.get(inClass);
		if(genInner == null){
			generated.put(inClass, genInner = new HookClassHelper(inClass));
		}
		String sec = secondKey(method, desc);
		genInner.put(sec,readies.remove(inClass+method+desc), proxy);
	}
	
	HashMap<String, Boolean> mods = new HashMap();
	public boolean isModActivated(String mod){
		Boolean ret = this.mods.get(mod);
		return ret == null ? false : ret.booleanValue();
	}
	public static void addMod(String mod, boolean active){
		INSTANCE.mods.put(mod, active);
	}
	public void reactivateMod(String mod){
		mods.put(mod, true);
	}
	public void deactivateMod(String mod){
		mods.put(mod, false);
	}
	
	// this is currently public so that a mod maker can install components himself, if he knows what he's doing. 
	// PLEASE DON'T BE STUPID WITH THIS! I'll change this, if I need to.
	// all hooks need to be registered before being first-use, otherwise it will be too late.
	// (I'll most likely change this design in the future, but it works as a temporary solution.)
	public MultiPathMapPlus<String, MultiPathMapPlus<String, MultiPathMapPlus<String, HookSegmentList>>> hooks = new MultiPathMapPlus();
	private List<HookSegmentList> getHookList(String fromClass, String method, String desc){
		Collection<HookSegmentList> ret = new HashSet();
		/*
		Iterator<MultiPathMapPlus<String, MultiPathMapPlus<String, HookSegmentList>>> i1 = hooks.get(fromClass).iterator();
		while(i1.hasNext()){
			Iterator<MultiPathMapPlus<String, HookSegmentList>> i2 = i1.next().get(method).iterator();
			while(i2.hasNext()){
				List<HookSegmentList> add = i2.next().get(desc);
				ret.addAll(add);
			}
		}*/
		Collection<String> superClasses = ClassData.INSTANCE.getAllSupers(fromClass);
		MultiPathMapPlus temp = null;
		
		//get the class data
		Collection<MultiPathMapPlus<String, MultiPathMapPlus<String, HookSegmentList>>> build1 = new ArrayList();
		temp = hooks.getPlus();
		if(temp != null)
			build1.add(temp);
		Iterator<String> iter1 = this.obfhelp.classNames.getAllSynonyms(fromClass);//TODO
		while(iter1.hasNext()){
			String fromClass1 = iter1.next();
			temp = hooks.getValue(fromClass1, MultiPathEnum_Plus.General);
			if(temp != null)
				build1.add(temp);
			temp = hooks.getValue(fromClass1, MultiPathEnum_Plus.Direct);
			if(temp != null)
				build1.add(temp);
		}
		Iterator<String> iter2 = superClasses.iterator();
		while(iter2.hasNext()){
			String superClass = iter2.next();
			Iterator<String> iter3 = this.obfhelp.classNames.getAllSynonyms(superClass);
			while(iter3.hasNext()){
				String fromClass1 = iter3.next();
				temp = hooks.getValue(fromClass1, MultiPathEnum_Plus.General);
				if(temp != null)
					build1.add(temp);
				temp = hooks.getValue(fromClass1, MultiPathEnum_Plus.NonDirect);
				if(temp != null)
					build1.add(temp);
			}
		}
		temp = null;
		//class data obtained
		if(build1.isEmpty()){//no possible chance for any hooks
			return new ArrayList(0);
		}
		//now for method data
		
		//first rough method data to narrow down the search
		Collection<MultiPathMapPlus<String, HookSegmentList>> build2 = new HashSet();
		MethodData mdat = this.obfhelp.getDenoter().denoteMethod(fromClass, method, desc);
		Collection<MethodData> mdats = this.obfhelp.methods.get(mdat);
		Collection<String> methodNames = new ArrayList();
		for(MethodData eachmethod1 : mdats){//TODO - optimize this. perhaps with multithreading?
			String methodName = eachmethod1.methodName;
			methodNames.add(methodName);
			/*for(MethodData eachmethod2 : mdats){//for mixed obfuscations
				String classNameFromMethod = eachmethod2.className;
				classNameFromMethod = ObfuscationHelper3.unprepareClassName2(classNameFromMethod);//remove the 'L' and ';'
				methodNames.add(classNameFromMethod + '.' + methodName);
			}*///this just wastes CPU, as nothing gets put in these spots
		}
		for(MultiPathMapPlus<String, MultiPathMapPlus<String, HookSegmentList>> eachClass : build1){
			temp = eachClass.getPlus();
			if(temp != null)
				build2.add(temp);
			for(String eachmn : methodNames){
				temp = eachClass.getValue(eachmn, MultiPathEnum_Plus.General);
				if(temp != null)
					build2.add(temp);
			}
		}
		//rough method data obtained
		
		if(build2.isEmpty()){//no possible chance for any hooks
			return new ArrayList(0);
		}
		//get fine method data
		
		//set up mixed obfuscation searching
		Type[] paramsT = Type.getArgumentTypes(desc);
		Type methodRetT = Type.getReturnType(desc);
		final int paramSize = paramsT.length;
		List<InstanceData> params = new ArrayList(paramSize);
		List<List<String>> paramClasses = new ArrayList(paramSize);
		for(int cur=0;cur<paramSize;cur++){
			InstanceData data = new InstanceData(paramsT[cur]);
			params.add(data);
			paramClasses.add(this.obfhelp.classNames.get(ObfuscationHelper3.unprepareClassName2(data.className)));
		}
		InstanceData methodRet = new InstanceData(methodRetT);
		Iterator<List<String>> paramiter = new CombinationIterator(paramClasses, true);
		Collection<String> methodRetOb = this.obfhelp.classNames.get(ObfuscationHelper3.unprepareClassName2(methodRet.className));
		for(MultiPathMapPlus<String, HookSegmentList> eachMethod : build2){
			HookSegmentList temp2 = eachMethod.getPlus();
			if(temp2 != null)
				ret.add(temp2);
			temp2 = eachMethod.getValue("");
			if(temp2 != null)
				ret.add(temp2);
		}
		while(paramiter.hasNext()){
			List<String> nextParamTest = paramiter.next();
			for(int cur=0;cur<paramSize;cur++){
				params.get(cur).className = ObfuscationHelper3.prepareForASM(nextParamTest.get(cur));
			}
			StringBuilder sbuild = new StringBuilder();
			sbuild.append('(');
			for(InstanceData each : params){
				sbuild.append(each.toString());
			}
			sbuild.append(')');
			String baseMethodDesc = sbuild.toString();
			for(String eachR : methodRetOb){
				methodRet.className = eachR;
				String obfmd = baseMethodDesc + methodRet.toString();
				//set up completed for this test
				
				for(MultiPathMapPlus<String, HookSegmentList> eachMethod : build2){
					HookSegmentList temp2 = eachMethod.getValue(obfmd, MultiPathEnum_Plus.General);
					if(temp2 != null)
						ret.add(temp2);
				}
			}
		}
		return new ArrayList(ret);
	}
	
	private List<ModHook> condense(List<HookSegmentList> list){
		HookSegmentList temp = new HookSegmentList();
		for(HookSegmentList each : list){
			temp.addHooks(each);
		}
		return temp.condense();
	}
	public static void register(String mod, CallWrapper cw){
		//INSTANCE.addMod(mod, true);//this is added seperately
		INSTANCE.addHook(new ModHook(mod, cw));
	}
	public static void register(String mod, List<CallWrapper> cws){
		//INSTANCE.addMod(mod, true);//this is added seperately
		for(CallWrapper each : cws){
			INSTANCE.addHook(new ModHook(mod, each));
		}
	}
	public void addHook(ModHook hook){
		CallData data = hook.call.data;
		String forClass = data.inClass;
		forClass = ObfuscationHelper3.unprepareClassName2(ObfuscationHelper3.prepareClassName2(forClass));//TODO
		MultiPathEnum_Plus pathClass = data.pathClass;
		String method = data.methodName;
		MultiPathEnum_Plus methodPath = data.pathMethod;
		String desc = data.methodDesc;
		MultiPathEnum_Plus descPath = data.pathDesc;
		MultiPathMapPlus<String, MultiPathMapPlus<String, HookSegmentList>> methodLevelHooks;
		if(hooks.containsValueForKey(forClass, pathClass)){
			methodLevelHooks = hooks.getValue(forClass, pathClass);
		}else{
			hooks.setValue(forClass, (methodLevelHooks = new MultiPathMapPlus()),pathClass);
		}
		MultiPathMapPlus<String, HookSegmentList> descLevelHooks;
		if(methodLevelHooks.containsValueForKey(method, methodPath)){
			descLevelHooks = methodLevelHooks.getValue(method, methodPath);
		}else{
			methodLevelHooks.setValue(method, (descLevelHooks = new MultiPathMapPlus()), methodPath);
		}
		HookSegmentList segmentHookList;
		if(methodLevelHooks.containsValueForKey(desc, descPath)){
			segmentHookList = descLevelHooks.getValue(desc, descPath);
		}else{
			descLevelHooks.setValue(desc, (segmentHookList = new HookSegmentList()), descPath);
		}
		segmentHookList.addHook(hook);
	}
	private HashMap<String, HookClassHelper> generated = new HashMap();
	static String secondKey(String method, String desc){
		return method + desc;
	}
	public static <T> T rigAndDistributeCall(String fromClass, String method, String desc, Object[] parameters){
		return INSTANCE.<T>getHookHelperPool(fromClass, method, desc).distribute(parameters);
	}
	public static <T> HookListenerHelperPool<T> getHookHelperPool_static(String inClass, String method, String desc){
		return INSTANCE.getHookHelperPool(inClass, method, desc);
	}
	public <T> HookListenerHelperPool<T> getHookHelperPool(String inClass, String method, String desc){
		String sec = secondKey(method, desc);
		return generated.get(inClass).get(sec);
	}
	public static void _giveClassLookup(String className, Lookup lookup){
		INSTANCE.generated.get(className).giveLookup(lookup);
	}
}
