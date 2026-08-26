package cz.maxtechnik.opm.client.editor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
/**
 * Samostatný SearchEngine modul pro vyhledávání položek, fluidů, tagů a receptů.
 * Podporuje:
 *  - {@code @mod [item]} : vyhledávání podle namespace modu (např. {@code @minecraft iron} nebo {@code @create})
 *  - {@code #tag [item]} : vyhledávání podle registrovaných tagů (např. {@code #logs} nebo {@code #c:ingots})
 *  - {@code text}        : běžné vyhledávání podle názvu, ID předmětu nebo tagu
 *  - {@code |}           : řetězení více dotazů (logické NEBO)
 */
public final class SearchEngine{
	private SearchEngine(){
	}
	/** Zjistí, zda předmět odpovídá zadanému dotazu. */
	public static boolean matches(ItemStack stack,String query){
		if(query==null||query.isBlank()) return true;
		if(stack==null||stack.isEmpty()) return false;
		String q=query.trim().toLowerCase(Locale.ROOT);
		// Podpora | (OR) – rozděl na větve, stačí shoda s jednou
		if(q.contains("|")){
			for(String branch: q.split("\\|")){
				if(!branch.isBlank()&&matchesSingle(stack,branch.trim())) return true;
			}
			return false;
		}
		return matchesSingle(stack,q);
	}
	private static boolean matchesSingle(ItemStack stack,String q){
		if(q.startsWith("@")){
			String body=q.substring(1).trim();
			if(body.isEmpty()) return true;
			String[] parts=body.split("\\s+");
			String modPart=parts[0];
			ResourceLocation loc=BuiltInRegistries.ITEM.getKey(stack.getItem());
			String ns=loc.getNamespace().toLowerCase(Locale.ROOT);
			if(!ns.contains(modPart)) return false;
			// Každé další slovo za modem musí odpovídat názvu/path/id (AND)
			String name=stack.getHoverName().getString().toLowerCase(Locale.ROOT);
			String path=loc.getPath().toLowerCase(Locale.ROOT);
			String id=loc.toString().toLowerCase(Locale.ROOT);
			for(int i=1;i<parts.length;i++){
				if(!parts[i].isEmpty()&&!(name.contains(parts[i])||path.contains(parts[i])||id.contains(parts[i]))){
					return false;
				}
			}
			return true;
		}
		if(q.startsWith("#")){
			String body=q.substring(1).trim();
			if(body.isEmpty()) return true;
			String[] parts=body.split("\\s+");
			String tagPart=parts[0];
			boolean hasTag;
			String hoverName=stack.getHoverName().getString().toLowerCase(Locale.ROOT);
			if(hoverName.contains(tagPart)){
				hasTag=true;
			}else{
				try{
					var holder=stack.getItemHolder();
					hasTag=holder.tags().anyMatch(t->t.location().toString().toLowerCase(Locale.ROOT).contains(tagPart)
							||t.location().getPath().toLowerCase(Locale.ROOT).contains(tagPart));
				}catch(Exception ignored){
					hasTag=false;
				}
			}
			if(!hasTag) return false;
			// Každé další slovo za tagem musí odpovídat názvu/path/id (AND)
			ResourceLocation loc=BuiltInRegistries.ITEM.getKey(stack.getItem());
			String path=loc.getPath().toLowerCase(Locale.ROOT);
			String id=loc.toString().toLowerCase(Locale.ROOT);
			for(int i=1;i<parts.length;i++){
				if(!parts[i].isEmpty()&&!(hoverName.contains(parts[i])||path.contains(parts[i])||id.contains(parts[i]))){
					return false;
				}
			}
			return true;
		}
		// Běžné vyhledávání – všechna slova musí odpovídat (AND)
		String name=stack.getHoverName().getString().toLowerCase(Locale.ROOT);
		ResourceLocation loc=BuiltInRegistries.ITEM.getKey(stack.getItem());
		String id=loc.toString().toLowerCase(Locale.ROOT);
		String path=loc.getPath().toLowerCase(Locale.ROOT);
		for(String word: q.split("\\s+")){
			if(word.isEmpty()) continue;
			boolean hit=name.contains(word)||id.contains(word)||path.contains(word);
			if(!hit){
				try{
					var holder=stack.getItemHolder();
					hit=holder.tags().anyMatch(t->t.location().toString().toLowerCase(Locale.ROOT).contains(word));
				}catch(Exception ignored){
				}
			}
			if(!hit) return false;
		}
		return true;
	}
	/** Zjistí, zda soubor receptu odpovídá vyhledávacímu dotazu. */
	public static boolean matchesFile(File file,Path baseDir,String query){
		if(query==null||query.isBlank()) return true;
		if(file==null||!file.exists()) return false;
		String q=query.trim().toLowerCase(Locale.ROOT);
		try{
			if(baseDir!=null){
				String rel=baseDir.relativize(file.toPath()).toString().replace('\\','/').toLowerCase(Locale.ROOT);
				if(rel.contains(q)) return true;
			}
		}catch(Exception ignored){
		}
		return file.getName().toLowerCase(Locale.ROOT).contains(q);
	}
}