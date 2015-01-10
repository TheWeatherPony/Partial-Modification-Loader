package weatherpony.partial.internal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import weatherpony.partial.WrapTiming;

public class HookSegmentList{
	//private EnumMap<WrapTiming, List<ModHook>> calls = new EnumMap(WrapTiming.class);
	private List<ModHook>[] calls = new List[WrapTiming.values().length];
	{
		int size = calls.length;
		for(int cur=0;cur<size;cur++){
			calls[cur] = new ArrayList(2);
		}
	}
	public void addHook(ModHook call){
		calls[call.call.data.timing.ordinal()].add(call);
	}
	void addHooks(HookSegmentList hooks){
		int size = calls.length;
		for(int cur=0;cur<size;cur++){
			this.calls[cur].addAll(hooks.calls[cur]);
		}
	}
	List<ModHook> condense(){
		ArrayList<ModHook> ret = new ArrayList();
		int size = calls.length;
		for(int cur=0;cur<size;cur++){
			ret.addAll(calls[cur]);
		}
		return ret;
	}
	int count(){
		int count = 0;
		int size = calls.length;
		for(int cur=0;cur<size;cur++){
			count += calls[cur].size();
		}
		return count;
	}
}
