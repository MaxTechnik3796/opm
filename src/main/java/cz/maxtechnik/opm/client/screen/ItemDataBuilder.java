package cz.maxtechnik.opm.client.screen;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@SuppressWarnings("unchecked")
public class ItemDataBuilder{
	private final ItemStack stack;
	public ItemDataBuilder(ItemStack stack){
		this.stack=stack;
	}

	//PUBLIC API ─────────────────────────────────────────────────────────────

	//Full mód – všechny komponenty, správné SNBT.
	public String buildFullText(){
		DataComponentMap comps=stack.getComponents();
		if(comps.isEmpty()) return "[]";
		StringBuilder sb=new StringBuilder("[\n");
		comps.forEach(c->sb.append("  ")
				.append(registryName(c.type(),false))
				.append(" = ")
				.append(formatSnbt(encodeComponent(c),1))
				.append(",\n"));
		if(sb.length()>2){
			sb.setLength(sb.length()-2);
			sb.append("\n");
		}
		return sb.append("]").toString();
	}

	//Simple mód – pouze diff oproti výchozímu stacku.
	public String buildSimpleText(){
		List<String> parts=buildDiffParts(stack,true);
		if(parts.isEmpty()) return "[]";
		return formatSnbt("["+String.join(",",parts)+"]");
	}

	public String buildGiveCommand(String playerName,boolean simpleMode){
		ResourceLocation loc=BuiltInRegistries.ITEM.getKey(stack.getItem());
		StringBuilder sb=new StringBuilder("/give ").append(playerName).append(" ").append(loc);
		List<String> parts=simpleMode?buildDiffParts(stack,false):buildAllParts(stack);
		if(!parts.isEmpty()) sb.append("[").append(String.join(",",parts)).append("]");
		int count=stack.getCount();
		if(count>1) sb.append(" ").append(count);
		return sb.toString();
	}
	//PART BUILDERS ──────────────────────────────────────────────────────────

	private List<String> buildAllParts(ItemStack item){
		List<String> parts=new ArrayList<>();
		item.getComponents().forEach(c->parts.add(componentEntry(c,false)));
		return parts;
	}

	private List<String> buildDiffParts(ItemStack item,boolean prettyTypes){
		ItemStack def=new ItemStack(item.getItem());
		DataComponentMap defComps=def.getComponents();
		List<String> parts=new ArrayList<>();
		item.getComponents().forEach(c->{
			@SuppressWarnings("unchecked")
			DataComponentType<Object> type=(DataComponentType<Object>)c.type();
			Object defaultVal=defComps.get(type);
			if(defaultVal!=null&&defaultVal.equals(c.value())) return;
			parts.add(componentEntry(c,prettyTypes));
		});
		return parts;
	}

	private String componentEntry(TypedDataComponent<?> c,boolean pretty){
		String name=registryName(c.type(),pretty);
		String snbt=encodeComponent(c);
		if(pretty) snbt=cleanSimpleSnbt(snbt);
		return name+"="+snbt;
	}
	private String cleanSimpleSnbt(String snbt){
		String result=snbt.replaceAll("\"minecraft:([a-z0-9_]+)\"","$1");
		result=result.replaceAll("minecraft:([a-z0-9_]+)","$1");
		result=result.replaceAll("\\b(\\d+)[bBsSlL]\\b","$1");
		result=result.replaceAll("\\b(\\d+\\.\\d+)[fFdD]\\b","$1");
		return result;
	}

	//COMPONENT ENCODING ─────────────────────────────────────────────────────

	public String encodeComponent(TypedDataComponent<?> c){
		if(c.type()==DataComponents.CONTAINER) return encodeContainer((ItemContainerContents)c.value(),c);
		Optional<Tag> tag=encodeWithCodec(c.type(),c.value());
		return tag.map(Tag::toString).orElseGet(()->"\""+c.value().toString().replace("\"","\\\"")+"\"");
	}

	private String encodeContainer(ItemContainerContents contents,TypedDataComponent<?> c){
		try{
			NonNullList<ItemStack> items=NonNullList.withSize(contents.getSlots(),ItemStack.EMPTY);
			contents.copyInto(items);
			StringBuilder sb=new StringBuilder("[");
			boolean first=true;
			for(int slot=0;slot<items.size();slot++){
				ItemStack item=items.get(slot);
				if(item.isEmpty()) continue;
				if(!first) sb.append(",");
				first=false;
				var itemCodec=ItemStack.CODEC;
				Optional<Tag> tag=itemCodec.encodeStart(getOps(),item).result();
				if(tag.isPresent()) sb.append("{slot:").append(slot).append(",item:").append(tag.get()).append("}");
			}
			sb.append("]");
			return sb.toString();
		}catch(Exception e){
			return encodeWithCodec(c.type(),c.value()).map(Tag::toString).orElse("[]");
		}
	}

	//HELPERS ────────────────────────────────────────────────────────────────

	private String registryName(DataComponentType<?> type,boolean pretty){
		ResourceLocation key=BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
		if(key==null) return type.toString();
		if(pretty&&key.getNamespace().equals("minecraft")) return key.getPath();
		return key.toString();
	}

	//SNBT FORMATTER ─────────────────────────────────────────────────────────

	public String formatSnbt(String raw){
		return formatSnbt(raw,0);
	}
	public String formatSnbt(String raw,int initialIndent){
		StringBuilder sb=new StringBuilder();
		int indent=initialIndent;
		boolean inStr=false;
		char strCh=0;
		char[] scope=new char[256];
		int depth=0;
		for(int i=0;i<raw.length();i++){
			char c=raw.charAt(i);
			if(inStr){
				sb.append(c);
				if(c==strCh&&(i==0||raw.charAt(i-1)!='\\')) inStr=false;
				else if(c==','&&i+1<raw.length()&&raw.charAt(i+1)!=' '){
					if(i>0&&raw.charAt(i-1)=='}'){
						sb.append("\n").repeat("  ",indent+1);
					}
				}
			}else if(c=='"'||c=='\''){
				inStr=true;
				strCh=c;
				sb.append(c);
			}else if(c=='{'||c=='['){
				if(depth<255) scope[depth]=c;
				depth++;
				char close=c=='{'?'}':']';
				if(i+1<raw.length()&&raw.charAt(i+1)==close) sb.append(c);
				else{
					indent++;
					sb.append(c).append("\n").repeat("  ",indent);
				}
			}else if(c=='}'||c==']'){
				depth=Math.max(0,depth-1);
				char open=c=='}'?'{':'[';
				if(i>0&&raw.charAt(i-1)==open) sb.append(c);
				else{
					indent=Math.max(initialIndent,indent-1);
					sb.append("\n").repeat("  ",indent).append(c);
				}
			}else if(c==';'){
				sb.append(c).append(" ");
				while(i+1<raw.length()&&raw.charAt(i+1)==' ') i++;
			}else if(c==','){
				sb.append(c);
				sb.append("\n").repeat("  ",indent);
				while(i+1<raw.length()&&raw.charAt(i+1)==' ') i++;
			}else if(c==':'){
				sb.append(c);
				if(i+1<raw.length()&&raw.charAt(i+1)!=' ') sb.append(" ");
			}else if(c=='\n'){
				sb.append('\n');
				while(i+1<raw.length()&&(raw.charAt(i+1)==' '||raw.charAt(i+1)=='\t')) i++;
				sb.repeat("  ",indent);
			}else sb.append(c);
		}
		return sb.toString();
	}
	private <T> Optional<Tag> encodeWithCodec(DataComponentType<T> type,Object value){
		var codec=type.codec();
		if(codec==null) return Optional.empty();
		return codec.encodeStart(getOps(),(T)value).result();
	}
	private com.mojang.serialization.DynamicOps<Tag> getOps(){
		var mc=net.minecraft.client.Minecraft.getInstance();
		if(mc.getConnection()!=null)
			return net.minecraft.resources.RegistryOps.create(NbtOps.INSTANCE,mc.getConnection().registryAccess());
		if(mc.level!=null) return net.minecraft.resources.RegistryOps.create(NbtOps.INSTANCE,mc.level.registryAccess());
		return NbtOps.INSTANCE;
	}
}