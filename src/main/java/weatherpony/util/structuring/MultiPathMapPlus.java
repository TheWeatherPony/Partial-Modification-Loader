package weatherpony.util.structuring;

import java.util.List;

import weatherpony.util.copies.ISynonymSupplier;

public class MultiPathMapPlus<K, V> extends MultiPathMap<K, V> {
	public MultiPathMapPlus(){
		super();
	}
	public MultiPathMapPlus(int initialCapacity){
		super(initialCapacity);
	}
	public MultiPathMapPlus(int initialCapacity, float loadFactor){
		super(initialCapacity, loadFactor);
	}
	private V plus;
	private boolean plusUsed;
	public V setPlus(V plus){
		V ret = this.plus;
		this.plus = plus;
		this.plusUsed = true;
		return ret;
	}
	public V getPlus(){
		return this.plus;
	}
	public V removePlus(){
		V ret = this.plus;
		this.plus = null;
		this.plusUsed = false;
		return ret;
	}
	public List<V> get(K key){
		List<V> ret = super.get(key);
		if(plusUsed)
			ret.add(0, plus);
		return ret;
	}
	@Override
	public V getValue(K key, MultiPathEnum_Plus type){
		switch(type){
		case General:
			return this.getValue(key);
		case Direct:
			return this.getDirectValue(key);
		case NonDirect:
			return this.getNonDirectValue(key);
		case Plus:
			return this.getPlus();
		default:
			throw new RuntimeException();
		}
	}
	@Override
	public boolean containsValueForKey(K key, MultiPathEnum_Plus type){
		switch(type){
		case General:
			return this.containsValueForKey(key);
		case Direct:
			return this.containsDirectValueForKey(key);
		case NonDirect:
			return this.containsNonDirectValueForKey(key);
		case Plus:
			return this.plusUsed;
		default:
			throw new RuntimeException();
		}
	}
	@Override
	public V setValue(K key, V val, MultiPathEnum_Plus type){
		switch(type){
		case General:
			return this.setValue(key, val);
		case Direct:
			return this.setValueDirect(key, val);
		case NonDirect:
			return this.setValueNotDirect(key, val);
		case Plus:
			return this.setPlus(val);
		default:
			throw new RuntimeException();
		}
	}
}
