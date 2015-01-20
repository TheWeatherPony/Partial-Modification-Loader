package weatherpony.partial.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import net.minecraft.launchwrapper.Launch;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;

public final class ClassData {
	public static ClassData INSTANCE;
	public ClassData(){
		INSTANCE = this;
	}
	HashMap<String, ExtentionData> classMap = new HashMap(512);//512 chosen because it's higher than the default of 16. Some re-hashing will still be needed, but not quite as much early on.
	static class ExtentionData{
		ExtentionData(String ext, List<String> inter){
			this.extend = ext;
			this.interfaces = inter;
		}
		final String extend;
		final List<String> interfaces;
		@Override
		public boolean equals(Object obj){
			if(this == obj)
				return true;
			if(obj instanceof ExtentionData){
				ExtentionData cmp = (ExtentionData)obj;
				if(this.extend.equals(cmp.extend))
					return Arrays.equals(this.interfaces.toArray(), cmp.interfaces.toArray());
				return false;
			}
			return false;
		}
	}
	public static void ASM_giveExtendsAndImplements(String forClass, String Extends, List<String> interfaces){
		INSTANCE._ASM_giveExtendsAndImplements_(forClass, Extends, interfaces);
	}
	private void _ASM_giveExtendsAndImplements_(String forClass, String Extends, List<String> interfaces){
		ExtentionData list = classMap.get(forClass);
		if(list == null){
			if(interfaces == null)
				interfaces = new ArrayList(0);
			classMap.put(forClass, (list = new ExtentionData(Extends,(interfaces == null ? new ArrayList(0) : interfaces))));
		}else{
			if(interfaces == null)
				interfaces = new ArrayList(0);
			ExtentionData list2 = new ExtentionData(Extends,(interfaces == null ? new ArrayList(0) : interfaces));
			if(!list.equals(list2))
				throw new RuntimeException();
		}
	}
	public static String deObjectify(String className){
		if(className.length() == 1)
			return className;
		if(className.startsWith("L") && className.endsWith(";")){
			return className.substring(1, className.length()-1);
		}
		return className;
	}
	protected Iterator<String> get(String forClass){
		ExtentionData data = this.classMap.get(forClass);
		if(data == null){
			if(forClass.equals("java.lang.Object"))
				return Iterators.emptyIterator();
			//found a previously loaded, unscanned class.
			try {
				Class looking = Class.forName(forClass, false, Launch.classLoader);
				String superN = null;
				if(!looking.isInterface())
					superN = deObjectify(looking.getSuperclass().getName());
				Class[] interfaceCs = looking.getInterfaces();
				List<String> interfaces = new ArrayList(interfaceCs.length);
				for(Class interfaceC : interfaceCs){
					interfaces.add(deObjectify(interfaceC.getName()));
				}
				_ASM_giveExtendsAndImplements_(forClass, superN, interfaces);
				data = this.classMap.get(forClass);
			} catch (ClassNotFoundException e) {
				//e.printStackTrace();
				throw Throwables.propagate(e);
			}
		}
		Iterator<String> is = data.interfaces == null ? Iterators.<String>emptyIterator() : data.interfaces.iterator();
		Iterator<String> s = data.extend == null ? Iterators.<String>emptyIterator() : Iterators.singletonIterator(data.extend);
		return Iterators.concat(s,is);
	}
	public Collection<String> getAllSupers(String forClass){
		Collection<String> ret = new HashSet();
		Stack<Iterator<String>> temp = new Stack();
		temp.push(this.get(forClass));
		while(!temp.isEmpty()){
			Iterator<String> each = temp.pop();
			while(each.hasNext()){
				String next = each.next();
				if(!ret.contains(next)){
					ret.add(next);
					temp.push(this.get(next));
				}
			}
		}
		return ret;
	}
}
