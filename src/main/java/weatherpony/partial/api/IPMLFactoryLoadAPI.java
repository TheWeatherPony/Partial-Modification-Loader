package weatherpony.partial.api;

public interface IPMLFactoryLoadAPI extends IPMLLoadAPI{
	public int modAPIVersionNumber();
	public boolean modAPIVersionNumberDirectlyCompatible(int modAPINumber);
}
