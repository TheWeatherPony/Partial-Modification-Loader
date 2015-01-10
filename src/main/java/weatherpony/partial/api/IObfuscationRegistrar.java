package weatherpony.partial.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public interface IObfuscationRegistrar<ClassDenote,FieldDenote,MethodDenote> {
	public interface IObfuscationDenoter<ClassDenote,FieldDenote,MethodDenote>{
		public ClassDenote denoteClass(String className);

		public FieldDenote denoteField(String className, String fieldName);

		public MethodDenote denoteMethod(String className, String methodName,	String methodDesc);
	}
	public void registerClassRelation(ClassDenote from, ClassDenote to);
	public void registerFieldRelation(FieldDenote from, FieldDenote to);
	public void registerMethodRelation(MethodDenote from, MethodDenote to);
	public IObfuscationDenoter<ClassDenote,FieldDenote,MethodDenote> getDenoter();
	public static class TypicalParser{
		public static void parseLine(String line, IObfuscationRegistrar registrar){
			//PK: . net/minecraft/src
			//CL: net/minecraft/world/gen/feature/WorldGenFire net/minecraft/world/gen/feature/WorldGenFire
			//FD: net/minecraft/client/renderer/entity/RenderCreeper/creeperTextures net/minecraft/client/renderer/entity/RenderCreeper/field_110830_f
			//MD: net/minecraft/world/ColorizerFoliage/getFoliageColorBirch ()I net/minecraft/world/ColorizerFoliage/func_77469_b ()I
			line = line.replace('/', '.');
			String[] parts = line.split(" ");
			String part1 = parts[0];
			if(part1.equals("PK:")){
				//ignore - unneeded
			}else if(part1.equals("CL:")){
				Object class1 = registrar.getDenoter().denoteClass(parts[1]);
				Object class2 = registrar.getDenoter().denoteClass(parts[2]);
				registrar.registerClassRelation(class1, class2);
				registrar.registerClassRelation(class2, class1);
			}else if(part1.equals("FD:")){
				String[] field1b = breakClassAndMemeber(parts[1]);
				Object field1 = registrar.getDenoter().denoteField(field1b[0], field1b[1]);
				String[] field2b = breakClassAndMemeber(parts[2]);
				Object field2 = registrar.getDenoter().denoteField(field2b[0], field2b[1]);
				registrar.registerFieldRelation(field1, field2);
				registrar.registerFieldRelation(field2, field1);
			}else if(part1.equals("MD:")){
				String[] method1b = breakClassAndMemeber(parts[1]);
				String[] method2b = breakClassAndMemeber(parts[3]);
				Object method1 = registrar.getDenoter().denoteMethod(method1b[0], method1b[1], parts[2]);
				Object method2 = registrar.getDenoter().denoteMethod(method2b[0], method2b[1], parts[4]);
				registrar.registerMethodRelation(method1, method2);
				registrar.registerMethodRelation(method2, method1);
			}else{
				throw new RuntimeException(line);
			}
		}
		public static String[] breakClassAndMemeber(String in){
			String[] ret = new String[2];
			String[] temp = in.split("[.]");
			StringBuilder builder = new StringBuilder();
			String add = "";
			String dot = ".";
			int size = temp.length-1;
			for(int cur=0;cur<size;cur++){
				builder.append(add);
				builder.append(temp[cur]);
				add = dot;
			}
			ret[0] = builder.toString();
			ret[1] = temp[size];
			return ret;
		}
		public static void parseInputStream(InputStream in, IObfuscationRegistrar registrar){
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			try {
				line = reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(line == null)
				throw new RuntimeException();
			while(line != null){
				parseLine(line, registrar);
				try {
					line = reader.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}