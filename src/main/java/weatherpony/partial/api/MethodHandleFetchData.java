package weatherpony.partial.api;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

public final class MethodHandleFetchData{
	private final Class originallyFrom;
	private final MethodType methodType;
	private final String name;
	private final int hash;
	private boolean isInPool = false;
	private static final HashMap<MethodHandleFetchData,MethodHandleFetchData> constantPool = new HashMap();//TODO - make this more RAM efficient, by writing a custom self-mapper
	public static MethodHandleFetchData getFetchData(Class originallyFrom, Method method){
		MethodHandleFetchData temp = new MethodHandleFetchData(originallyFrom, method);
		MethodHandleFetchData other = constantPool.get(temp);
		if(other == null){
			constantPool.put(temp, temp);
			temp.isInPool = true;
			return temp;
		}else{
			return other;
		}
	}
	private MethodHandleFetchData(Class originallyFrom, Method method){
		this.originallyFrom = originallyFrom;
		this.methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
		this.name = method.getName();
		this.hash = (originallyFrom.getName().hashCode() ^ this.name.hashCode()) ^ Arrays.hashCode(method.getParameterTypes());
	}
	@Override
	public int hashCode(){
		return this.hash;
	}
	@Override
	public boolean equals(Object obj){
		if(this == obj)
			return true;
		if(obj instanceof MethodHandleFetchData){
			MethodHandleFetchData data = ((MethodHandleFetchData)obj);
			
			if(this.isInPool && data.isInPool)//but they aren't the same object, so they're different
				return false;
			
			if(this.hashCode() != obj.hashCode())
				return false;
		
			if(!this.name.equals(data.name) || !this.originallyFrom.equals(data.originallyFrom) || !this.methodType.equals(data.methodType))
				return false;
			
			return true;
		}
		
		return false;
	}
	public Class getOriginal(){
		return this.originallyFrom;
	}
	public String getName() {
		return this.name;
	}
	public MethodType getMethodType() {
		return this.methodType;
	}
}
