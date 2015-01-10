package weatherpony.partial;

public interface ICallListener<RetType> {
	RetType call2(HookListenerHelper<RetType> hooks) throws Throwable;
}
