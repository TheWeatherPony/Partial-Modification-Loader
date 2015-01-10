package weatherpony.partial;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import weatherpony.partial.api.MethodHandleFetchData;
import weatherpony.partial.hook.HookClassHelper;
import weatherpony.partial.internal.GeneralHookManager;
import weatherpony.partial.internal.ModHook;

public final class HookListenerHelperPool<RetType>{
	public HookListenerHelperPool(HookClassHelper hookClassHelper, List<ModHook<RetType>> listeners, CallWrapper<RetType> proxy){
		this.listeners = listeners;
		this.proxy = proxy;
		this.classCollection = hookClassHelper;
		generateHookListenerHelperData();
	}
	protected final HookClassHelper classCollection;
	private final List<ModHook<RetType>> listeners;
	private final CallWrapper<RetType> proxy;
	private final Object poolLock = new Object();
	private int currentlyOut = 0;
	
	protected Object version;
	private Stack<CallWrapper<RetType>> hooks;
	private boolean needsTrace;
	private void generateHookListenerHelperData(){
		version = new Object();
		hooks = new Stack();
		boolean needsTrace = false;
		ListIterator<ModHook<RetType>> iter = listeners.listIterator(listeners.size());
		while(iter.hasPrevious()){//this adds the listeners in reverse order, which makes the first element end on top
			CallWrapper<RetType> hook = iter.previous().getIfWarented();
			if(hook != null){
				hooks.push(hook);
				if(hook.requiresTrace())
					needsTrace = true;
			}
		}
		
		make_inAdvance();//prepares the pool with one pre-prepared HookListenerHelper
	}
	
	private Stack<HookListenerHelper<RetType>> ready = new Stack();
	private HookListenerHelper<RetType> make(){
		HookListenerHelper ret = null;
		synchronized(this.poolLock){
			ret = new HookListenerHelper(this.hooks, this.proxy, this.needsTrace, this);
			return ret;
		}
	}
	private void make_inAdvance(){
		HookListenerHelper ret = null;
		synchronized(this.poolLock){
			ret = new HookListenerHelper(this.hooks, this.proxy, this.needsTrace, this);
			ready.add(ret);
		}
	}
	protected HookListenerHelper<RetType> getOne(){
		HookListenerHelper<RetType> ret;
		synchronized(poolLock){
			this.currentlyOut++;
			if(!ready.isEmpty()){
				ret = get();
			}
			
			ret = make();
			return ret;
		}
	}
	private HookListenerHelper<RetType> get(){
		synchronized(poolLock){
			return ready.pop();
		}
	}
	public synchronized void reloadPool_safe(){
		start: synchronized(poolLock){
			if(this.currentlyOut > 0)
				break start;//go back to waiting, and release the pool's lock... briefly
			//now there are none currently in use, and no more can be accessed.
			ready.clear();//dump all previous hooks
			generateHookListenerHelperData();//reload the data, and make one 
			
		}
	}
	
	protected void markDone(HookListenerHelper<RetType> hooks){
		synchronized(poolLock){
			ready.push(hooks);
			this.currentlyOut--;
		}
	}
	private HashMap<String, Object> consistantDetails = new HashMap();
	private ThreadLocal<HashMap<String, Object>> detailsFromThread = new ThreadLocal();
	public HashMap<String, Object> getConsistants(){
		return consistantDetails;
	}
	public HashMap<String, Object> getThreadConsistants(){
		HashMap<String, Object> val = detailsFromThread.get();
		if(val == null){
			detailsFromThread.set((val = new HashMap()));
		}
		return val;
	}
	public RetType distribute(Object[] params){
		HookListenerHelper<RetType> hooks = getOne();

		hooks.prepare(getConsistants(), getThreadConsistants());//loads the extra details that were loaded for the call
		RetType ret = hooks.distribute(params);//distributes the call, then cleans up 
		
		return ret;
	}
	public static HookListenerHelperPool getHookHelperPool(String forClass, String methodName, String methodDesc){
		return GeneralHookManager.getHookHelperPool_static(forClass, methodName, methodDesc);
	}
	protected MethodHandle getMethodHandleForSuper(MethodHandleFetchData method) throws NoSuchMethodException, IllegalAccessException{
		return this.classCollection.getMethodHandleForSuper(method);
	}
}
