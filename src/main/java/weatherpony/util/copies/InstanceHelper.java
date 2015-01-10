package weatherpony.util.copies;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is intended for "internalizing" objects, similar to String.intern(), but by internalizing them outside of the actual class... externally, I guess; the class that gets intern'ed doesn't maintain it's pool itself, but maintains a copy of this object.</br>
 * The internalized objects must override Object.equals(Object)boolean and Object.hashCode()int in order to work.</br>
 * The internalized class doesn't need to be final, nor have it's fields be final. Because of this, it is possible to use this class in some creative ways.
 * @author The_WeatherPony
 */
public class InstanceHelper<Type>{
	public InstanceHelper(){
		pool = new HashMap();
	}
	public InstanceHelper(int initialCap){
		pool = new HashMap(initialCap);
	}
	final Map<Type,Type> pool;
	/**
	 * 
	 * @param internalize - the value to internalize
	 * @return either an already-internalized copy of what was passed, or the passed variable that was freshly internalized
	 */
	public Type intern(Type internalize){
		if(this.pool.containsKey(internalize))
			return this.pool.get(internalize);
		this.pool.put(internalize, internalize);
		return internalize;
	}
	public boolean isIntern(Type interned){
		return this.pool.containsKey(interned);
	}
	public boolean isInternInstance(Type interned){
		if(this.isIntern(interned)){
			return this.intern(interned) == interned;//checks if the instance is the same, rather than if they are equivalent.
		}
		return false;
	}
	public Type removeIntern(Type internalized){
		if(this.pool.containsKey(internalized))
			return this.pool.remove(internalized);
		return internalized;
	}
	public Type removeInterned(Type internalized){
		if(this.pool.containsKey(internalized))
			return this.pool.remove(internalized);
		return null;
	}
	public void clearInternalizations(){
		this.pool.clear();
	}
}
