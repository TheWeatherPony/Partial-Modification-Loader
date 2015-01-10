package weatherpony.partial.asmedit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;


public class ProxyListing{
	static final ProxyListing instance = new ProxyListing();
	private HashMap<Data, Integer> reverse = new HashMap();
	private ArrayList<Data> direct = new ArrayList();
	private static final String base = "proxyNum";
	private ProxyListing(){
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
			@Override
			public void run(){
				File saveFile = new File("PML_proxyList.txt");
				if(saveFile.exists())
					saveFile.delete();
				try{
					BufferedWriter out = new BufferedWriter(new FileWriter(saveFile));
					out.append("The method proxies generated last runtime by PML corresponded to methods as follows. (this is in case something crashed)");
					out.newLine();
					int size = direct.size();
					for(int cur=0;cur<size;cur++){
						out.append(base).append(Integer.toString(cur+1)).append(": ").append(direct.get(cur).toString());
						out.newLine();
					}
					out.close();
				}catch(Throwable e){
					System.err.println("PML Proxy list was unable to save to file. Printing info below instead:");
					PrintStream out = System.err;
					out.append("    The method proxies generated this runtime by PML corresponded to methods as follows. (this is in case something crashed)");
					out.println();
					int size = direct.size();
					for(int cur=0;cur<size;cur++){
						out.append("    ");
						out.append(base).append(Integer.toString(size)).append(": ").append(direct.get(cur).toString());
						out.println();
					}
				}
			}
		}));
		
	}
	String generateClassName(String fromClass, String method, String desc){
		Data value = new Data(fromClass, method, desc);
		if(reverse.containsValue(value))
			return base + reverse.get(value);
		int size = direct.size();
			
		reverse.put(value, size);
		direct.add(value);
			
		return base + size;
	}
	public Data getDataFromName(String name){
		if(name.startsWith(base)){
			String end = name.replaceFirst(base, "");
			try{
				return direct.get(Integer.parseInt(end));
			}catch(Exception e){
				
			}
		}
		return null;
	}
	public static final class Data{
		public final String clazz,method,desc;
		Data(String clazz, String method, String desc){
			this.clazz = clazz;
			this.method = method;
			this.desc = desc;
		}
		public String getMethod(){
			return (clazz+method+desc);
		}
		@Override
		public String toString(){
			return getMethod();
		}
		@Override
		public int hashCode(){
			return clazz.hashCode() + method.hashCode() + desc.hashCode();
		}
		@Override
		public boolean equals(Object comp){
			if(comp instanceof Data){
				Data comp2 = (Data)comp;
				return this.clazz.equals(comp2.clazz) && this.method.equals(comp2.method) && this.desc.equals(comp2.desc);
			}else if(comp instanceof String){
				return (clazz+method+desc).equals(comp);
			}else
				return false;
		}
	}
}
