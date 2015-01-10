package weatherpony.util.launchassist;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import com.google.common.base.Throwables;

/**
 * See LoadLastTweaker for answers to "Why?" and any complaints of idiocy.</br>
 * </br>
 * To set this up, call the setup method. This should be done by the owner of the jar/zip that manages this,
 * and should only be called once, ever.</br>
 * To use this, call the get method, which will setup all the internal stuff. Use the returned instance 
 * to add or manipulate the list of IClassTransformer-implementing class names. When all the other ITweaks 
 * have been run through, this will add the registered transformers.
 * 
 * @author The_WeatherPony
 */
public class LoadLastTweakSetup implements Callable<Void>{
	private static final String packagePath = "weatherpony/util/launchassist";
	private static final String lastLoadPrefix = "LoadLastTweaker";
	private static final String zipPathToTweakerBase = packagePath+'/'+lastLoadPrefix;
	private static final String lastLoadNameStart = (zipPathToTweakerBase).replace('/', '.');
	private static ClassNode delayClassNode;
	private static int maxDelays = 20;
	private static int currentDelay = 20;
	private static int maxDelayRounds = 1;
	private static int currentDelayRound = -1;
	private static LoadLastTweakSetup instance;
	private static File loadDirBase;
	public static void setup(File myJar){
		try{
			byte[] bytes = Launch.classLoader.getClassBytes(lastLoadNameStart);
			delayClassNode = new ClassNode();
			ClassReader cr = new ClassReader(bytes);
			cr.accept(delayClassNode, 0);//read normal. I'll only be editing the name, so it doesn't matter...
		}catch(Exception e){
			Throwables.propagate(e);
		}
		loadDirBase = new File("WPUtil_delayedTweaksDir");
		loadDirBase.mkdir();
		loadDirBase.deleteOnExit();
	}
	private LoadLastTweakSetup(LaunchClassLoader loader){
		if(loader == null)
			throw new RuntimeException();
		this.loader = loader;
		lasts = new ArrayList();
		Launch.blackboard.put("LoadLastTweakSetup", this);
	}
	public static LoadLastTweakSetup get(LaunchClassLoader loader){
		if(instance == null)
			instance = new LoadLastTweakSetup(loader);
		return instance;
		
	}
	private static boolean started = false;
	public void start(){
		if(started)
			return;
		started = true;
		onDelay();
	}
	private LaunchClassLoader loader;
	public final ArrayList<String> lasts;
	public Void call(){
		checkDelay();
		return null;
	}
	public static void onDelay(){
		get(null).checkDelay();
	}
	private void checkDelay(){
		//is there anything to do?
		if(lasts.isEmpty()){//nothing to do. why bother?
			return;
		}
		//is it time to finally add them?
		if(parallels() == 1){//if it's one, than it's just this one
			this.finallyAdd();
			return;
		}
		
		((List<String>)Launch.blackboard.get("TweakClasses")).add(
				readyNextDelay()//build the classes and save them, if needed
				);//register the next delay class. I need to register a new one because we can't add the same named tweak twice or delay the tweak in any other manner.
	}
	private static String readyNextDelay(){
		currentDelay++;
		if(currentDelay >= maxDelays){
			//done with these prepared delays
			currentDelayRound++;
			if(currentDelayRound >= maxDelayRounds){
				//will not generate any more.
				throw new RuntimeException();//it is somewhat likely that there's another mod doing what amounts to the same thing
			}else{
				currentDelay = 0;
				//generate the next round
				File prep = prepareNextRound();
				addFileToParentClassLoader(prep);
			}
			
		}
		return getNameForDelay(currentDelayRound, currentDelay);
	}
	private static URLClassLoader originalLoader = null;
	private static Method addURL = null;
	
	private static void addFileToParentClassLoader(File file){
		if(originalLoader == null)
			try{
				originalLoader = ((URLClassLoader)(Launch.blackboard.get("ParentClassLoader")));
				addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
				addURL.setAccessible(true);
			}catch(Exception e){
				throw Throwables.propagate(e);
			}
		try{
			addURL.invoke(originalLoader, file.toURI().toURL());
		}catch(Exception e){
			throw Throwables.propagate(e);
		}
		
	}
	private static File prepareNextRound(){
		File loadFolder = new File(loadDirBase, "Pass"+currentDelayRound);
		if(loadFolder.exists())
			loadFolder.delete();
		File saveFolder = new File(loadFolder, packagePath+currentDelayRound);
		saveFolder.mkdirs();
		for(int curD=0;curD<maxDelays;curD++){
			delayClassNode.name = packagePath+currentDelayRound+'/'+lastLoadPrefix+curD;
			ClassWriter cw = new ClassWriter(0);
			delayClassNode.accept(cw);
			BufferedOutputStream out = null;
			try {
				out = new BufferedOutputStream(new FileOutputStream(new File(saveFolder, lastLoadPrefix+curD+".class")));
				out.write(cw.toByteArray());
			} catch (Exception e) {
				throw Throwables.propagate(e);
			}finally{
				if(out != null)
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}
		return loadFolder;
	}
	private static String getNameForDelay(int round, int step){
		return (packagePath+round+'/'+lastLoadPrefix+step).replace('/', '.');
	}
	private int parallels(){
		return ((List<String>) Launch.blackboard.get("TweakClasses")).size() + ((List<String>) Launch.blackboard.get("Tweaks")).size();
	}
	private void finallyAdd(){
		for(String each : lasts)
			loader.registerTransformer(each);
	}
}
