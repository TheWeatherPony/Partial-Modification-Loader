package pmlexample;

import java.io.File;

import weatherpony.partial.api.IPMLFactoryLoadAPI;
import weatherpony.partial.api.IPMLMod;
import weatherpony.partial.api.IPMLModFactory;

public class ExampleFactory extends IPMLModFactory{
	public ExampleFactory(){
		System.out.println("ExampleFactory constructing");
	}
	public static final int FACTORYAPI = 1;
	private boolean load;
	@Override
	@SuppressWarnings("unused")
	public void preinit(IPMLFactoryLoadAPI loadAPI){
		boolean load;
		if(loadAPI.apiVersionNumber() !=FACTORYAPI){
			if(true) throw new RuntimeException("don't throw something here. This is just so I remember I need to update.");
			if(loadAPI.isDirectlyCompatible(FACTORYAPI)){
				load = true;
			}else{
				load = false;
			}
		}else{
			load = true;
		}
		if(load){
			this.load = load;
		}else{
			throw new RuntimeException("Please do something appropriate here. I'm just throwing an exception because I need to update this whenever I change PML's APIs, so it will never actually be thrown");
		}
	}
	@Override
	public File[] loadModFrom(File loadedThisFrom){
		return null;
	}
	@Override
	public String[] noASMOn_prefixs(){
		return null;
	}
	@Override
	public IPMLMod[] loadMod(){
		return this.load ? new IPMLMod[]{new ExampleMod()} : null;
	}
}
