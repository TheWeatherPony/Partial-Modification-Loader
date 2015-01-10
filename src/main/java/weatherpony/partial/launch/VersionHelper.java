package weatherpony.partial.launch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.launchwrapper.Launch;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.google.common.base.Throwables;

public class VersionHelper {
	static final String regex = ".*[0-9]+\\.[0-9]+";//<anything>#.# //yes, it does actually need that ".*". I tried without it and crashed. Silly Java not following the full rules of regex...
	//static final String clientClass = "net/minecraft/client/Minecraft.class";
		//the client class is obfuscated. got to try and find it
	static final String clientMainClass = "net/minecraft/client/main/Main.class";
	static final String serverClass = "net/minecraft/server/MinecraftServer.class";
	private static final int ROOTSIZE = 2;
	public VersionHelper(){
		boolean client;
		String sidedClass;
		//Object clientClassURL = Launch.classLoader.findResource(clientClass);
		Object clientMainClassURL = Launch.classLoader.findResource(clientMainClass);
		if(clientMainClassURL == null){
			Object serverClassURL = Launch.classLoader.findResource(serverClass);
			if(serverClassURL == null){
				throw new RuntimeException();
			}else{
				client = false;
				sidedClass = serverClass;
			}
		}else{
			client = true;
			String clientClass = null;
			
			try {
				ClassReader cr = new ClassReader(Launch.classLoader.getResourceAsStream(clientMainClass));
				ClassNode main = new ClassNode();
				cr.accept(main, ClassReader.EXPAND_FRAMES);
				MethodNode mainmethod = null;
				Iterator<MethodNode> iter = main.methods.iterator();
				while(iter.hasNext()){
					MethodNode next = iter.next();
					if(next.name.equals("main"))
						if(mainmethod == null)
							mainmethod = next;
						else
							throw new RuntimeException();
				}
				iter = null; //cleanup
				AbstractInsnNode insnnode = mainmethod.instructions.getLast();
				boolean foundMarker = false;
				while(insnnode != null){
					if(insnnode instanceof LdcInsnNode){
						LdcInsnNode ldc = (LdcInsnNode)insnnode;
						if(ldc.cst instanceof String){
							if(((String)ldc.cst).equals("Client thread")){
								foundMarker = true;
								insnnode = insnnode.getNext().getNext();//should be the third to last. but doing it this way in case they change stuff...
								break;
							}
						}
					}
					insnnode = insnnode.getPrevious();
				}
				/*
				818  ldc_w <String "Client thread"> [263]
			    821  invokevirtual java.lang.Thread.setName(java.lang.String) : void [266]
			    824  aload 34
			    826  invokevirtual net.minecraft.client.Minecraft.run() : void [269]
			    829  return
			    */
				if(foundMarker){
					if(insnnode instanceof LabelNode){
						insnnode = insnnode.getNext().getNext();
					}
					if(insnnode instanceof VarInsnNode){
						int variableNumber = ((VarInsnNode)insnnode).var;
						System.out.println("variable number: "+variableNumber);
						foundMarker = false;
						while(insnnode != null){
							insnnode = insnnode.getPrevious();
							if(insnnode instanceof VarInsnNode){
								VarInsnNode test = (VarInsnNode)insnnode;
								if(test.getOpcode() == Opcodes.ASTORE && variableNumber == test.var){
									//found the assignment :D
									foundMarker = true;
									insnnode = insnnode.getPrevious();
									break;
								}
							}
						}
						if(foundMarker){
							/*
							724  invokespecial net.minecraft.client.Minecraft(net.minecraft.util.Session, int, int, boolean, boolean, java.io.File, java.io.File, java.io.File, java.net.Proxy, java.lang.String) [221]
    						727  astore 34
							*/
							if(insnnode instanceof MethodInsnNode){
								MethodInsnNode test = (MethodInsnNode) insnnode;
								if(test.getOpcode() == Opcodes.INVOKESPECIAL){
									//found it. finally. :} *wipes forehead*
									clientClass = test.owner+".class";
									
								}else{
									throw new RuntimeException();
								}
							}else{
								throw new RuntimeException();
							}
						}else{
							throw new RuntimeException();
						}
					}else{
						throw new RuntimeException();
					}
				}else{
					throw new RuntimeException();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			sidedClass = clientClass;
		}
		System.out.println("about to look for MC Version string in class '"+sidedClass+'\'');
		
		String foundVersionString = "";
		
		try {
			ClassReader cr = new ClassReader(Launch.classLoader.getResourceAsStream(sidedClass));
			MCVersionFinder finder = new MCVersionFinder();
			cr.accept(finder, ClassReader.EXPAND_FRAMES | ClassReader.SKIP_DEBUG);
			foundVersionString = finder.getVersionString();
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
		
		this.MCVersion = client ? //"Minecraft #.#<.#>" ?
				foundVersionString.split("\\s")[1]//split it at the space character, and take the second part
				: foundVersionString;
				
		final String[] mcVsplits = this.MCVersion.split("\\.");
		String mcvroot = "";
		{
			String add = "";
			String dot = ".";
			for(int cur=0;cur<ROOTSIZE && cur<mcVsplits.length;cur++){
				mcvroot += add;
				add = dot;
				mcvroot += mcVsplits[cur];
			}
		}
		this.MCVersionRoot = mcvroot;
	}
	public final String MCVersion;
	public final String MCVersionRoot;
	
	static class MCVersionFinder extends ClassVisitor{
		public MCVersionFinder() {
			super(Opcodes.ASM4);
			this.methodvisitorinstance = new MCVersionFinder_MethodVisitor(this);
		}
		MCVersionFinder_MethodVisitor methodvisitorinstance;
		List<String> maybes = new ArrayList();
		String getVersionString(){
			String ret = null;
			for(String each : maybes){
				if(each.equals("##0.00"))//the decimal format from Minecraft.class
					continue;//ignore that one.
				if(ret == null){
					ret = each;
				}else{
					if(!ret.equals(each))
						throw new RuntimeException();
				}
			}
			return ret;
		}
		void addMaybe(String found){
			this.maybes.add(found);
		}
		@Override
		public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5) {
		}
		@Override
		public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
			return null;
		}
		@Override
		public void visitEnd() {
		}
		@Override
		public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
		}
		@Override
		public void visitOuterClass(String arg0, String arg1, String arg2) {
		}
		@Override
		public void visitSource(String arg0, String arg1) {
		}
		
		@Override
		public MethodVisitor visitMethod(int arg0, String arg1, String arg2, String arg3, String[] arg4) {
			return methodvisitorinstance;
		}
		@Override
		public FieldVisitor visitField(int arg0, String arg1, String arg2, String arg3, Object arg4) {
			return null;//currently the version string is only found in methods
		}
		class MCVersionFinder_MethodVisitor extends MethodVisitor{
			public MCVersionFinder_MethodVisitor(MCVersionFinder parent) {
				super(Opcodes.ASM4);
				this.parent = parent;
			}
			MCVersionFinder parent;
			public void visitLdcInsn(Object cst) {
		        if(cst instanceof String){
		        	String load = (String)cst;
		        	if(load.matches(regex))
		        		this.parent.addMaybe(load);
		        }
		    }
		}
	}
}
