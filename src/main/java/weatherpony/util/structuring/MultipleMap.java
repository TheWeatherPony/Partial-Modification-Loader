package weatherpony.util.structuring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultipleMap<K, V> implements Map<K, List<V>> {
	private List<Map<K,V>> maps;
	private List<Map<K,List<V>>> listmaps;
	public MultipleMap(){
		this.maps = new ArrayList();
		this.listmaps = new ArrayList();
	}
	public MultipleMap(int capacity1, int capacity2){
		this.maps = new ArrayList(capacity1);
		this.listmaps = new ArrayList(capacity2);
	}
	@Override
	public void clear(){
		this.maps.clear();
		this.listmaps.clear();
	}
	@Override
	public boolean containsKey(Object arg0) {
		Iterator<Map<K, V>> iter = maps.iterator();
		while(iter.hasNext()){
			if(iter.next().containsKey(arg0))
				return true;
		}
		Iterator<Map<K, List<V>>> iter2 = this.listmaps.iterator();
		while(iter2.hasNext()){
			if(iter2.next().containsKey(arg0))
				return true;
		}
		return false;
	}
	@Override
	public boolean containsValue(Object arg0) {
		Iterator<Map<K,V>> iter = maps.iterator();
		while(iter.hasNext()){
			if(iter.next().containsValue(arg0))
				return true;
		}
		throw new UnsupportedOperationException();
		//return false;
	}
	@Override
	public Set<java.util.Map.Entry<K, List<V>>> entrySet() {
		throw new UnsupportedOperationException();//maybe later.
	}
	@Override
	public List<V> get(Object key){
		Iterator<Map<K,V>> iter = this.maps.iterator();
		List<V> ret = new ArrayList();
		boolean good = false;
		while(iter.hasNext()){
			Map<K,V> next = iter.next();
			if(next.containsKey(key)){
				good = true;
				ret.add(next.get(key));
			}
		}
		Iterator<Map<K,List<V>>> iter2 = this.listmaps.iterator();
		while(iter2.hasNext()){
			Map<K,List<V>> next = iter2.next();
			if(next.containsKey(key)){
				good = true;
				ret.addAll(next.get(key));
			}
		}
		return good ? Collections.unmodifiableList(ret) : null;
	}
	@Override
	public boolean isEmpty() {
		Iterator<Map<K,V>> iter = this.maps.iterator();
		while(iter.hasNext()){
			if(!iter.next().isEmpty())
				return false;
		}
		Iterator<Map<K,List<V>>> iter2 = this.listmaps.iterator();
		while(iter2.hasNext()){
			if(!iter2.next().isEmpty())
				return false;
		}
		return true;
	}
	@Override
	public Set<K> keySet() {
		throw new UnsupportedOperationException();
	}
	@Override
	public List<V> put(K key, List<V> value){
		throw new UnsupportedOperationException();
	}
	@Override
	public void putAll(Map<? extends K, ? extends List<V>> m) {
		throw new UnsupportedOperationException();
	}
	@Override
	public List<V> remove(Object arg0) {
		throw new UnsupportedOperationException();
	}
	@Override
	public int size() {
		throw new UnsupportedOperationException();
	}
	@Override
	public Collection<List<V>> values() {
		throw new UnsupportedOperationException();
	}
	public boolean addMap(Map<K,V> map){
		if(this.maps.contains(map)){
			return false;
		}
		this.maps.add(map);
		return true;
	}
	public boolean addMap(Map<K,V> map, boolean addIfAddedAlready){
		if(addIfAddedAlready){
			this.maps.add(map);
			return true;
		}else{
			if(this.maps.contains(map)){
				return false;
			}else{
				this.maps.add(map);
				return true;
			}
		}
	}
	public boolean addListMap(Map<K,List<V>> map){
		if(this.listmaps.contains(map)){
			return false;
		}
		this.listmaps.add(map);
		return true;
	}
	public boolean addListMap(Map<K,List<V>> map, boolean addIfAddedAlready){
		if(addIfAddedAlready){
			this.listmaps.add(map);
			return true;
		}else{
			if(this.listmaps.contains(map)){
				return false;
			}else{
				this.listmaps.add(map);
				return true;
			}
		}
	}
}