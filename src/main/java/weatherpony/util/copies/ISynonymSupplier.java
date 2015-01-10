package weatherpony.util.copies;

import java.util.Iterator;

public interface ISynonymSupplier<Type> {
	public Iterator<Type> getAllSynonyms(Type of);
}
