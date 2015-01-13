package weatherpony.partial.asmedit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.base.Throwables;

import weatherpony.partial.api.IObfuscationHelper.ObfuscationException;
import weatherpony.partial.internal.ClassData;
import weatherpony.partial.internal.ObfuscationHelper3;
import weatherpony.partial.internal.ObfuscationHelper3.InstanceData;
import weatherpony.partial.internal.ObfuscationHelper3.MethodData;
import weatherpony.partial.internal.OverridingManager;
import weatherpony.partial.internal.OverridingManager.OverrideData;
import weatherpony.partial.internal.SuperMethodCorrectionManager;
import weatherpony.partial.launch.PMLActualTweak;
import weatherpony.util.lists.CombinationIterator;

public class MethodInjectionAndSuperCorrectionTransformer implements IClassTransformer {

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		if(bytes == null)
			return null;//no code to look at...
		if(!LaunchLoaderCallCheck.fromLaunchLoader())
			return bytes;
		ClassReader cr = new ClassReader(bytes);
		ClassNode tree = new ClassNode();
		cr.accept(tree, ClassReader.EXPAND_FRAMES);
		if(!transformedName.equals(tree.name.replace('/', '.'))){
			throw new RuntimeException();
		}
		boolean hasChanged = false;
		
		ObfuscationHelper3 obf = (ObfuscationHelper3) (PMLActualTweak.instance.getObfHelper());
		List<String> eachClassSynonymn = obf.classNames.get(name);//original or transformed - doesn't matter. it ends up being the same in the end.
		Collection<String> supers = ClassData.INSTANCE.getAllSupers(transformedName);
		
		for(String eachOfMySynonymns : eachClassSynonymn){
		//add the methods
			List<OverrideData> overrides = getRequiredOverrides(ObfuscationHelper3.unprepareClassName2(eachOfMySynonymns));
			if(overrides == null || overrides.isEmpty())
				//return bytes;
				continue;
			
			loop: for(OverrideData eachoverride : new HashSet<OverrideData>(overrides)){
				String method = eachoverride.method;
				String desc = eachoverride.desc;
				String originallyFrom = eachoverride.originallyFrom;
				
				MethodData finder = obf.getDenoter().denoteMethod(originallyFrom, method, desc);
				finder.unBoil();
				List<InstanceData> params = finder.params;
				int parmCount = params.size();
				List<List<String>> paramList = new ArrayList(parmCount);
				for(InstanceData eachParam : params){
					paramList.add(obf.classNames.get(ObfuscationHelper3.unprepareClassName2(eachParam.className)));
				}
				List<String> returnTypes = obf.classNames.get(ObfuscationHelper3.unprepareClassName2(finder.returnName.className));
				Iterator<List<String>> paramMix = new CombinationIterator(paramList, true);
				while(paramMix.hasNext()){
					List<String> nextParamCombo = paramMix.next();
					for(int cur=0;cur<parmCount;cur++){
						params.get(cur).className = ObfuscationHelper3.prepareForASM(nextParamCombo.get(cur));
					}
					for(String eachReturn : returnTypes){
						finder.returnName.className = ObfuscationHelper3.prepareForASM(eachReturn);
						
						Iterator<MethodData> methodseekingiter = obf.methods.getAllSynonyms(finder.clone());
						while(methodseekingiter.hasNext()){
							MethodData seek = methodseekingiter.next();
							String methodName = seek.methodName;
							String methodDesc = seek.getDescString();

							//well, that was really ugly.
							
							Iterator<MethodNode> miter = tree.methods.iterator();
							while(miter.hasNext()){
								MethodNode next = miter.next();
								if(next.name.equals(method) && next.desc.equals(desc)){
									continue loop;//the hook's already added
								}
							}
							
							//the method doesn't already exist, so now we add it.
							hasChanged = true;
							MethodVisitor mv = tree.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, methodName, methodDesc, null, null);
							GeneratorAdapter ga = new GeneratorAdapter(mv, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, methodName, methodDesc);
							mv.visitCode();
							Label l = new Label();
							mv.visitLabel(l);
							ga.loadThis();
							ga.loadArgs();
							
							
							String previouslyFrom;
							try{
								previouslyFrom = obf.getClassName(eachoverride.previouslyFrom);
							}catch(ObfuscationException e){
								throw Throwables.propagate(e);
							}
							mv.visitMethodInsn(Opcodes.INVOKESPECIAL, previouslyFrom.replace('.', '/'), methodName, methodDesc);//if previouslyFrom is wrong, it will be fixed later on.
							
							//Type ret = Type.getReturnType(desc);
							mv.visitInsn(Type.getReturnType(methodDesc).getOpcode(Opcodes.IRETURN));
							/*if(ret.equals(Type.VOID_TYPE)){
								mv.visitInsn(Opcodes.RETURN);
							}else{
								mv.visitInsn(ret.getOpcode(Opcodes.IRETURN));
							}*/
						}
					}
				}
				/*
				Iterator<MethodData> methodnameiter = obf.methods.getAllSynonyms();
				while(methodnameiter.hasNext()){
					String methodName = methodnameiter.next().getMethodName();
					
				}
				while(classesForMethod.hasNext()){
					String eachMethodClass = classesForMethod.next();
					List<String> eachMethodCo = obf.classNames.get(eachMethodClass);
				}
				~
				Iterator<MethodNode> miter = tree.methods.iterator();
				while(miter.hasNext()){
					MethodNode next = miter.next();
					if(next.name.equals(method) && next.desc.equals(desc)){
						continue loop;
					}
				}
				//the method doesn't already exist, so now we add it.
				hasChanged = true;
				MethodVisitor mv = tree.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, method, desc, null, null);
				GeneratorAdapter ga = new GeneratorAdapter(mv, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, method, desc);
				mv.visitCode();
				Label l = new Label();
				mv.visitLabel(l);
				ga.loadArgs();
				
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, eachoverride.previouslyFrom, method, desc);//if previouslyFrom is wrong, it will be fixed later on.
				
				Type ret = Type.getReturnType(desc);
				if(ret.equals(Type.VOID_TYPE)){
					mv.visitInsn(Opcodes.RETURN);
				}else{
					mv.visitInsn(ret.getOpcode(Opcodes.IRETURN));
				}*/
			}
		}
	//create the method hierarchy
		tree.accept(new MethodFindingReader(transformedName));
		
	//fix the calls to super methods (in the source code, the super methods are dynamic; but in the bytecode, it's static)
		//this fixes both the calls that were just injected and the calls that would have been altered if the added methods were in the source-code directly.
		/*Iterator<MethodNode> miter = tree.methods.iterator();
		while(miter.hasNext()){
			MethodNode mnext = miter.next();
			//mnext.visitMethodInsn(opcode, owner, name, desc);
			Iterator<AbstractInsnNode> iiter = mnext.instructions.iterator();
			while(iiter.hasNext()){
				AbstractInsnNode insn = iiter.next();
				if(insn instanceof MethodInsnNode){
					MethodInsnNode call = (MethodInsnNode) insn;
					if(call.getOpcode() == Opcodes.INVOKESPECIAL){
						call.owner = getMostLikelySuperMethodOwner(transformedName, call.name, call.desc, call.owner);
					}
				}
			}
		}*///FIXME
		
		if(hasChanged){
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);
			tree.accept(cw);
			return cw.toByteArray();
		}
		return bytes;		
	}
	
	List<OverrideData> getRequiredOverrides(String forClass){
		return OverridingManager.getRequiredOverrides(forClass);
	}
	
	static class MethodFindingReader extends ClassVisitor{
		final String lookingAtClass;
		public MethodFindingReader(String forClass){
			super(Opcodes.ASM4);
			this.lookingAtClass = forClass;
		}
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions){
			this.addMethodToKnownList(lookingAtClass, name, desc);
			return null;
		}
		
		void addMethodToKnownList(String fromClass, String method, String desc){
			SuperMethodCorrectionManager._ASM_addMethodData(fromClass, method, desc);
		}
	}
	String getMostLikelySuperMethodOwner(String inClass, String method, String desc, String givenClass){
		return SuperMethodCorrectionManager._ASM_getLastSeenClass(inClass, method, desc, givenClass.replace('/', '.')).replace('.', '/');
	}
}
