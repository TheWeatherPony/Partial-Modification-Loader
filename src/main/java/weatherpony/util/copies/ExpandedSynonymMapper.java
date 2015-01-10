package weatherpony.util.copies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.world.World;

public class ExpandedSynonymMapper<Form> implements ISynonymSupplier<Form>{
	public ExpandedSynonymMapper(){
		this.maps = new HashMap();
	}
	public ExpandedSynonymMapper(int initialSize){
		this.maps = new HashMap(initialSize);
	}
	private Map<Form, List<Form>> maps;
	public void add_OneDirectional_pair(Form from, Form to){
		if(from == null || to == null)
			throw new IllegalArgumentException();
		if(from.equals(to))
			return;//why bother?
		List<Form> list = this.maps.get(from);
		if(list == null){//not added yet
			this.maps.put(from, (list = new ArrayList(5)));
		}
		if(!list.contains(to)){
			list.add(to);
		}
	}
	public boolean containsKey(Form base){
		if(base == null)
			throw new IllegalArgumentException();
		return this.containsKey(base);
	}
	public List<Form> get(Form base){
		if(base == null)
			return null;
		List<Form> ret = new ArrayList();
		if(!this.maps.containsKey(base)){
			ret.add(base);
			return ret;
		}
		Collection<Form> processing = new ArrayList();
		Collection<Form> nextToProcess = new ArrayList();
		processing.add(base);
		while(!processing.isEmpty()){
			Iterator<Form> iter = processing.iterator();
			while(iter.hasNext()){
				Form next = iter.next();
				ret.add(next);
				iter.remove();
				if(this.maps.containsKey(next)){
					List<Form> add = this.maps.get(next);
					Iterator<Form> iter2 = add.iterator();
					while(iter2.hasNext()){
						Form test = iter2.next();
						if(!ret.contains(test) && !processing.contains(test)){
							if(!nextToProcess.contains(test))
								nextToProcess.add(test);//if it hasn't been found yet, then add it to the list to process
						}
					}
				}
			}
			Collection<Form> temp = processing;//reuse the empty Collection, rather than make a new one, because of the potential speed improvement (noticable on larger groups)
			processing = nextToProcess;
			nextToProcess = temp;
		}
		//processed them all.
		return ret;
	}
	@Override
	public Iterator<Form> getAllSynonyms(Form of) {
		Collection<Form> syns = this.get(of);
		if(syns == null)
			return null;//just to be cruel. jk :P (this will only be if passed null. null in => null out)
		return syns.iterator();
	}
}
