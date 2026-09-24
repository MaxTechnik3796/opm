package cz.maxtechnik.opm.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
@SuppressWarnings("unused")
public class OpmCodeViewer extends AbstractWidget{
	private static final Pattern NUMBER_PATTERN=Pattern.compile("^-?\\d+(\\.\\d+)?[fFdDlLsSbB]?$");
	public static final int COLOR_ARRAY=0xFFDA70D6;    // [ ]
	public static final int COLOR_KEY=0xFF9CDCFE;        // minecraft:item, levels
	public static final int COLOR_EQUALS=0xFF808080;     // = : ,
	public static final int COLOR_OBJECT=0xFFFFD700;      // { }
	public static final int COLOR_STRING=0xFFCE9178;     // "common"
	public static final int COLOR_COMPONENT=0xFF4EC9B0;// '{"text":"..."}'
	public static final int COLOR_NUMBER=0xFFB5CEA8;     // 64, 5.0
	public static final int COLOR_BOOLEAN=0xFF569CD6;    // 0b, 1b, true, false
	public record CodeLine(Component display,String toCopy){
	}
	@FunctionalInterface
	public interface OnCopyListener{
		void onCopy(double x,double y,String text);
	}
	private final Font font;
	private final OnCopyListener copyListener;
	private final List<CodeLine> lines=new ArrayList<>();
	private int scroll=0;
	private final int lineHeight=10;
	private int gutterWidth=18; // Šířka prostoru pro čísla řádků
	private int dividerColor=OpmColors.MEDIUM_GRAY;
	private int lineNumberColor=OpmColors.LIGHT_GRAY;
	public OpmCodeViewer(Font font,int x,int y,int width,int height,OnCopyListener copyListener){
		super(x,y,width,height,Component.empty());
		this.font=font;
		this.copyListener=copyListener;
	}
	public void loadFromItemStack(ItemStack itemStack,boolean onlyChanges){
		this.lines.clear();
		if(itemStack==null||itemStack.isEmpty()) return;
		List<OpmItemUtil.ComponentEntry> code=OpmItemUtil.extractComponentsToList(itemStack,onlyChanges);
		this.lines.add(new CodeLine(
				Component.literal("[").withStyle(s->s.withColor(COLOR_ARRAY)),
				"["
		));
		for(OpmItemUtil.ComponentEntry entry: code){
			List<String> formatted=formatComponentLines(entry.id().toString(),entry.valueString());
			for(String fLine: formatted) this.lines.add(new CodeLine(highlightLine(fLine),fLine.trim()));
		}
		this.lines.add(new CodeLine(
				Component.literal("]").withStyle(s->s.withColor(COLOR_ARRAY)),
				"]"
		));
		this.scroll=0;
	}
	public void setLines(List<CodeLine> customLines){
		this.lines.clear();
		this.lines.addAll(customLines);
		this.scroll=0;
	}
	public int getMaxScroll(){
		int totalHeight=this.lines.size()*this.lineHeight;
		return Math.max(0,totalHeight-this.height);
	}
	@Override
	public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY){
		if(!this.visible||!this.isMouseOver(mouseX,mouseY)) return false;
		int scrollSpeed=16;
		this.scroll=Math.clamp(this.scroll-(int)(scrollY*scrollSpeed),0,getMaxScroll());
		return true;
	}
	@Override
	public boolean mouseClicked(double mouseX,double mouseY,int button){
		if(!this.active||!this.visible) return false;
		if(button==0&&this.isMouseOver(mouseX,mouseY)){
			int dividerX=this.getX()+this.gutterWidth;
			int codeStartX=dividerX+4;
			if(mouseX>=codeStartX){
				int clickedIndex=(int)((mouseY-this.getY()+this.scroll)/this.lineHeight);
				if(clickedIndex>=0&&clickedIndex<this.lines.size()){
					CodeLine line=this.lines.get(clickedIndex);
					int textWidth=this.font.width(line.display());
					if(mouseX<=codeStartX+textWidth){
						if(this.copyListener!=null) this.copyListener.onCopy(mouseX,mouseY,line.toCopy());
						Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,1F));
						return true;
					}
				}
			}
			return false;
		}
		return false;
	}
	@Override
	protected void renderWidget(@NotNull GuiGraphics gui,int mouseX,int mouseY,float partialTick){
		if(!this.visible) return;
		int dividerX=this.getX()+this.gutterWidth;
		int gutterRightX=dividerX-3;
		int codeStartX=dividerX+4;
		gui.fill(dividerX,this.getY(),dividerX+1,this.getY()+this.height,this.dividerColor);
		gui.enableScissor(this.getX(),this.getY(),this.getX()+this.width,this.getY()+this.height);
		this.scroll=Math.clamp(this.scroll,0,getMaxScroll());
		for(int i=0;i<this.lines.size();i++){
			int lineY=this.getY()+(i*this.lineHeight)-this.scroll;
			if(lineY+this.lineHeight>=this.getY()&&lineY<=this.getY()+this.height){
				String numStr=String.valueOf(i+1);
				gui.drawString(this.font,numStr,gutterRightX-this.font.width(numStr),lineY,this.lineNumberColor,false);
				CodeLine line=this.lines.get(i);
				gui.drawString(this.font,line.display(),codeStartX,lineY,0xFFFFFF,false);
			}
		}
		gui.disableScissor();
	}
	@Override
	protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput){
	}
	@Override
	public void playDownSound(@NotNull SoundManager soundManager){
	}
	public void setGutterWidth(int gutterWidth){
		this.gutterWidth=gutterWidth;
	}
	public void setDividerColor(int dividerColor){
		this.dividerColor=dividerColor;
	}
	public void setLineNumberColor(int lineNumberColor){
		this.lineNumberColor=lineNumberColor;
	}
	public static MutableComponent highlightLine(String val){
		MutableComponent comp=Component.empty();
		int len=val.length();
		int i=0;
		while(i<len){
			char chart=val.charAt(i);
			if(Character.isWhitespace(chart)){
				comp.append(Component.literal(String.valueOf(chart)));
				i++;
				continue;
			}
			if(chart=='{'||chart=='}'){
				comp.append(Component.literal(String.valueOf(chart)).withStyle(s->s.withColor(COLOR_OBJECT)));
				i++;
			}else if(chart=='['||chart==']'){
				comp.append(Component.literal(String.valueOf(chart)).withStyle(s->s.withColor(COLOR_ARRAY)));
				i++;
			}else if(chart==':'||chart==','||chart=='='){
				comp.append(Component.literal(String.valueOf(chart)).withStyle(s->s.withColor(COLOR_EQUALS)));
				i++;
			}else if(chart=='\''){
				int start=i++;
				while(i<len&&val.charAt(i)!='\''){
					if(val.charAt(i)=='\\'&&i+1<len) i++;
					i++;
				}
				if(i<len) i++;
				comp.append(Component.literal(val.substring(start,i)).withStyle(s->s.withColor(COLOR_COMPONENT)));
			}else if(chart=='"'){
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
				comp.append(Component.literal(str).withStyle(s->s.withColor(isKey?COLOR_KEY:COLOR_STRING)));
			}else{
				int start=i;
				while(i<len&&!Character.isWhitespace(val.charAt(i))&&"{}[],:='\"".indexOf(val.charAt(i))==-1) i++;
				String token=val.substring(start,i);
				int peek=i;
				while(peek<len&&Character.isWhitespace(val.charAt(peek))) peek++;
				boolean isKey=peek<len&&(val.charAt(peek)==':'||val.charAt(peek)=='=');
				if(isKey) comp.append(Component.literal(token).withStyle(s->s.withColor(COLOR_KEY)));
				else if(token.equalsIgnoreCase("true")||token.equalsIgnoreCase("false")||token.equalsIgnoreCase("1b")||token.equalsIgnoreCase("0b")) comp.append(Component.literal(token).withStyle(s->s.withColor(COLOR_BOOLEAN)));
				else if(NUMBER_PATTERN.matcher(token).matches()) comp.append(Component.literal(token).withStyle(s->s.withColor(COLOR_NUMBER)));
				else comp.append(Component.literal(token).withStyle(s->s.withColor(COLOR_STRING)));
			}
		}
		return comp;
	}
	public static List<String> formatComponentLines(String key,String value){
		List<String> lines=new ArrayList<>();
		StringBuilder current=new StringBuilder();
		current.append("  ").append(key).append(" = ");
		int indent=1;
		int len=value.length();
		int i=0;
		while(i<len){
			char quote=value.charAt(i);
			if(quote=='"'||quote=='\''){
				current.append(quote);
				i++;
				while(i<len){
					char chart=value.charAt(i);
					current.append(chart);
					if(chart=='\\'&&i+1<len){
						i++;
						current.append(value.charAt(i));
					}else if(chart==quote){
						i++;
						break;
					}
					i++;
				}
				continue;
			}
			if(quote=='{'||quote=='['){
				char close=(quote=='{')?'}':']';
				int peek=i+1;
				while(peek<len&&Character.isWhitespace(value.charAt(peek))) peek++;
				if(peek<len&&value.charAt(peek)==close){
					current.append(quote).append(close);
					i=peek+1;
				}else{
					current.append(quote);
					lines.add(current.toString());
					indent++;
					current=new StringBuilder();
					current.repeat("  ",indent);
					do i++;
					while(i<len&&Character.isWhitespace(value.charAt(i)));
				}
				continue;
			}
			if(quote=='}'||quote==']'){
				if(!current.toString().trim().isEmpty()) lines.add(current.toString());
				indent=Math.max(1,indent-1);
				current=new StringBuilder();
				current.repeat("  ",indent);
				current.append(quote);
				i++;
				int peek=i;
				while(peek<len&&Character.isWhitespace(value.charAt(peek))) peek++;
				if(peek<len&&value.charAt(peek)==','){
					current.append(',');
					i=peek+1;
				}
				lines.add(current.toString());
				current=new StringBuilder();
				current.repeat("  ",indent);
				while(i<len&&Character.isWhitespace(value.charAt(i))) i++;
				continue;
			}
			if(quote==','){
				current.append(quote);
				lines.add(current.toString());
				current=new StringBuilder();
				current.repeat("  ",indent);
				do i++;
				while(i<len&&Character.isWhitespace(value.charAt(i)));
				continue;
			}
			if(quote==':'){
				current.append(": ");
				do i++;
				while(i<len&&Character.isWhitespace(value.charAt(i)));
				continue;
			}
			current.append(quote);
			i++;
		}
		if(!current.toString().trim().isEmpty()) lines.add(current.toString());
		return lines;
	}
}