package weatherpony.util.lists;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class CombinationIterator<Type> implements Iterator<List<Type>>{
	public CombinationIterator(List<List<Type>> iterateThrough, boolean emptyCounts){
		this.collection = iterateThrough;
		this.next = new int[iterateThrough.size()];
		if(emptyCounts)
			this.startedBlank = iterateThrough.isEmpty();
		else
			this.startedBlank = false;
		this.validate();
		this.hasNext = true;
		
	}
	private final boolean startedBlank;
	private final List<List<Type>> collection;
	private final int[] next;
	private boolean hasNext;
	private void advanceNext(){
		for(int cur=this.next.length-1;cur>=0;cur--){
			this.next[cur] = this.next[cur] +1;
			if(this.next[cur] == this.collection.get(cur).size()){//hit the end
				this.next[cur] = 0;
			}else{//still some to go
				break;//don't change the others
			}
		}
		//finished the last one
		this.hasNext = false;
	}
	private void validate(){
		for(int cur=0;cur<this.next.length;cur++){
			if(this.collection.get(cur).isEmpty())
				throw new RuntimeException();
		}
		if(this.startedBlank != this.collection.isEmpty())
			throw new RuntimeException();
	}
	@Override
	public boolean hasNext(){
		return this.hasNext;
	}

	@Override
	public List<Type> next(){
		if(!this.hasNext)
			throw new NoSuchElementException();
		if(this.startedBlank){
			this.hasNext = false;
			return new ArrayList(0);
		}
		List<Type> ret = new ArrayList(next.length);
		for(int cur=0;cur<next.length;cur++){
			ret.add(this.collection.get(cur).get(this.next[cur]));
		}
		this.advanceNext();
		return ret;
	}

	@Override
	public void remove(){
		throw new UnsupportedOperationException();
	}

}
