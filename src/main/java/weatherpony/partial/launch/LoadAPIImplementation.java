package weatherpony.partial.launch;

import weatherpony.partial.api.IPMLLoadAPI;

public abstract class LoadAPIImplementation implements IPMLLoadAPI{
	LoadAPIImplementation(int pmlv1, int pmlv2, String mcf, String mcc){
		this.pmlv1 = pmlv1;
		this.pmlv2 = pmlv2;
		this.mcf = mcf;
		this.mcc = mcc;
	}
	final int pmlv1, pmlv2;
	final String mcf, mcc;
	@Override
	public int majorVersionNumber() {
		return this.pmlv2;
	}
	@Override
	public int minorVersionNumber() {
		return this.pmlv1;
	}
	@Override
	public String minecraftVersionNumber() {
		return this.mcf;
	}
	@Override
	public String minecraftCoreVersionNumber() {
		return this.mcc;
	}
}
