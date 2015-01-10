package weatherpony.partial;


public abstract class CallWrapper<RetType>{
	//this is for ASM-created implementations that operate on annotated methods TODO
	@Deprecated
	public CallWrapper(String className, String methodName, String methodDesc, WrapTiming timing){
		this(new CallData.CallDataFactory()
			.setClass(className)
			.setMethodName(methodName)
			.setMethodDesc(methodDesc)
			.setTiming(timing)
			.create());
	}
	public CallWrapper(CallHook details){
		this.data = new CallData(details);
	}
	//this is for direct implementations
	public CallWrapper(CallData data){
		this.data = data;
	}
	public final CallData data;
	public boolean requiresTrace(){
		return data.needsTrace;
	}
	protected RetType call(HookListenerHelper<RetType> hooks) throws Throwable//returns an Object.
	{
		try{
			return call2(hooks);
		}catch(Throwable e){
			throw WrappedException.wrap(e);
		}
	}
	protected abstract RetType call2(HookListenerHelper<RetType> hooks) throws Throwable;
	
	
}
