package weatherpony.partial;

import weatherpony.util.structuring.MultiPathEnum_Plus;

//not yet implemented.
public @interface CallHook {
	public String inClass();
	public MultiPathEnum_Plus pathClass() default MultiPathEnum_Plus.Direct;//the hook will only be added to the class itself
	public String methodName();
	public MultiPathEnum_Plus pathMethod() default MultiPathEnum_Plus.General;//the hook will be added to the method(s) with this name, either directly or by synonym
	public String methodDesc();//an empty String means "any method description"
	public MultiPathEnum_Plus pathDesc() default MultiPathEnum_Plus.General; //the hook will be added to the method with this signature
	public WrapTiming timing();//when you want your hook called/injected
	public boolean needsTrace() default false;//making a stack trace is on the expensive side for this system. Please don't ask for one in a method that is meant to be called very rapidly and repeatedly.
}
