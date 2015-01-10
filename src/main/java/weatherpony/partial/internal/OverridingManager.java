package weatherpony.partial.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OverridingManager{
	protected static OverridingManager INSTANCE;
	public OverridingManager(){
		 INSTANCE = this;
	}
	public static List<OverrideData> getRequiredOverrides(String forClass){
		return INSTANCE.map.get(forClass);
	}
	HashMap<String, List<OverrideData>> map = new HashMap();
	public static class OverrideData{
		OverrideData(String previousClass, String method, String desc, String originalClass){
			this.previouslyFrom = previousClass;
			this.method = method;
			this.desc = desc;
			this.originallyFrom = originalClass;
		}
		public final String previouslyFrom;
		public final String originallyFrom;
		public final String method, desc;
	}
	public static void addRequest(String inClass, String method, String desc, String expectedToOverrideFrom, String originalClass){
		HashMap<String, List<OverrideData>> map = INSTANCE.map;
		List<OverrideData> list = map.get(inClass);
		if(list == null){
			map.put(inClass, (list = new ArrayList(5)));
		}
		list.add(new OverrideData(expectedToOverrideFrom, method, desc, originalClass));
	}
}
