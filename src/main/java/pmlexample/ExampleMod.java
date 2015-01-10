package pmlexample;

import net.minecraft.world.biome.BiomeGenBase;
import weatherpony.partial.CallData;
import weatherpony.partial.CallWrapper;
import weatherpony.partial.HookListenerHelper;
import weatherpony.partial.WrapTiming;
import weatherpony.partial.api.IHookRegistrar;
import weatherpony.partial.api.IPMLMod;
import weatherpony.partial.api.IPMLModLoadAPI;

public class ExampleMod extends IPMLMod {
	ExampleMod(){
		super("Example");
		System.out.println("Example PMLMod constructing");
	}
	public static int MODAPI = 1;
	@Override
	public void init(IPMLModLoadAPI loadAPI){
		if(loadAPI.isDirectlyCompatible(MODAPI)){
			loadAPI.getHookRegistrar().register(this.modName, new CallWrapper<BiomeGenBase>(
					new CallData.CallDataFactory()
					.setClass("net.minecraft.world.World")//net.minecraft.world.World.getBiomeGenForCoordsBody(int, int)
					.setMethodName("getBiomeGenForCoordsBody")
					.setMethodDesc("(II)Lnet.minecraft.world.biome.BiomeGenBase;")
					.setTiming(WrapTiming.Early)
					.create())
					{
				@Override
				protected BiomeGenBase call2(HookListenerHelper<BiomeGenBase> hooks) throws Throwable {
					System.out.println("getting a biome!!! :D");//this causes a lot of lag, but it shows that it works.
					return hooks.callNext();
						//as an alternative, you can change the effective biome. This doesn't change the actual biome, but makes the game think this is what it is
					//return BiomeGenBase.iceMountains;
					//I chose this example at random. I don't do this with my mods, and I don't suggest anybody else does, either.
				}
			});
			System.out.println("Example PMLMod example hook added");
		}else{
			System.out.println("Example PMLMod example hook *NOT* added. It's not compatible D:>");
		}
	}

	@Override
	protected Object interpretMessage(IPMLMod fromMod, Object message) {
		return Void.class;
	}

}
