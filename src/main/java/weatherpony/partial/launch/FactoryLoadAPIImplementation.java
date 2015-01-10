package weatherpony.partial.launch;

import weatherpony.partial.api.IPMLFactoryLoadAPI;
import weatherpony.partial.api.IPMLModLoadAPI;

public class FactoryLoadAPIImplementation extends LoadAPIImplementation implements IPMLFactoryLoadAPI{
	public static final int currentAPIVersionNumber = 1;
	static final int[] compatibleAPIVersionNumbers = {1};
	FactoryLoadAPIImplementation(int pmlv1, int pmlv2, String mcf, String mcc, IPMLModLoadAPI modAPI) {
		super(pmlv1, pmlv2, mcf, mcc);
		this.modAPI = modAPI;
	}
	final IPMLModLoadAPI modAPI;
	@Override
	public int apiVersionNumber() {
		return this.currentAPIVersionNumber;
	}
	@Override
	public boolean isDirectlyCompatible(int otherAPINumber) {
		for(int comp : this.compatibleAPIVersionNumbers){
			if(comp == otherAPINumber)
				return true;
		}
		return false;
	}
	@Override
	public int modAPIVersionNumber() {
		return this.modAPI.apiVersionNumber();
	}
	@Override
	public boolean modAPIVersionNumberDirectlyCompatible(int modAPINumber) {
		return this.modAPI.isDirectlyCompatible(modAPINumber);
	}
}
