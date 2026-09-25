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
import java.util.Map;
import java.util.Optional;
public class OpmComponentHandler{
	public record ComponentEntry(ResourceLocation id,String valueString){
	}
	public static String extractComponentsToString(ItemStack itemStack,boolean onlyChanges){
		if(itemStack.isEmpty()||Minecraft.getInstance().level==null) return "";
		HolderLookup.Provider registries=Minecraft.getInstance().level.registryAccess();
		DataComponentPatch patch=onlyChanges?itemStack.getComponentsPatch():createFullPatch(itemStack);
		ItemInput itemInput=new ItemInput(itemStack.getItemHolder(),patch);
		String serialized=itemInput.serialize(registries);
		int bracketStart=serialized.indexOf('[');
		if(bracketStart!=-1) return serialized.substring(bracketStart);
		return "";
	}
	@SuppressWarnings("unchecked")
	public static List<ComponentEntry> extractComponentsToList(ItemStack itemStack,boolean onlyChanges){
		List<ComponentEntry> list=new ArrayList<>();
		if(itemStack.isEmpty()||Minecraft.getInstance().level==null) return list;
		HolderLookup.Provider registries=Minecraft.getInstance().level.registryAccess();
		DynamicOps<Tag> ops=registries.createSerializationContext(NbtOps.INSTANCE);
		if(onlyChanges){
			DataComponentPatch patch=itemStack.getComponentsPatch();
			for(Map.Entry<DataComponentType<?>,Optional<?>> entry: patch.entrySet()){
				DataComponentType<?> type=entry.getKey();
				ResourceLocation id=BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
				if(id==null) continue;
				if(entry.getValue().isPresent()){
					String valueText=serializeValue((DataComponentType<Object>)type,entry.getValue().get(),ops);
					list.add(new ComponentEntry(id,valueText));
				}else list.add(new ComponentEntry(id,"!removed"));
			}
		}else{
			for(TypedDataComponent<?> typed: itemStack.getComponents()){
				DataComponentType<?> type=typed.type();
				ResourceLocation id=BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
				if(id==null) continue;
				String valueText=serializeValue(type,typed.value(),ops);
				list.add(new ComponentEntry(id,valueText));
			}
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
}