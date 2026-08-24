package cz.maxtechnik.opm.client.recipe;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
public final class RecipeFileManager{
	private RecipeFileManager(){
	}
	public record SaveResult(boolean success,File savedFile,String message){
	}
	public static Path getRecipeDir(){
		try{
			String world=OpmConfig.WORLD_NAME.get().trim();
			String dpName=OpmConfig.DATAPACK_NAME.get().trim();
			if(!world.isEmpty()&&!dpName.isEmpty()){
				Path gameDir=Minecraft.getInstance().gameDirectory.toPath();
				Path datapackDir=gameDir.resolve("saves").resolve(world).resolve("datapacks").resolve(dpName);
				if(Files.exists(datapackDir)){
					String rf=OpmConfig.RECIPE_FOLDER.get().trim();
					if(!rf.isEmpty()){
						return datapackDir.resolve("data").resolve(rf).resolve("recipe");
					}
					Path dataDir=datapackDir.resolve("data");
					if(Files.exists(dataDir)){
						try(var stream=Files.list(dataDir)){
							for(Path nsDir: stream.toList()){
								if(Files.isDirectory(nsDir)){
									Path rDir=nsDir.resolve("recipe");
									if(Files.exists(rDir)) return rDir;
								}
							}
						}
					}
					return datapackDir.resolve("data").resolve(dpName).resolve("recipe");
				}
			}
		}catch(Exception ignored){
		}
		return Minecraft.getInstance().gameDirectory.toPath()
				.resolve("config").resolve("opm").resolve("recipes");
	}
	public static SaveResult saveRecipe(String fileName,String json){
		try{
			Path dir=getRecipeDir();
			String safe=fileName.replaceAll("[^a-z0-9_/]","_").toLowerCase(java.util.Locale.ROOT);
			if(safe.isBlank()) safe="recipe";
			Path file=dir.resolve(safe+".json");
			Files.createDirectories(file.getParent());
			Files.writeString(file,json,StandardCharsets.UTF_8);
			return new SaveResult(true,file.toFile(),"Saved!");
		}catch(Exception e){
			return new SaveResult(false,null,"Save failed: "+e.getMessage());
		}
	}
	public static boolean deleteRecipes(java.util.Collection<File> filesToDelete){
		if(filesToDelete==null||filesToDelete.isEmpty()) return false;
		boolean anyDeleted=false;
		HashSet<Path> parentDirsToCheck=new HashSet<>();
		for(File f: new ArrayList<>(filesToDelete)){
			if(f.exists()){
				try{
					Path filePath=f.toPath();
					Path parentDir=filePath.getParent();
					Files.delete(filePath);
					anyDeleted=true;
					if(parentDir!=null) parentDirsToCheck.add(parentDir);
				}catch(Exception ignored){
				}
			}
		}
		try{
			Path baseDir=getRecipeDir();
			for(Path p: parentDirsToCheck){
				cleanEmptyParents(p,baseDir);
			}
		}catch(Exception ignored){
		}
		return anyDeleted;
	}
	private static void cleanEmptyParents(Path p,Path baseDir){
		try{
			if(p==null||!p.startsWith(baseDir)||p.equals(baseDir)) return;
			try(var s=Files.list(p)){
				if(s.findAny().isEmpty()){
					Files.delete(p);
					cleanEmptyParents(p.getParent(),baseDir);
				}
			}
		}catch(Exception ignored){
		}
	}
	/** Řadí soubory: složky před soubory, pak přirozeně podle jména. */
	public static int compareFiles(File f1,File f2){
		try{
			Path base=getRecipeDir();
			return comparePaths(base,f1.toPath(),f2.toPath());
		}catch(Exception e){
			return f1.getAbsolutePath().compareTo(f2.getAbsolutePath());
		}
	}
	private static int comparePaths(Path base,Path p1,Path p2){
		Path r1=base.relativize(p1), r2=base.relativize(p2);
		String s1=r1.toString().replace('\\','/');
		String s2=r2.toString().replace('\\','/');
		String[] a=s1.split("/"), b=s2.split("/");
		int len=Math.min(a.length,b.length);
		for(int i=0;i<len;i++){
			boolean f1IsFolder=i<a.length-1, f2IsFolder=i<b.length-1;
			if(f1IsFolder&&!f2IsFolder) return -1;
			if(!f1IsFolder&&f2IsFolder) return 1;
			int cmp=compareNatural(a[i],b[i]);
			if(cmp!=0) return cmp;
		}
		return Integer.compare(a.length,b.length);
	}
	private static int compareNatural(String s1,String s2){
		int i1=0, i2=0;
		while(i1<s1.length()&&i2<s2.length()){
			char c1=s1.charAt(i1), c2=s2.charAt(i2);
			if(Character.isDigit(c1)&&Character.isDigit(c2)){
				int st1=i1, st2=i2;
				while(i1<s1.length()&&Character.isDigit(s1.charAt(i1))) i1++;
				while(i2<s2.length()&&Character.isDigit(s2.charAt(i2))) i2++;
				int cmp=new java.math.BigInteger(s1.substring(st1,i1)).compareTo(new java.math.BigInteger(s2.substring(st2,i2)));
				if(cmp!=0) return cmp;
			}else{
				int cmp=Character.compare(Character.toLowerCase(c1),Character.toLowerCase(c2));
				if(cmp!=0) return cmp;
				i1++;
				i2++;
			}
		}
		return Integer.compare(s1.length(),s2.length());
	}
}