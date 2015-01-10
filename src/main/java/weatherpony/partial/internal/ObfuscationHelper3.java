package weatherpony.partial.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Type;

import weatherpony.partial.api.IObfuscationHelper;
import weatherpony.partial.api.IObfuscationHelper.ObfuscationException.ExclusiveObfuscationException;
import weatherpony.partial.api.IObfuscationHelper.ObfuscationException.InclusiveObfuscationException;
import weatherpony.partial.api.IObfuscationRegistrar;
import weatherpony.partial.api.IObfuscationRegistrar.IObfuscationDenoter;
import weatherpony.partial.asmedit.CircularAvoidance;
import weatherpony.partial.internal.ObfuscationHelper3.FieldData;
import weatherpony.partial.internal.ObfuscationHelper3.MethodData;
import weatherpony.util.copies.ExpandedSynonymMapper;
import weatherpony.util.lists.CombinationIterator;
import weatherpony.util.reflection.ClassUtil;

public class ObfuscationHelper3 implements IObfuscationHelper, IObfuscationRegistrar<String,FieldData,MethodData>, IObfuscationDenoter<String,FieldData,MethodData>{
	public ExpandedSynonymMapper<String> classNames = new ExpandedSynonymMapper(6000);
	public ExpandedSynonymMapper<FieldData> fields = new ExpandedSynonymMapper(30000);
	public ExpandedSynonymMapper<MethodData> methods = new ExpandedSynonymMapper(30000);
	
	@Override
	public String denoteClass(String className){
		return className;
	}
	@Override
	public FieldData denoteField(String className, String fieldName){
		return new FieldData(className, fieldName);
	}
	@Override
	public MethodData denoteMethod(String className, String methodName, String methodDesc){
		InstanceData returnType = null;
		List<InstanceData> paramlist = null;
		if(methodDesc != null){// not a stub
			returnType = new InstanceData(Type.getReturnType(methodDesc));
			Type[] params = Type.getArgumentTypes(methodDesc);
			final int paramlength = params.length; 
			paramlist = new ArrayList(paramlength);
			for(int cur=0;cur<paramlength;cur++){
				paramlist.add(new InstanceData(params[cur]));
			}
		}
		return new MethodData(className,methodName,returnType,paramlist,true);
	}
	public static String prepareClassName2(String name){
		name = name.replace('/', '.');
		if(name.length() == 1)//primitive
			return name;
		if(!(name.startsWith("L") && name.endsWith(";"))){
			return 'L'+name+';';
		}
		return name;
	}
	public static String unprepareClassName2(String name){
		if(name.length() == 1)//primitive
			return name;
		if(name.startsWith("L") && name.endsWith(";")){
			return name.substring(1, name.length()-1);
		}
		return name;
	}
	public static String prepareForASM(String name){
		return prepareClassName2(name).replace('.', '/');
	}
	public String getClassName(String name) throws ObfuscationException{
		name = prepareClassName2(name);//for consistancy
		Iterator<String> iter = this.classNames.getAllSynonyms(name);
		Collection<String> options = new ArrayList();
		while(iter.hasNext()){
			String testName = iter.next();
			//testName = testName.substring(1, testName.length()-1);//remove the 'L' and ';'
			testName = unprepareClassName2(testName);
			boolean add = CircularAvoidance.isLoading(testName);
			if(!add){
				try{
					Class test = ClassUtil.forName(testName);
					add = true;
				}catch (/*ClassNotFoundException*/Throwable e) {
				//ignore. this is expected, and will happen a lot.
				}
			}
			if(add)
				options.add(testName);
		}
		String ret = null;
		Iterator<String> citer = options.iterator();
		while(citer.hasNext()){
			String next = citer.next();
			if(ret == null){
				ret = next;
				continue;
			}
			if(ret.equals(next))
				continue;
			
			throw new InclusiveObfuscationException();
		}
		if(ret == null)
			throw new ExclusiveObfuscationException();
		return ret;
	}
	@Override
	public Class getClass(String name) throws ObfuscationException{
		name = prepareClassName2(name);//for consistancy
		Iterator<String> iter = this.classNames.getAllSynonyms(name);
		Collection<Class> options = new ArrayList();
		while(iter.hasNext()){
			try {
				String testName = iter.next();
				//testName = testName.substring(1, testName.length()-1);//remove the 'L' and ';'
				testName = unprepareClassName2(testName);
				options.add(ClassUtil.forName(testName));
			} catch (/*ClassNotFoundException*/Throwable e) {
				//ignore. this is expected, and will happen a lot.
			}
		}
		Class ret = null;
		Iterator<Class> citer = options.iterator();
		while(citer.hasNext()){
			Class next = citer.next();
			if(ret == null){
				ret = next;
				continue;
			}
			if(ret.equals(next))
				continue;
			
			throw new InclusiveObfuscationException();
		}
		if(ret == null)
			throw new ExclusiveObfuscationException();
		return ret;
	}

	@Override
	public Field getField(String className, String fieldName) throws ObfuscationException {
		//className = prepareClassName2(className);//for consistancy
		className = className.replace('/', '.');
		FieldData finder = new FieldData(className, fieldName);
		Iterator<String> iter = this.classNames.getAllSynonyms(className);//returns an Iterator with at least one entry
		while(iter.hasNext()){
			finder.className = iter.next();//the first time, it doesn't change anything.
			Collection<Field> options = new ArrayList();
			Iterator<FieldData> fiter = this.fields.getAllSynonyms(finder);
			while(fiter.hasNext()){
				FieldData next = fiter.next();
				String nextClassName = next.className;
				//nextClassName = nextClassName.substring(1, nextClassName.length()-1);
				nextClassName = this.unprepareClassName2(nextClassName);
				try {
					Class test = ClassUtil.forName(nextClassName);
					options.add(test.getDeclaredField(next.fieldName));
				}catch(Throwable e){
					//ignore. this will happen a lot, and isn't a problem... yet.
				}
				/* catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}*/
			}
			Field ret = null;
			Iterator<Field> foundfields = options.iterator();
			while(foundfields.hasNext()){
				Field next = foundfields.next();
				if(ret == null){
					ret = next;
					continue;
				}
				if(ret.equals(next))
					continue;
				
				throw new InclusiveObfuscationException();
			}
			if(ret == null)
				throw new ExclusiveObfuscationException();
			ret.setAccessible(true);
			return ret;
		}
		throw new RuntimeException();//this should never be reached.
	}
	
	@Override
	public Method getMethod(String className, String methodName, String methodDesc) throws ObfuscationException{
		InstanceData returnType = new InstanceData(Type.getReturnType(methodDesc));
		Type[] paramsT = Type.getArgumentTypes(methodDesc);
		final int paramSize = paramsT.length;
		List<InstanceData> params = new ArrayList(paramSize);
		List<List<String>> paramClasses = new ArrayList(paramSize);
		for(int cur=0;cur<paramSize;cur++){
			InstanceData data = new InstanceData(paramsT[cur]);
			params.add(data);
			paramClasses.add(this.classNames.get(unprepareClassName2(data.className)));
		}
		MethodData finder = new MethodData(className,methodName,returnType,params,false);
		//className = prepareClassName2(className);//for consistancy
		Collection<MethodData> initialFinds = new HashSet();
		Collection<String> fromOptions = this.classNames.get(className);
		Collection<String> retOptions = this.classNames.get(unprepareClassName2(finder.returnName.className));
		Iterator<List<String>> paramiter = new CombinationIterator(paramClasses, true);
		while(paramiter.hasNext()){
			List<String> nextParamTest = paramiter.next();
			for(int cur=0;cur<paramSize;cur++){
				params.get(cur).className = prepareForASM(nextParamTest.get(cur));
			}
			
			for(String eachRetOption : retOptions){
				finder.returnName.className = prepareForASM(eachRetOption);
				if(paramSize == 0){
					initialFinds.addAll(this.methods.get(finder.clone()));
				}
				for(String eachFromOption : fromOptions){
					finder.className = eachFromOption;
					
					//the finder is now set up. next to see what it matches
					initialFinds.addAll(this.methods.get(finder.clone()));
				}
			}
		}
		
		//all of the candidates are found. now to test them...
			//sheesh this is long and unoptimized. yet it's *SO* much better than the last version.
		Collection<Method> deepFinds = new HashSet();
		for(String eachFromOption : fromOptions){
			Class foundClass;
			try{
				foundClass = ClassUtil.forName(eachFromOption);
			}catch(Throwable e){
				//this is expected
				continue;
			}
			Iterator<MethodData> iterinitial = initialFinds.iterator();
			while(iterinitial.hasNext()){
				MethodData next = iterinitial.next();
				//Iterator<MethodData> iterin = this.methods.getAllSynonyms(next);
				//while(iterin.hasNext()){
					//next = iterin.next();
					/*if(next.params == null)
						continue;*///this is an info stub used with hook generation
					try {
						if(next.params.size() != paramSize){
							//... uhhh...
							continue;// :S
						}
						
						Class[] mparam = new Class[paramSize];
						for(int cur=0;cur<paramSize;cur++){
							mparam[cur] = ClassUtil.forName(next.params.get(cur).getClassName());
							System.out.println("t: "+mparam[cur]);
						}
						//so far so good...
						Method method = foundClass.getDeclaredMethod(next.methodName, mparam);
						//TODO - the above line relies on no mixing of obfuscations
						deepFinds.add(method);
					} catch (Throwable e) {
						System.out.println("fail");
						//this is expected
						continue;
					}/* catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					}*/
				//}
			}
		}
		Method ret = null;
		Iterator<Method> reviewiter = deepFinds.iterator();
		while(reviewiter.hasNext()){
			Method next = reviewiter.next();
			if(ret == null){
				ret = next;
				continue;
			}
			if(next.equals(ret))
				continue;
			throw new InclusiveObfuscationException();
		}
		if(ret == null)
			throw new ExclusiveObfuscationException();
		ret.setAccessible(true);
		return ret;
	}
	public static class InstanceData implements Cloneable{
		static boolean sourceForm = false;
		InstanceData(Type from){
			if(from.getSort() == Type.ARRAY){
				this.array = from.getDimensions();
				this.className = from.getElementType().getDescriptor();
			}else{
				this.array = 0;
				this.className = from.getDescriptor();
			}
		}
		private InstanceData(int array, String type){
			this.array = array;
			this.className = type;
		}
		int array;
		public String className;
		@Override
		public InstanceData clone(){
			return new InstanceData(this.array, this.className);
		}
		
		@Override
		public boolean equals(Object obj){
			if(super.equals(obj))
				return true;
			if(obj instanceof InstanceData){
				InstanceData test = (InstanceData)obj;
				if(this.array == test.array)
					return this.className.equals(test.className);
				return false;
			}
			return false;
		}
		@Override
		public String toString(){
			if(sourceForm){
				String ret = className;//well, not quite right for primitives, but it's fast, and it can be understood well enough
				for(int cur=0;cur<array;cur++){
					ret += "[]";
				}
				return ret;
			}else{
				String ret = "";
				for(int cur=0;cur<array;cur++){
					ret += '[';
				}
				return ret+ObfuscationHelper3.prepareForASM(this.className);
			}
		}
		public String getClassName(){
			if(this.array == 0){
				return ObfuscationHelper3.unprepareClassName2(ObfuscationHelper3.prepareClassName2(this.className));
			}else{
				String ret = "";
				for(int cur=0;cur<array;cur++){
					ret += '[';
				}
				return ret+ObfuscationHelper3.prepareForASM(this.className);
			}
		}
	}
	public class MethodData implements Cloneable{
		MethodData(String className, String methodName, InstanceData returnName, List<InstanceData> params, boolean boil){
			this.className = className;
			this.methodName = methodName;
			this.params = params;
			this.returnName = returnName;
			if(boil){
				this.hash = new Integer(this.hashCode());
				this.toString = this.toString();
			}
		}
		public String className;
		public String methodName;
		public List<InstanceData> params;
		public InstanceData returnName;
		String toString;
		Integer hash;
		@Override
		public MethodData clone(){
			List<InstanceData> params = new ArrayList(this.params.size());
			for(InstanceData each : this.params){
				params.add(each.clone());
			}
			return new MethodData(this.className, this.methodName, this.returnName, params, true);
		}
		public void unBoil(){
			this.toString = null;
			this.hash = null;
		}
		public void boil(){
			this.hash = new Integer(this.hashCode());
			this.toString = this.toString();
		}
		public void updateBoil(){
			this.unBoil();
			this.boil();
		}
		@Override
		public boolean equals(Object obj){
			if(super.equals(obj))
				return true;
			if(obj instanceof MethodData){
				MethodData test = (MethodData) obj;
				/*if(this.className.equals(test.className) && this.methodName.equals(test.methodName)){
					if(this.params == null){
						if(test.params == null){
						}else{
							return false;
						}
					}else{
						if(test.params == null){
							return false;
						}//else
						if(this.returnName.equals(test.returnName)){
							int size = this.params.size();
							if(size == test.params.size()){
								for(int cur=0;cur<size;cur++){
									if(!this.params.get(cur).equals(test.params.get(cur)))
										return false;
								}
								return true;
							}
						}
					}
				}*/
				return this.toString().equals(test.toString());
			}
			return false;
		}
		@Override
		public String toString(){
			if(this.toString != null)
				return this.toString;
			StringBuilder builder = new StringBuilder();
			builder.append(this.className).append('.').append(this.methodName);
			if(this.params != null){
				builder.append('(');
				for(InstanceData eachp : this.params){
					builder.append(eachp.toString());
				}
				builder.append(')');
				builder.append(this.returnName);
			}
			return builder.toString();
		}
		@Override
		public int hashCode(){
			if(hash != null)
				return hash.intValue();
			return this.toString().hashCode();
		}
		public String getDescString(){
			StringBuilder methodDescB = new StringBuilder();
			methodDescB.append('(');
			for(InstanceData eachParam : this.params){
				methodDescB.append(eachParam.toString());
			}
			methodDescB.append(')');
			methodDescB.append(this.returnName.toString());
			return methodDescB.toString();
		}
	}
	class FieldData{
		FieldData(String className, String fieldName){
			this.className = className;
			this.fieldName = fieldName;
		}
		String className;
		String fieldName;
		@Override
		public boolean equals(Object obj){
			if(super.equals(obj))
				return true;
			if(obj instanceof FieldData){
				FieldData test = (FieldData)obj;
				return this.className.equals(test.className) && this.fieldName.equals(test.fieldName);
			}
			return false;
		}
		@Override
		public int hashCode(){
			return (className+'.'+fieldName).hashCode();
		}
		
	}
	@Override
	public void registerClassRelation(String from, String to){
		this.classNames.add_OneDirectional_pair(from, to);
	}
	@Override
	public void registerFieldRelation(FieldData from, FieldData to){
		this.fields.add_OneDirectional_pair(from, to);
	}
	@Override
	public void registerMethodRelation(MethodData from, MethodData to){
		this.methods.add_OneDirectional_pair(from, to);
	}
	@Override
	public IObfuscationDenoter<String, FieldData, MethodData> getDenoter(){
		return this;
	}
}
