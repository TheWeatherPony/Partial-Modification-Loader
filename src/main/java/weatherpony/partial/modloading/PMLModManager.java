package weatherpony.partial.modloading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.apache.commons.io.FileUtils;

import weatherpony.partial.api.ExternalLibraryDependancy;
import weatherpony.partial.api.IPMLFactoryLoadAPI;
import weatherpony.partial.api.IPMLMod;
import weatherpony.partial.api.IPMLModFactory;
import weatherpony.partial.api.IPMLModLoadAPI;
import weatherpony.partial.internal.GeneralHookManager;

import com.google.common.base.Throwables;

public class PMLModManager{
	private List<IPMLMod> mods;
	public PMLModManager(IPMLFactoryLoadAPI factoryLoadAPI, IPMLModLoadAPI modLoadAPI){
		System.out.println("PML Mod Manager being created");
		LaunchClassLoader lcl = Launch.classLoader;
		List<File> likelyCandidates = new ArrayList();
		ArrayList<IPMLMod> mods = new ArrayList();
		File modsFolder = new File("mods");
		likelyCandidates.add(null);//this is for mods in a Dev. enviornment
		if(modsFolder.exists()){
			File[] subFiles = modsFolder.listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name){
					return name.toLowerCase().endsWith(".pmlm");//.Partial Modification Loader Mod
				}
			});
			if(subFiles != null)
				likelyCandidates.addAll(Arrays.asList(subFiles));
		}
		modsFolder = new File("PMLMods");
		if(!modsFolder.exists())
			modsFolder.mkdir();
		{
			File[] subFiles = modsFolder.listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name){
					if(name.toLowerCase().endsWith(".pmlm"))//.Partial Modification Loader Mod
						return true;
					return new File(dir, name).isFile();
					/*File file = new File(dir, name);
					if(file.isFile())
						return true;
					//it's a directory
					if(name.equals(PMLActualTweak.MCVersionShort)||name.equals(PMLActualTweak.MCVersion))
						return true;
					if(name.matches("[0-9]+[.][0-9]+"))
						return false;
					return //...
							true;*/
				}
			});
			likelyCandidates.addAll(Arrays.asList(subFiles));
		}
		
		Iterator<File> iter = likelyCandidates.iterator();
		ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
		while(iter.hasNext()){
			File each = iter.next();
			System.out.println("PML Mod Manager is looking at "+(each == null ? "<main path>" : each.getAbsolutePath()));
			BufferedReader guideFile = null;
			ZipFile zip = null;
			if(each == null){
				InputStream ins = lcl.getResourceAsStream("pml.info");
				if(ins != null){
					guideFile = new BufferedReader(new InputStreamReader(ins));
					System.out.println("opening pml.info from path");
				}
			}else if(each.isDirectory()){
				File guide = new File(each, "pml.info");
				if(guide.exists() && guide.isFile()){
					try {
						guideFile = new BufferedReader(new FileReader(guide));
						System.out.println("opening pml.info from directory: "+each.getAbsolutePath());
					}catch (FileNotFoundException e){
						continue;
					}
				}
			}else{//each.isFile()
				try{
					zip = new ZipFile(each);
				}catch (Exception e){
					continue;
				}
				ZipEntry entry = zip.getEntry("pml.info");
				if(entry != null)
					try {
						guideFile = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
						System.out.println("opening pml.info from .pmlm: "+each.getAbsolutePath());
					} catch (IOException e) {
						if(zip != null)
							try{
								zip.close();
							}catch(Exception e1){
							}
					}
			}
			if(guideFile != null){
				try {
					boolean hasReadAtLeastOne = false;
					List<IPMLModFactory> factories = new ArrayList();
					while(guideFile.ready()){
						String factoryClass = guideFile.readLine();
						System.out.println("PML Mod Manager read about Factory '"+factoryClass+"'");
						
						if(factoryClass != null){
							lcl.addTransformerExclusion(factoryClass);
							if(each != null && !hasReadAtLeastOne){
								try {
									lcl.addURL(each.toURI().toURL());
								} catch (MalformedURLException e) {
									e.printStackTrace();
									continue;
								}
							}
							hasReadAtLeastOne = true;
							try{
								factories.add((weatherpony.partial.api.IPMLModFactory)(Class.forName(factoryClass).newInstance()));
								System.out.println("PML Mod Manager found mod factory \""+factoryClass+"\" in: "+(each == null? "<main path>" : each.getAbsolutePath()));
							}catch(Exception e){
								e.printStackTrace();
								continue;
							}
						}
					}
					if(!hasReadAtLeastOne){
						System.out.println("PML Mod Manager found a PML-based mod, but couldn't figure out anything about it. :/ (in: "+(each == null? "<main path>" : each.getAbsolutePath())+" )");
					}else{
						for(IPMLModFactory factory : factories){
							factory.preinit(factoryLoadAPI);
							ExternalLibraryDependancy[] libs = factory.externalDependancies();
							if(libs != null){
								for(ExternalLibraryDependancy lib : libs)
									loadExternalDependancy(lib);
							}
							File[] wishes = factory.loadModFrom(each);
							if(wishes != null){
								for(File eachfile : wishes){
									try{
										lcl.addURL(eachfile.toURI().toURL());
									}catch(Exception e){
										
									}
								}
							}
							String[] noASM = factory.noASMOn_prefixs();
							if(noASM != null){
								for(String eachnoasm : noASM){
									lcl.addTransformerExclusion(eachnoasm);
								}
							}
							IPMLMod[] newmods = factory.loadMod();
							if(newmods != null){
								mods.addAll(Arrays.asList(newmods));
							}
						}
					}
				}catch (IOException e){
					continue;
				}finally{
					if(zip != null)
						try{
							zip.close();
						}catch(IOException e){
						}
				}
			}
			
		}
		
		this.mods = Collections.unmodifiableList(mods);
		
		for(IPMLMod eachmod : mods){
			ExternalLibraryDependancy[] libs = eachmod.externalDependancies();
			if(libs != null){
				for(ExternalLibraryDependancy lib : libs)
					loadExternalDependancy(lib);
			}
		}
		for(IPMLMod eachmod : mods){
			//eachmod.offerObfuscationPatternConstructor(helper);
			eachmod.givePMLMods(this.mods);
		}
		/*for(IPMLMod eachmod : mods){
			eachmod.giveFMLCoreMods(fmlcoremods);
		}*///not yet ready
		for(IPMLMod eachmod : mods){
			GeneralHookManager.addMod(eachmod.modName, true);
			eachmod.init(modLoadAPI);
		}
	}
	public void giveFMLMods(List<Object> fmlmods){
		for(IPMLMod eachmod : mods){
			eachmod.giveFMLMods(fmlmods);
		}
	}
	
	static final File externalLibraryFolder = new File("PML_downloadedLibs");
	public static void loadExternalDependancy(ExternalLibraryDependancy lib){
		//first, check if the library is already downloaded...
		File libFile = new File(new File(externalLibraryFolder,lib.name),lib.version+".jar");
		//the subfolders will be made by FileUtils.copyURLToFile, if they don't exist
		if(!libFile.exists()){
			//the library is not already downloaded. need to fetch it, if we can
			URL download = null;
			try {
				download = new URL(lib.downloadURL);
			} catch (MalformedURLException e) {
				System.out.println("PML: a mod asked to download an external dependancy, but supplied a bad download URL ("+lib.name+'-'+lib.version+'['+lib.downloadURL+"])");
				Throwables.propagate(e);
			}
			try {
				FileUtils.copyURLToFile(download, libFile, 30000, 5000);//wait no more than 30 seconds while trying to connect, and 5 seconds if the connection is broken
			} catch (IOException e) {
				System.out.println("PML: a mod is tying to download an external dependancy, but can't. If you aren't connected to the internet, then please do. If you are connected, please try again later. If problems persist, please talk to the mod maker.");
				Throwables.propagate(e);
			}
		}
		//at this point, the libarary was either successfully downloaded, or it was already available.
		try {
			Launch.classLoader.addURL(libFile.toURI().toURL());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
