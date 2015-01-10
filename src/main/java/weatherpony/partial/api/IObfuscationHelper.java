package weatherpony.partial.api;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

//please wait to use this stuff until the game is mostly loaded. Basically, wait until the last moment. Also, hang on to the results rather than make repeated calls to get the same thing. Figuring this stuff out is rather expensive for CPU.
public interface IObfuscationHelper{
	public static class ObfuscationException extends Exception{
		public static class ExclusiveObfuscationException extends ObfuscationException{
		}
		public static class InclusiveObfuscationException extends ObfuscationException{
		}
	}

	public Class getClass(String name) throws ObfuscationException;

	public Field getField(String className, String fieldName) throws ObfuscationException;

	public Method getMethod(String className, String methodName, String methodDesc) throws ObfuscationException;
}