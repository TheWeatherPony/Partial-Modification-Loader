package weatherpony.partial.asmedit;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import weatherpony.partial.CallWrapper;
import weatherpony.partial.internal.GeneralHookManager;

import com.google.common.base.Throwables;

public class HookInjectorTransformer implements IClassTransformer{
	public static final String packagePrefix = "weatherpony/partial/generated/";
	public static final String packagePrefixRaw = packagePrefix.replace('/', '.');
	public static final String savePrefix = "tempGeneratedClassSaveFolder";
	public static final File saveFolderBase = new File(savePrefix);
	
	public static final String WrapTimingInternal = "weatherpony/partial/WrapTiming";
	public static final String WrapTimingInternalObject = "L"+WrapTimingInternal+";";
	public static final int WrapTimingAccess = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_ENUM;
	public static final String WrapTimingProxyValue = "Proxy";
	
	public static final String CallWrapperInternal = Type.getInternalName(CallWrapper.class);
	public static final String CallWrapperInternalObject = "L"+CallWrapperInternal+";";
	public static final String CallWrapperInitDesc = "(Ljava/lang/String;"//class
													+ "Ljava/lang/String;"//method
													+ "Ljava/lang/String;"//description
													+ WrapTimingInternalObject
													+ ")V";//because it's a constructor
	public static final String ThrowableInteral = Type.getInternalName(Throwable.class);
	public static final String ObjectInternal = Type.getInternalName(Object.class);
	public static final String WrappedExceptionInternal = "weatherpony/partial/WrappedException";
	public static final String WrappedExceptionWrappedName = "e";
	
	public static final String HookListingInternal = "weatherpony/partial/internal/GeneralHookManager";//"weatherpony/partial/HookListing";
	public static final String HookListingInternalObject = 'L'+HookListingInternal+';';
	//public static final String HookListingInstanceFieldName = "INSTANCE";
	public static final String HookListingMethod = "rigAndDistributeCall";
	
	public static final String HookListenerHelperInternal = "weatherpony/partial/HookListenerHelper";
	public static final String HookListenerHelperGetParamName = "getParam";
	public static final String HookListenerHelperGetParamDesc = "(I)Ljava/lang/Object;";
	
	public static final String proxyMethodName = "call2";//this is from CallWrapper.class
	public static final String proxyMethodParams_descPart = "(L"+HookListenerHelperInternal+";)";
	public static final String proxyMethodDesc_Syn = proxyMethodParams_descPart+"Ljava/lang/Object;";
	public static final int proxyMethodAcc = Opcodes.ACC_PROTECTED;
	public static final int proxyMethodAcc_Syn = proxyMethodAcc + Opcodes.ACC_BRIDGE + Opcodes.ACC_SYNTHETIC;
	
	public HookInjectorTransformer(){
		deleteFileFolder_recursive(saveFolderBase);
	}
	public static final void deleteFileFolder_recursive(File delete){
		if(delete.exists()){
			if(delete.isDirectory()){
				File[] sub = delete.listFiles();
				for(File each : sub){
					deleteFileFolder_recursive(each);
				}
			}
			delete.delete();
		}
	}
	@Override
	public byte[] transform(String className, String transformedName, byte[] bytes){
		if(bytes == null)
			return bytes;//no code to look at...
		String transformed_slash = transformedName.replace('.', '/');
		ClassReader cr = new ClassReader(bytes);
		ClassNode tree = new ClassNode();
		cr.accept(tree, /*ClassReader.EXPAND_FRAMES | */ClassReader.SKIP_FRAMES);
		if(!transformedName.equals(tree.name.replace('/', '.'))){
			throw new RuntimeException();
		}
		boolean changedAnything = false;
		
		List<MethodNode> methods = tree.methods;
		Iterator<MethodNode> iter = methods.iterator();
		ArrayList<MethodNode> add = new ArrayList();
		
		File classsPath = new File(this.saveFolderBase,transformed_slash);
		//classsPath.mkdirs();
		List<String> proxiesMade = new ArrayList();
		while(iter.hasNext()){
			MethodNode methodnode = iter.next();
			String methodName = methodnode.name;
			String methodDesc = methodnode.desc;
			
			boolean needsToAlter = this.askForHookGeneration(transformedName, methodName, methodDesc);
			if(!needsToAlter)
				continue;
			
			//this method needs to be transformed.
			changedAnything = true;
			add.add(generateReplacement(methodnode, /*tree.name*/transformedName));
			methodnode.name = this.mirrorName(transformedName, methodName, methodDesc);
			methodnode.access = this.publicAccess(methodnode.access);
			
			ClassNode proxy = this.generateProxy(transformed_slash, methodName, methodDesc, (methodnode.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC);
			this.saveProxy(proxy, classsPath);
			proxiesMade.add(proxy.name.replace('/', '.'));
		}
		if(changedAnything){
			tree.methods.addAll(add);//adds the mirrored methods
			
			//add the MethodHandle Lookup
			
				//alters the <clinit> method (adding it if needed) to call the method to add the MethodHandleLookup
				MethodNode clinit = null;
				for(MethodNode each : tree.methods){
					if(each.name.equals("<clinit>")){
						clinit = each;
						break;
					}
				}
				if(clinit == null){
					clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
					tree.methods.add(clinit);
					clinit.instructions.add(new InsnNode(Opcodes.RETURN));
				}
				InsnList il = new InsnList();
				Label l = new Label();
				LabelNode ln = new LabelNode(l);
				il.add(ln);
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, transformed_slash, "registerLookup", "()V"));
				il.add(clinit.instructions);
				clinit.instructions = il;
				
				//now to add the lookup method
				MethodNode lookupMethod = new MethodNode(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, "registerLookup", "()V", null, null);
				tree.methods.add(lookupMethod);
				lookupMethod.visitCode();
				Label l0 = new Label();
				lookupMethod.visitLabel(l0);
				lookupMethod.visitLdcInsn(transformedName);
				lookupMethod.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;");
				lookupMethod.visitMethodInsn(Opcodes.INVOKESTATIC, "weatherpony/partial/internal/GeneralHookManager", "_giveClassLookup", "(Ljava/lang/String;Ljava/lang/invoke/MethodHandles$Lookup;)V");
				Label l1 = new Label();
				lookupMethod.visitLabel(l1);
				lookupMethod.visitInsn(Opcodes.RETURN);
				lookupMethod.visitMaxs(2, 0);
				lookupMethod.visitEnd();
			
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
			tree.accept(cw);
			try {
				Launch.classLoader.addURL(classsPath.toURI().toURL());
			} catch (MalformedURLException e) {
				throw Throwables.propagate(e);
			}
			for(String each : proxiesMade){
				try {
					registerProxy((CallWrapper) Class.forName(each, true, Launch.classLoader).newInstance());
				} catch (Exception e) {
					throw Throwables.propagate(e);
				}
			}
			return cw.toByteArray();
		}
		return bytes;//nothing changed.
	}
	
	public static String mirrorName(String inClass, String method, String desc){
		return "_"+(inClass.replace('.', '_').replace('/', '_'))+"_"+method.replace('<', '_').replace('>', '_')+"_"; //the desc stays the same
	}
	public static String proxyPackageName(String inClass){
		//return packagePrefix+inClass;
		return packagePrefix;
	}
	public static String proxyInternalName(String inClass, String method, String desc){
		return (proxyPackageName(inClass) 
				+ProxyListing.instance.generateClassName(inClass, method, desc));
					
	}
	
	//the code for this method is extracted from the FML's AccessTransformer
	private int publicAccess(int basedon){
		int t = Opcodes.ACC_PUBLIC;
		int ret = (basedon & ~7);
        switch (basedon & 7)
        {
        case Opcodes.ACC_PRIVATE:
            ret |= t;
            break;
        case 0: // default
            ret |= (t != Opcodes.ACC_PRIVATE ? t : 0 /* default */);
            break;
        case Opcodes.ACC_PROTECTED:
            ret |= (t != Opcodes.ACC_PRIVATE && t != 0 /* default */? t : Opcodes.ACC_PROTECTED);
            break;
        case Opcodes.ACC_PUBLIC:
            ret |= (t != Opcodes.ACC_PRIVATE && t != 0 /* default */&& t != Opcodes.ACC_PROTECTED ? t : Opcodes.ACC_PUBLIC);
            break;
        default:
            throw new RuntimeException("The hell?");//this line is modified to be a little less objectionable. but only a little.
        }
        return ret;
	}
	private MethodNode generateReplacement(MethodNode change, String inClass){
		MethodNode mv = new MethodNode(change.access, change.name, change.desc, change.signature, change.exceptions.toArray(new String[change.exceptions.size()]));
		
		mv.visitCode();
		Label l0 = new Label();
		Label l1 = new Label();
		Label l2 = new Label();
		mv.visitTryCatchBlock(l0, l1, l2, WrappedExceptionInternal);
		mv.visitLabel(l0);
		int paramsize = addHookCall(change, mv, inClass, l1);
		addCatchBlock(change, mv, paramsize, l2);
		
		return mv;
	}
	private int addHookCall(MethodNode from, MethodVisitor mv, String inClass, Label normalreturnlabel){
		addgetHookCallInstance(mv);
		//prepares the info about who this hook is
			mv.visitLdcInsn(inClass);
			mv.visitLdcInsn(from.name);
			mv.visitLdcInsn(from.desc);
		
		int ret = addParameterArray(from, mv);//prepares the parameter(+this) info
		addHookCall(mv);
		addReturnAndCast(from, mv, normalreturnlabel);
		return ret;
	}
	private void addgetHookCallInstance(MethodVisitor mv){
		//this is leftover from when I used a public static instance variable
	}
	private void addHookCall(MethodVisitor mv){
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, HookListingInternal, HookListingMethod, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
	}
	/**
	 * this is based on Adel's code on http://stackoverflow.com/questions/13911142/java-method-parameters-values-in-asm
	 * his saved the array as a local variable. mine does not - this version leaves the array on the top of the stack, ready to be used.
	 * mine is also static-sensitive, while his was not. (his was not expecting "this" to be the first parameter)
	 * mine also accepts all types while his didn't - no doubles or longs (even though it looked like it supported them)
	 * 
	 * his was a nice start, at least, and greatly helped me put mine together 
	 */
	private int addParameterArray(MethodNode from, MethodVisitor mv){
		
		Type[] paramTypes = Type.getArgumentTypes(from.desc);
		boolean isNotStatic = (from.access & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC;
		int paramLength = paramTypes.length + (isNotStatic ? 1 : 0);
		
		// Create array with length equal to number of parameters
	    mv.visitIntInsn(Opcodes.BIPUSH, paramLength);
	    mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
	    
	    
	    if(isNotStatic){
		    //add the "this" instance
	    	mv.visitInsn(Opcodes.DUP);//duplicate top object (the array), so it can be seen again later
		    mv.visitInsn(Opcodes.ICONST_0);//put 0 on stack
			mv.visitVarInsn(Opcodes.ALOAD, 0);//get "this"
			mv.visitInsn(Opcodes.AASTORE);//put top Object into the second-top position of third-top array
	    }else{
	    	//add null, since this is static.
	    	//already done, since the elements initialize to null :)
	    }
	    
	    
	    //mv.visitVarInsn(Opcodes.ASTORE, paramLength);
	    
	    // Fill the created array with method parameters
	    //int i = 0;
		int i = 0;
		int storeIndex = 1;//This is my biggest change. ignore the first position, which is reserved for "this"
		if(isNotStatic)
			i = 1;//skip "this", otherwise the rest of this stuff will go screwy.
			
	    for (Type tp : paramTypes) {
	        //mv.visitVarInsn(Opcodes.ALOAD, paramLength);
	    	mv.visitInsn(Opcodes.DUP);
	        mv.visitIntInsn(Opcodes.BIPUSH, storeIndex);

	        if (tp.equals(Type.BOOLEAN_TYPE)) {
	            mv.visitVarInsn(Opcodes.ILOAD, i);
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
	        }
	        else if (tp.equals(Type.BYTE_TYPE)) {
	            mv.visitVarInsn(Opcodes.ILOAD, i);
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
	        }
	        else if (tp.equals(Type.CHAR_TYPE)) {
	            mv.visitVarInsn(Opcodes.ILOAD, i);
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
	        }
	        else if (tp.equals(Type.SHORT_TYPE)) {
	            mv.visitVarInsn(Opcodes.ILOAD, i);
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
	        }
	        else if (tp.equals(Type.INT_TYPE)) {
	            mv.visitVarInsn(Opcodes.ILOAD, i);
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
	        }
	        else if (tp.equals(Type.LONG_TYPE)) {
	            mv.visitVarInsn(Opcodes.LLOAD, i);
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
	            i++;
	        }
	        else if (tp.equals(Type.FLOAT_TYPE)) {
	            mv.visitVarInsn(Opcodes.FLOAD, i);
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
	        }
	        else if (tp.equals(Type.DOUBLE_TYPE)) {
	            mv.visitVarInsn(Opcodes.DLOAD, i);
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
	            i++;
	        }
	        else
	            mv.visitVarInsn(Opcodes.ALOAD, i);

	        mv.visitInsn(Opcodes.AASTORE);
	        i++;
	        storeIndex++;
	    }
	    return i;
	}
	
	private void addReturnAndCast(MethodNode from, MethodVisitor mv, Label returnlabel){
		Type returntype = Type.getReturnType(from.desc);
		if(returntype.equals(Type.VOID_TYPE)){//void
			mv.visitInsn(Opcodes.POP);//get rid of it.
			mv.visitLabel(returnlabel);
			mv.visitInsn(Opcodes.RETURN);
		}else if(returntype.equals(Type.BOOLEAN_TYPE)){//boolean
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
			mv.visitLabel(returnlabel);
			mv.visitInsn(Opcodes.IRETURN);
		}else if(returntype.equals(Type.CHAR_TYPE)){//char
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");
		}else if(returntype.equals(Type.SHORT_TYPE)){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");
			mv.visitLabel(returnlabel);
			mv.visitInsn(Opcodes.IRETURN);
		}else if(returntype.equals(Type.INT_TYPE)){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
			mv.visitLabel(returnlabel);
			mv.visitInsn(Opcodes.IRETURN);
		}else if(returntype.equals(Type.FLOAT_TYPE)){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F");
			mv.visitLabel(returnlabel);
			mv.visitInsn(Opcodes.FRETURN);
		}else if(returntype.equals(Type.LONG_TYPE)){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
			mv.visitLabel(returnlabel);
			mv.visitInsn(Opcodes.LRETURN);
		}else if(returntype.equals(Type.DOUBLE_TYPE)){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");
			mv.visitLabel(returnlabel);
			mv.visitInsn(Opcodes.DRETURN);
		}else{//it wants an Object or Object[]... or something crazy that the API can't figure out.
			mv.visitTypeInsn(Opcodes.CHECKCAST, returntype.getInternalName());//cast it (if possible) to the right type
			mv.visitLabel(returnlabel);
			mv.visitInsn(Opcodes.ARETURN);
		}
		
	}
	
	private void addCatchBlock(MethodNode from, MethodVisitor mv, int nextVar, Label catchlabel){
		List<String> exceptions = from.exceptions;
		int exceptioncount = exceptions.size();
		
		mv.visitLabel(catchlabel);
		mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {WrappedExceptionInternal});
		int wrappedExceptionVar = nextVar++;//"2"
		mv.visitVarInsn(Opcodes.ASTORE, wrappedExceptionVar);
		Label l3 = new Label();
		mv.visitLabel(l3);
		mv.visitVarInsn(Opcodes.ALOAD, wrappedExceptionVar);
		mv.visitFieldInsn(Opcodes.GETFIELD, WrappedExceptionInternal, WrappedExceptionWrappedName, "Ljava/lang/Throwable;");
		int unwrappedExceptionVar = nextVar++;//"3"
		mv.visitVarInsn(Opcodes.ASTORE, unwrappedExceptionVar);
		
		Label next = new Label();
		for(int cur=0;cur<exceptioncount;cur++){
			String thisException = exceptions.get(cur);
			mv.visitLabel(next);
			mv.visitVarInsn(Opcodes.ALOAD, unwrappedExceptionVar);
			mv.visitTypeInsn(Opcodes.INSTANCEOF, thisException);
			next = new Label();
			mv.visitJumpInsn(Opcodes.IFEQ, next);
			Label iflabel = new Label();
			mv.visitLabel(iflabel);
			mv.visitVarInsn(Opcodes.ALOAD, unwrappedExceptionVar);
			mv.visitTypeInsn(Opcodes.CHECKCAST, thisException);
			mv.visitInsn(Opcodes.ATHROW);
		}
		mv.visitLabel(next);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(Opcodes.ALOAD, unwrappedExceptionVar);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/google/common/base/Throwables", "propagate", "(Ljava/lang/Throwable;)Ljava/lang/RuntimeException;");
		mv.visitInsn(Opcodes.ATHROW);
	}
	
	private void addFrameDataForCatch(MethodVisitor mv, int number){
		if(number == 0)
			return;
		if(number == 1)
			mv.visitFrame(Opcodes.F_APPEND,2, new Object[] {this.WrappedExceptionInternal, "java/lang/Throwable"}, 0, null);
		else
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
	}
	
	private ClassNode generateProxy(String inClass, String method, String desc, boolean isStatic){
		String focusedGenericReturn = getObjectForGeneric_byReturn(desc);//focusedGenericReturn is the fully-qualified, internal, Object-based name. for example: Lnet/minecraft/world/biome/BiomeGenBase;
		String focusedGenericReturnInternal = getObjectForGeneric_byReturn_internal(desc);
		ClassNode cw = new ClassNode();
		MethodVisitor mv;

		String proxyClassName = this.proxyInternalName(inClass, method, desc);
		System.out.println(focusedGenericReturn);
		String HookListenerHelperGenFocused = "L"+CallWrapperInternal+"<"+focusedGenericReturn+">;";/*"Lweatherpony/partial/CallWrapper<Lnet/minecraft/world/biome/BiomeGenBase;>;"*/
		cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, proxyClassName, HookListenerHelperGenFocused, CallWrapperInternal, null);

		cw.visitSource(null, null);

		{//constructor
			mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);//constructor. making it empty for simplicity
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitLdcInsn(inClass.replace('/', '.'));
			mv.visitLdcInsn(method);
			mv.visitLdcInsn(desc);
			mv.visitFieldInsn(Opcodes.GETSTATIC, WrapTimingInternal, WrapTimingProxyValue, WrapTimingInternalObject);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, CallWrapperInternal, "<init>", CallWrapperInitDesc);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitInsn(Opcodes.RETURN);
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitLocalVariable("this", "L"+cw.name+";", null, l0, l2, 0);
			mv.visitMaxs(5, 1);
			mv.visitEnd();
		}
		{//the call2 method
			mv = cw.visitMethod(proxyMethodAcc, proxyMethodName, 
					proxyMethodParams_descPart+focusedGenericReturn,
					"(L"+HookListenerHelperInternal+"<"+focusedGenericReturn+">;)"+focusedGenericReturn,
					new String[] { ThrowableInteral });
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(16, l0);
			//new GeneratorAdapter().loadArgArray();
			Type[] paramTypes = Type.getArgumentTypes(desc);
			int paramSize = paramTypes.length;
			int index = 1;
			int pos = 0;
			
			if(isStatic){
				//index = 1;//skip the "this", since it's static (it's null)
			}else{
				mv.visitVarInsn(Opcodes.ALOAD, 1);
				mv.visitInsn(Opcodes.ICONST_0);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, HookListenerHelperInternal, HookListenerHelperGetParamName, HookListenerHelperGetParamDesc);//get the variable
				//mv.visitTypeInsn(Opcodes.CHECKCAST, "net/minecraft/world/World");
				mv.visitTypeInsn(Opcodes.CHECKCAST, inClass);
			}
			for(;pos<paramSize;index++,pos++){
				Type tp = paramTypes[pos];
				mv.visitVarInsn(Opcodes.ALOAD, 1);//push the helper
				mv.visitIntInsn(Opcodes.BIPUSH, index);//push the index
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, HookListenerHelperInternal, HookListenerHelperGetParamName, HookListenerHelperGetParamDesc);//get the variable
				//cast it to the right type
				if (tp.equals(Type.BOOLEAN_TYPE)) {
					mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
		        }
		        else if (tp.equals(Type.BYTE_TYPE)) {
		        	mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B");
		        }
		        else if (tp.equals(Type.CHAR_TYPE)) {
		        	mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");
		        }
		        else if (tp.equals(Type.SHORT_TYPE)) {
		        	mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");
		        }
		        else if (tp.equals(Type.INT_TYPE)) {
		        	mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
		        }
		        else if (tp.equals(Type.LONG_TYPE)) {
		        	mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
		        }
		        else if (tp.equals(Type.FLOAT_TYPE)) {
		        	mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F");
		        }
		        else if (tp.equals(Type.DOUBLE_TYPE)) {
		        	mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");
		        }
		        else{
		        	String wanted = tp.getInternalName();
		        	//if(!wanted.equals(ObjectInternal))//this is for a slight optimization
		        		mv.visitTypeInsn(Opcodes.CHECKCAST, tp.getInternalName());
		        	
		        }
			}
			//call the method, and call it the right way
			mv.visitMethodInsn((isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL), inClass, mirrorName(inClass, method, desc), desc);
			//return the returned value as an Object (of the right type)
			Type returntype = Type.getReturnType(desc);
			
			if (returntype.equals(Type.BOOLEAN_TYPE)) {
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
	        }
	        else if (returntype.equals(Type.BYTE_TYPE)) {
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
	        }
	        else if (returntype.equals(Type.CHAR_TYPE)) {
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
	        }
	        else if (returntype.equals(Type.SHORT_TYPE)) {
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
	        }
	        else if (returntype.equals(Type.INT_TYPE)) {
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
	        }
	        else if (returntype.equals(Type.LONG_TYPE)) {
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
	        }
	        else if (returntype.equals(Type.FLOAT_TYPE)) {
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
	        }
	        else if (returntype.equals(Type.DOUBLE_TYPE)) {
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
	        }else if(returntype.equals(Type.VOID_TYPE)){
	        	mv.visitInsn(Opcodes.ACONST_NULL);
	        }
	        else{}
	        mv.visitTypeInsn(Opcodes.CHECKCAST, focusedGenericReturnInternal);
			mv.visitInsn(Opcodes.ARETURN);

			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "L"+proxyClassName+";", null, l0, l1, 0);
			mv.visitLocalVariable("hooks", CallWrapperInternalObject, HookListenerHelperGenFocused, l0, l1, 1);
			//mv.visitMaxs(4, 2);
			mv.visitEnd();
			
		}
		{//the synthetic call2 method
			mv = cw.visitMethod(proxyMethodAcc_Syn, proxyMethodName, proxyMethodDesc_Syn, null, new String[] { ThrowableInteral });
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ALOAD, 1);
			//mv.visitTypeInsn(CHECKCAST, "weatherpony/partial/HookListenerHelper");//absolutely useless.
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, proxyClassName, proxyMethodName, proxyMethodParams_descPart+focusedGenericReturn);
			mv.visitInsn(Opcodes.ARETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		cw.visitEnd();
		return cw;
	}
	String getObjectForGeneric_byReturn(String desc){//so it is a non-primative, and has the 'L' and ';', and the '[' if it's an array
		Type type = Type.getReturnType(desc);
		
		int sort = type.getSort();
		switch(sort){
		case Type.METHOD:
			throw new RuntimeException();//methods should be in this
		case Type.OBJECT:
		case Type.ARRAY://arrays are objects, even if it's an array of primitives
			return type.getDescriptor();
		default://primitive
			{
				char descp = type.getDescriptor().charAt(0);
				String base;
                if (descp == 'I'){
                    base = "Integer";
                }else if(descp == 'V'){
                    base = "Void";
                }else if(descp == 'Z'){
                    base = "Boolean";
                }else if(descp == 'B'){
                	base = "Byte";
                }else if(descp == 'C') {
                	base = "Character";
                }else if(descp == 'S'){
                	base = "Short";
                }else if(descp == 'D'){
                	base = "Double";
                }else if(descp == 'F'){
                	base = "Float";
                }else /*if(descp == 'J')*/{
                	base = "Long";
                }
                return 'L'+"java/lang/"+base+';';
			}
		}
	}
	String getObjectForGeneric_byReturn_internal(String desc){//so it is a non-primative, and has the 'L' and ';', and the '[' if it's an array
		Type type = Type.getReturnType(desc);
		
		int sort = type.getSort();
		switch(sort){
		case Type.METHOD:
			throw new RuntimeException();//methods should be in this
		case Type.OBJECT:
		case Type.ARRAY://arrays are objects, even if it's an array of primitives
			return type.getInternalName();
		default://primitive
			{
				char descp = type.getDescriptor().charAt(0);
				String base;
	            if (descp == 'I'){
	                base = "Integer";
	            }else if(descp == 'V'){
	                base = "Void";
	            }else if(descp == 'Z'){
	                base = "Boolean";
	            }else if(descp == 'B'){
	            	base = "Byte";
	            }else if(descp == 'C') {
	            	base = "Character";
	            }else if(descp == 'S'){
	            	base = "Short";
	            }else if(descp == 'D'){
	            	base = "Double";
	            }else if(descp == 'F'){
	            	base = "Float";
	            }else /*if(descp == 'J')*/{
	            	base = "Long";
	            }
	            return "java/lang/"+base;
			}
		}
	}
	void saveProxy(ClassNode forClass, File classsPath){
		File save = new File(classsPath, forClass.name.replace('.', '/')+".class");
		File saveParent = save.getParentFile();
		if(!saveParent.exists())
			saveParent.mkdirs();
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		forClass.accept(cw);
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(save));
			out.write(cw.toByteArray());
			out.close();
		} catch (Exception e) {
			Throwables.propagate(e);
		}
	}
	void registerProxy(String forClass, String method, String desc, CallWrapper proxy){
		GeneralHookManager.ASM_giveProxy(forClass, method, desc, proxy);
	}
	void registerProxy(CallWrapper proxy){
		registerProxy(proxy.data.inClass, proxy.data.methodName, proxy.data.methodDesc, proxy);
	}
	boolean askForHookGeneration(String forClass, String method, String desc){
		return GeneralHookManager.ASM_prep(forClass, method, desc);
	}
}
