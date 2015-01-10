package weatherpony.partial;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Stack;

import weatherpony.partial.api.MethodHandleFetchData;

public class HookListenerHelper<RetType>{
	public HookListenerHelper(Stack<CallWrapper<RetType>> hooks, CallWrapper<RetType> proxy, boolean needsTrace, HookListenerHelperPool<RetType> pool){
		this.version = pool.version;
		this.nexts = (Stack<CallWrapper<RetType>>) hooks.clone();
		this.needsTrace = needsTrace;
		this.proxy = proxy;
		this.pool = pool;
	}
	protected final Object version;
	private final boolean needsTrace;
	private final HookListenerHelperPool<RetType> pool;
	private Stack<CallWrapper<RetType>> nexts;
	private Object[] params = null;
	private HashMap<String,Object> someExtraDetails;
	public Object getExtra(String key){
		return someExtraDetails.get(key);
	}
	public void setExtra(String key, Object val){
		someExtraDetails.put(key, val);
	}
	public boolean hasExtra(String key){
		return someExtraDetails.containsKey(key);
	}
	public void removeExtra(String key){
		someExtraDetails.remove(key);
	}
	public <T> T getParam(int spot){
		return (T) params[spot];
	}
	public void setParam(Object value, int spot){
		params[spot] = value;
	}
	public RetType distribute(Object[] params) throws WrappedException{
		this.params = params;
		if(this.needsTrace){
			StackTraceElement[] trace = Thread.currentThread().getStackTrace();
			this.trace = new StackTraceElement[trace.length-2];//HookListenerHelper.distribute, and a call from the internal workings of PML
			System.arraycopy(trace, 0, this.trace, 0, this.trace.length);
		}
		RetType ret = this.callNext();
		done();
		return ret;
	}
	public RetType callNext() throws WrappedException//which extends RuntimeExcption. This can also throw a RuntimeException, but only under very weird conditions
	{
		if(nexts.isEmpty()){
			return callProxy();//throws WrappedException
		}
		CallWrapper<RetType> call = nexts.pop();
		try{
			return call.call(this);
		}catch(Throwable e){
			throw WrappedException.wrap(e);//this will ensure the error is always one-deep
		}finally{
			this.nexts.push(call);
		}
	}
	private CallWrapper<RetType> proxy;
	private RetType callProxy(){
		try{
			return proxy.call(this);
		}catch(Throwable e){
			throw WrappedException.wrap(e);//this will ensure the error is always one-deep
		}
	}
	private StackTraceElement[] trace;//this is useful to see who called the hooked method, rather than try to weed-out StackTraceElements to get the same result.
	
	public StackTraceElement[] getPremadeTrace_ifPreRequested() {
		return this.trace;
	}
	public boolean wasStackTracePreRequested() {
		return this.needsTrace;
	}
	
	private void done(){
		this.trace = null;
		this.params = null;
		this.someExtraDetails = null;
		this.pool.markDone(this);
	}
	protected void prepare(HashMap<String, Object> consistant, HashMap<String, Object> preregistered){
		someExtraDetails = new HashMap();
		someExtraDetails.putAll(consistant);
		someExtraDetails.putAll(preregistered);
	}

	public MethodHandle getMethodHandleForSuper(MethodHandleFetchData method) throws NoSuchMethodException, IllegalAccessException{
		return this.pool.getMethodHandleForSuper(method);
	}
}
