package weatherpony.partial;

public class CallWrapperI<RetType> extends CallWrapper<RetType> {
	public CallWrapperI(CallData data, ICallListener<RetType> call) {
		super(data);
		this.call = call;
	}
	private ICallListener<RetType> call;
	@Override
	protected RetType call2(HookListenerHelper<RetType> hooks) throws Throwable {
		return call(hooks);
	}

}
