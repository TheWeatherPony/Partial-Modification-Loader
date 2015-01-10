package weatherpony.util.structuring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterators;

import weatherpony.util.copies.ISynonymSupplier;


public class MultiPathMap<K,V>{
	private HashMap<K,Node> map;
	private MultipleMap<K,List<K>> paths;
	private Map<K,List<K>> personalPaths = new HashMap();
	
	public MultiPathMap(){
		map = new HashMap();
		paths = new MultipleMap();
		paths.addMap(personalPaths);
	}
	public MultiPathMap(int initialCapacity){
		map = new HashMap(initialCapacity);
		paths = new MultipleMap();
		paths.addMap(personalPaths);
	}
	public MultiPathMap(int initialCapacity, float loadFactor){
		map = new HashMap(initialCapacity, loadFactor);
		paths = new MultipleMap();
		paths.addMap(personalPaths);
	}
	
	class Node{
		V val;//1
		V valDir;//2
		V valNonDir;//4
		int uses;
	}
	//the order things are returned in may be changed in the future. don't depend on them being in a certain order.
	public List<V> get(K key){
		ArrayList<V> ret = new ArrayList();
		if(key != null){
			Collection<K> todo = new HashSet(64);
			Collection<K> done = new HashSet(64);//value chosen abstractly. chosen because it is higher than the default capacity of 16. HashSet chosen for speed improvement (constant search time)
			
			Iterator<K> keyiterator;
			//if(this.synonymgiver == null){
				keyiterator = Iterators.singletonIterator(key);
			/*}else{
				keyiterator = this.synonymgiver.getAllSynonyms(key);
			}*/
			while(keyiterator.hasNext()){
				K eachkey = keyiterator.next();
				
				Node node = map.get(eachkey);
				done.add(eachkey);
				if(node != null){
					if(paths.containsKey(eachkey)){
						Iterator<List<K>> iter = paths.get(eachkey).iterator();
						while(iter.hasNext()){
							todo.addAll(iter.next());//sub-nodes
						}
					}
					
					if((node.uses & 2) == 2){
						ret.add(node.valDir);//direct value
					}
					if((node.uses & 1) == 1){
						ret.add(node.val);//general value
					}
				}
			}
			
			Collection<K> nexttodo = new ArrayList();
			
			while(!todo.isEmpty()){
				Iterator<K> iter = todo.iterator();
				
				while(iter.hasNext()){
					K next = iter.next();
					iter.remove();
					
					Iterator<K> nextiterator;
					//if(this.synonymgiver == null){
						nextiterator = Iterators.singletonIterator(key);
					/*}else{
						nextiterator = this.synonymgiver.getAllSynonyms(key);
						
					}*/
					while(nextiterator.hasNext()){
						K eachnext = nextiterator.next();
					
					
						if(done.contains(eachnext))
							continue;
						done.add(eachnext);
						
						Node node = map.get(eachnext);
						if(node == null)
							continue;
						
						if(paths.containsKey(eachnext)){
							Iterator<List<K>> iter2 = paths.get(next).iterator();
							while(iter2.hasNext()){
								todo.addAll(iter2.next());//sub-nodes
							}
						}
						
						if((node.uses & 4) == 4)
							ret.add(node.valNonDir);//non-direct value
						if((node.uses & 1) == 1)
							ret.add(node.val);//general value
						
					}
				}
				
				Collection<K> swap = todo;
				todo = nexttodo;
				nexttodo = swap;
			}
		}else{
			new Exception().printStackTrace();
		}
		return ret;
	}
	public void clearMap(){
		map.clear();
	}
	public boolean containsValueForKey(K key, MultiPathEnum_Plus type){
		switch(type){
		case General:
			return this.containsValueForKey(key);
		case Direct:
			return this.containsDirectValueForKey(key);
		case NonDirect:
			return this.containsNonDirectValueForKey(key);
		case Plus:
		default:
			throw new RuntimeException();
		}
	}
	public boolean containsSomeValueForKey(K key){
		return map.containsKey(key);
	}
	public boolean containsValueForKey(K key){
		Node node = map.get(key);
		return node == null ? false : ((node.uses & 1) == 1);
	}
	public boolean containsDirectValueForKey(K key){
		Node node = map.get(key);
		return node == null ? false : ((node.uses & 2) == 2);
	}
	public boolean containsNonDirectValueForKey(K key){
		Node node = map.get(key);
		return node == null ? false : ((node.uses & 4) == 4);
	}
	public boolean hasPathForKey(K key){
		return paths.containsKey(key);
	}
	public V getValue(K key, MultiPathEnum_Plus type){
		switch(type){
		case General:
			return this.getValue(key);
		case Direct:
			return this.getDirectValue(key);
		case NonDirect:
			return this.getNonDirectValue(key);
		case Plus:
		default:
			throw new RuntimeException();
		}
	}
	public V getValue(K key){
		Node node = map.get(key);
		return node == null ? null : node.val;
	}
	public V getDirectValue(K key){
		Node node = map.get(key);
		return node == null ? null : node.valDir;
	}
	public V getNonDirectValue(K key){
		Node node = map.get(key);
		return node == null ? null : node.valNonDir;
	}
	public List<K> getPaths(K key){
		Iterator<List<K>> iter = this.paths.get(key).iterator();
		ArrayList<K> ret = new ArrayList();
		while(iter.hasNext())
			ret.addAll(iter.next());
		return ret;
	}
	public void addPathMap(Map<K, List<K>> map){
		this.paths.addMap(map, false);
	}
	Node getNode_make(K key){
		Node node = map.get(key);
		if(node == null){
			map.put(key, (node = new Node()));
		}
		return node;
	}
	public V setValue(K key, V val, MultiPathEnum_Plus type){
		switch(type){
		case General:
			return this.setValue(key, val);
		case Direct:
			return this.setValueDirect(key, val);
		case NonDirect:
			return this.setValueNotDirect(key, val);
		case Plus:
		default:
			throw new RuntimeException();
		}
	}
	public V setValue(K key, V val){
		Node node = getNode_make(key);
		V ret = node.val;
		node.val = val;
		node.uses |= 1;
		return ret;
	}
	public V setValueDirect(K key, V valdirect){
		Node node = getNode_make(key);
		V ret = node.valDir;
		node.valDir = valdirect;
		node.uses |= 2;
		return ret;
	}
	public V setValueNotDirect(K key, V valnotdirect){
		Node node = getNode_make(key);
		V ret = node.valNonDir;
		node.valNonDir = valnotdirect;
		node.uses |= 4;
		return ret;
	}
	List<K> getPaths_make(K key){
		List<K> ret = this.personalPaths.get(key);
		if(ret == null){
			this.personalPaths.put(key, (ret = new ArrayList()));
		}
		return ret;
	}
	public boolean addPersonalPath(K key, K path){
		List<K> paths = getPaths_make(key);
		if(!paths.contains(path)){
			return paths.add(path);
		}
		return false;
	}
	public boolean removePath(K key, K path){
		return getPaths_make(key).remove(path);
	}
	public boolean hasPersonalPath(K key, K path){
		return this.personalPaths.containsKey(key) ? this.personalPaths.get(key).contains(path) : false;
	}
	public void addPersonalPaths(K key, List<K> paths){
		List<K> nodepaths = getPaths_make(key);
		Iterator<K> iter = paths.iterator();
		while(iter.hasNext()){
			K path = iter.next();
			if(!nodepaths.contains(path)){
				nodepaths.add(path);
			}
		}
	}
	public boolean removePersonalPaths(K key, List<K> paths){
		List<K> list = this.personalPaths.get(key);
		if(list == null)
			return false;
		return list.removeAll(paths);
	}
}
