package cz.maxtechnik.opm.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
public class OpmItemUtil{
	public record ComponentEntry(ResourceLocation id,String valueString){
	}
	/**
	 * Vrátí pouze část v hranatých závorkách: např. "[minecraft:max_stack_size=64,...]"
	 */
	public static String getComponentsString(ItemStack itemStack,boolean fullMode){
		if(itemStack.isEmpty()||Minecraft.getInstance().level==null) return "";
		HolderLookup.Provider registries=Minecraft.getInstance().level.registryAccess();
		DataComponentPatch patch=fullMode?createFullPatch(itemStack):itemStack.getComponentsPatch();
		ItemInput itemInput=new ItemInput(itemStack.getItemHolder(),patch);
		String serialized=itemInput.serialize(registries);
		int bracketStart=serialized.indexOf('[');
		if(bracketStart!=-1){
			return serialized.substring(bracketStart);
		}
		return "";
	}
	/**
	 * Vytáhne všechny komponenty itemu jako seznam záznamů k zobrazení v UI.
	 */
	public static List<ComponentEntry> extractComponents(ItemStack itemStack){
		List<ComponentEntry> list=new ArrayList<>();
		if(itemStack.isEmpty()||Minecraft.getInstance().level==null) return list;
		HolderLookup.Provider registries=Minecraft.getInstance().level.registryAccess();
		DynamicOps<Tag> ops=registries.createSerializationContext(NbtOps.INSTANCE);
		for(TypedDataComponent<?> typed: itemStack.getComponents()){
			DataComponentType<?> type=typed.type();
			ResourceLocation id=BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
			String valueText=serializeValue(type,typed.value(),ops);
			list.add(new ComponentEntry(id,valueText));
		}
		return list;
	}
	@SuppressWarnings("unchecked")
	private static <T> String serializeValue(DataComponentType<T> type,Object value,DynamicOps<Tag> ops){
		Codec<T> codec=type.codec();
		if(codec!=null){
			DataResult<Tag> result=codec.encodeStart(ops,(T)value);
			if(result.isSuccess()) return result.getOrThrow().toString();
		}
		return String.valueOf(value);
	}
	private static DataComponentPatch createFullPatch(ItemStack itemStack){
		DataComponentPatch.Builder builder=DataComponentPatch.builder();
		for(TypedDataComponent<?> typed: itemStack.getComponents()) if(typed.type().codec()!=null) addToPatch(builder,typed);
		return builder.build();
	}
	private static <T> void addToPatch(DataComponentPatch.Builder builder,TypedDataComponent<T> typed){
		builder.set(typed.type(),typed.value());
	}
	// Barevná paleta pro syntax highlighting
	public static final int COLOR_BRACKET=0xFFE5C07B;    // [ ]
	public static final int COLOR_KEY=0xFF61AFEF;        // minecraft:item, levels
	public static final int COLOR_EQUALS=0xFFABB2BF;     // =
	public static final int COLOR_BRACE=0xFFD19A66;      // { }
	public static final int COLOR_STRING=0xFF98C379;     // "common"
	public static final int COLOR_COMP_STRING=0xFFE06C75;// '{"text":"..."}'
	public static final int COLOR_NUMBER=0xFFE5C07B;     // 64, 5.0
	public static final int COLOR_BOOLEAN=0xFFC678DD;    // 0b, 1b, true, false
	public static final int COLOR_DELIMITER=0xFF7F848E;  // : a ,
	/**
	 * Zformátuje SNBT hodnotu na obarvený Component.
	 */
	public static net.minecraft.network.chat.MutableComponent highlightValue(String val){
		net.minecraft.network.chat.MutableComponent comp=net.minecraft.network.chat.Component.empty();
		int len=val.length();
		int i=0;
		while(i<len){
			char c=val.charAt(i);
			// Mezery
			if(Character.isWhitespace(c)){
				comp.append(net.minecraft.network.chat.Component.literal(String.valueOf(c)));
				i++;
				continue;
			}
			// Závorky a oddělovače
			if(c=='{'||c=='}'){
				comp.append(net.minecraft.network.chat.Component.literal(String.valueOf(c)).withStyle(s->s.withColor(COLOR_BRACE)));
				i++;
			}else if(c=='['||c==']'){
				comp.append(net.minecraft.network.chat.Component.literal(String.valueOf(c)).withStyle(s->s.withColor(COLOR_BRACKET)));
				i++;
			}else if(c==':'||c==','){
				comp.append(net.minecraft.network.chat.Component.literal(String.valueOf(c)).withStyle(s->s.withColor(COLOR_DELIMITER)));
				i++;
			}
			// Componentové texty v jednoduchých uvozovkách: '{"text":"..."}'
			else if(c=='\''){
				int start=i++;
				while(i<len&&val.charAt(i)!='\''){
					if(val.charAt(i)=='\\'&&i+1<len) i++;
					i++;
				}
				if(i<len) i++;
				comp.append(net.minecraft.network.chat.Component.literal(val.substring(start,i)).withStyle(s->s.withColor(COLOR_COMP_STRING)));
			}
			// Dvojité uvozovky: "..." (buď String, nebo klíč)
			else if(c=='"'){
				int start=i++;
				while(i<len&&val.charAt(i)!='"'){
					if(val.charAt(i)=='\\'&&i+1<len) i++;
					i++;
				}
				if(i<len) i++;
				String str=val.substring(start,i);
				// Kontrola, zda za uvozovkami následuje dvojtečka (pak jde o klíč)
				int peek=i;
				while(peek<len&&Character.isWhitespace(val.charAt(peek))) peek++;
				boolean isKey=peek<len&&val.charAt(peek)==':';
				comp.append(net.minecraft.network.chat.Component.literal(str).withStyle(s->s.withColor(isKey?COLOR_KEY:COLOR_STRING)));
			}
			// Slova, čísla, booleany, unquoted identifikátory
			else{
				int start=i;
				while(i<len&&!Character.isWhitespace(val.charAt(i))&&"{}[],:'\"".indexOf(val.charAt(i))==-1){
					i++;
				}
				String token=val.substring(start,i);
				int peek=i;
				while(peek<len&&Character.isWhitespace(val.charAt(peek))) peek++;
				boolean isKey=peek<len&&val.charAt(peek)==':';
				if(isKey){
					comp.append(net.minecraft.network.chat.Component.literal(token).withStyle(s->s.withColor(COLOR_KEY)));
				}else if(token.equalsIgnoreCase("true")||token.equalsIgnoreCase("false")||token.equalsIgnoreCase("1b")||token.equalsIgnoreCase("0b")){
					comp.append(net.minecraft.network.chat.Component.literal(token).withStyle(s->s.withColor(COLOR_BOOLEAN)));
				}else if(token.matches("^-?\\d+(\\.\\d+)?[fFdDlLsSbB]?$")){
					comp.append(net.minecraft.network.chat.Component.literal(token).withStyle(s->s.withColor(COLOR_NUMBER)));
				}else{
					comp.append(net.minecraft.network.chat.Component.literal(token).withStyle(s->s.withColor(COLOR_STRING)));
				}
			}
		}
		return comp;
	}
	// ====================================================================
	// PRETTY PRINT / JSON-LIKE INDENT FORMÁTOVAČ PRO SNBT
	// ====================================================================
	public static List<String> formatComponentLines(String key,String value){
		List<String> lines=new ArrayList<>();
		StringBuilder current=new StringBuilder();
		current.append("  ").append(key).append(" = ");
		int indent=1; // 1 úroveň = 2 mezery
		int len=value.length();
		int i=0;
		while(i<len){
			char c=value.charAt(i);
			// 1. Ochrana stringů: v uvozovkách závorky ani čárky neformátujeme
			if(c=='"'||c=='\''){
				char quote=c;
				current.append(c);
				i++;
				while(i<len){
					char ch=value.charAt(i);
					current.append(ch);
					if(ch=='\\'&&i+1<len){
						i++;
						current.append(value.charAt(i));
					}else if(ch==quote){
						i++;
						break;
					}
					i++;
				}
				continue;
			}
			// 2. Otevírací závorky: detekce prázdných [] a {}
			if(c=='{'||c=='['){
				char close=(c=='{')?'}':']';
				int peek=i+1;
				while(peek<len&&Character.isWhitespace(value.charAt(peek))) peek++;
				if(peek<len&&value.charAt(peek)==close){
					// Prázdné závorky zůstanou na jednom řádku
					current.append(c).append(close);
					i=peek+1;
					continue;
				}else{
					// Neprázdná závorka: zalomit a zvednout indent
					current.append(c);
					lines.add(current.toString());
					indent++;
					current=new StringBuilder();
					current.append("  ".repeat(indent));
					i++;
					while(i<len&&Character.isWhitespace(value.charAt(i))) i++;
					continue;
				}
			}
			// 3. Uzavírací závorky: zmenšit indent a dát na nový řádek
			if(c=='}'||c==']'){
				if(!current.toString().trim().isEmpty()){
					lines.add(current.toString());
				}
				indent=Math.max(1,indent-1);
				current=new StringBuilder();
				current.append("  ".repeat(indent));
				current.append(c);
				i++;
				// Pokud za závorkou hned následuje čárka, přilepíme ji k závorce
				int peek=i;
				while(peek<len&&Character.isWhitespace(value.charAt(peek))) peek++;
				if(peek<len&&value.charAt(peek)==','){
					current.append(',');
					i=peek+1;
				}
				lines.add(current.toString());
				current=new StringBuilder();
				current.append("  ".repeat(indent));
				while(i<len&&Character.isWhitespace(value.charAt(i))) i++;
				continue;
			}
			// 4. Čárka: zalomit a držet aktuální úroveň odsazení
			if(c==','){
				current.append(c);
				lines.add(current.toString());
				current=new StringBuilder();
				current.append("  ".repeat(indent));
				i++;
				while(i<len&&Character.isWhitespace(value.charAt(i))) i++;
				continue;
			}
			// 5. Dvojtečka: přidat mezeru za dvojtečku pro čitelnost
			if(c==':'){
				current.append(": ");
				i++;
				while(i<len&&Character.isWhitespace(value.charAt(i))) i++;
				continue;
			}
			current.append(c);
			i++;
		}
		if(!current.toString().trim().isEmpty()){
			lines.add(current.toString());
		}
		return lines;
	}
	// ====================================================================
	// SYNTAX HIGHLIGHTER PRO JEDNOTLIVÉ ŘÁDKY
	// ====================================================================
	public static net.minecraft.network.chat.MutableComponent highlightLine(String val){
		net.minecraft.network.chat.MutableComponent comp=net.minecraft.network.chat.Component.empty();
		int len=val.length();
		int i=0;
		while(i<len){
			char c=val.charAt(i);
			if(Character.isWhitespace(c)){
				comp.append(net.minecraft.network.chat.Component.literal(String.valueOf(c)));
				i++;
				continue;
			}
			if(c=='{'||c=='}'){
				comp.append(net.minecraft.network.chat.Component.literal(String.valueOf(c)).withStyle(s->s.withColor(COLOR_BRACE)));
				i++;
			}else if(c=='['||c==']'){
				comp.append(net.minecraft.network.chat.Component.literal(String.valueOf(c)).withStyle(s->s.withColor(COLOR_BRACKET)));
				i++;
			}else if(c=='='){
				comp.append(net.minecraft.network.chat.Component.literal("=").withStyle(s->s.withColor(COLOR_EQUALS)));
				i++;
			}else if(c==':'||c==','){
				comp.append(net.minecraft.network.chat.Component.literal(String.valueOf(c)).withStyle(s->s.withColor(COLOR_DELIMITER)));
				i++;
			}else if(c=='\''){
				int start=i++;
				while(i<len&&val.charAt(i)!='\''){
					if(val.charAt(i)=='\\'&&i+1<len) i++;
					i++;
				}
				if(i<len) i++;
				comp.append(net.minecraft.network.chat.Component.literal(val.substring(start,i)).withStyle(s->s.withColor(COLOR_COMP_STRING)));
			}else if(c=='"'){
				int start=i++;
				while(i<len&&val.charAt(i)!='"'){
					if(val.charAt(i)=='\\'&&i+1<len) i++;
					i++;
				}
				if(i<len) i++;
				String str=val.substring(start,i);
				int peek=i;
				while(peek<len&&Character.isWhitespace(val.charAt(peek))) peek++;
				boolean isKey=peek<len&&(val.charAt(peek)==':'||val.charAt(peek)=='=');
				comp.append(net.minecraft.network.chat.Component.literal(str).withStyle(s->s.withColor(isKey?COLOR_KEY:COLOR_STRING)));
			}else{
				int start=i;
				while(i<len&&!Character.isWhitespace(val.charAt(i))&&"{}[],:='\"".indexOf(val.charAt(i))==-1){
					i++;
				}
				String token=val.substring(start,i);
				int peek=i;
				while(peek<len&&Character.isWhitespace(val.charAt(peek))) peek++;
				boolean isKey=peek<len&&(val.charAt(peek)==':'||val.charAt(peek)=='=');
				if(isKey){
					comp.append(net.minecraft.network.chat.Component.literal(token).withStyle(s->s.withColor(COLOR_KEY)));
				}else if(token.equalsIgnoreCase("true")||token.equalsIgnoreCase("false")||token.equalsIgnoreCase("1b")||token.equalsIgnoreCase("0b")){
					comp.append(net.minecraft.network.chat.Component.literal(token).withStyle(s->s.withColor(COLOR_BOOLEAN)));
				}else if(token.matches("^-?\\d+(\\.\\d+)?[fFdDlLsSbB]?$")){
					comp.append(net.minecraft.network.chat.Component.literal(token).withStyle(s->s.withColor(COLOR_NUMBER)));
				}else{
					comp.append(net.minecraft.network.chat.Component.literal(token).withStyle(s->s.withColor(COLOR_STRING)));
				}
			}
		}
		return comp;
	}
}