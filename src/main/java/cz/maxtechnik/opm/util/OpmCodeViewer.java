package cz.maxtechnik.opm.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
public class OpmCodeViewer extends AbstractWidget{
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
	public void loadFromItemStack(ItemStack itemStack){
		this.lines.clear();
		if(itemStack==null||itemStack.isEmpty()) return;
		List<OpmItemUtil.ComponentEntry> code=OpmItemUtil.extractComponents(itemStack);
		this.lines.add(new CodeLine(
				Component.literal("[").withStyle(s->s.withColor(COLOR_BRACKET)),
				"["
		));
		for(OpmItemUtil.ComponentEntry entry: code){
			List<String> formatted=formatComponentLines(entry.id().toString(),entry.valueString());
			for(String fLine: formatted){
				this.lines.add(new CodeLine(highlightLine(fLine),fLine.trim()));
			}
		}
		this.lines.add(new CodeLine(
				Component.literal("]").withStyle(s->s.withColor(COLOR_BRACKET)),
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
			// Kliknutí musí být v prostoru kódu (napravo od linky)
			if(mouseX>=codeStartX){
				int clickedIndex=(int)((mouseY-this.getY()+this.scroll)/this.lineHeight);
				if(clickedIndex>=0&&clickedIndex<this.lines.size()){
					CodeLine line=this.lines.get(clickedIndex);
					int textWidth=this.font.width(line.display());
					// Kliknuto přímo na text řádku
					if(mouseX<=codeStartX+textWidth){
						if(this.copyListener!=null){
							this.copyListener.onCopy(mouseX,mouseY,line.toCopy());
						}
						return true;
					}
				}
			}
		}
		return super.mouseClicked(mouseX,mouseY,button);
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
			char c=val.charAt(i);
			if(Character.isWhitespace(c)){
				comp.append(Component.literal(String.valueOf(c)));
				i++;
				continue;
			}
			if(c=='{'||c=='}'){
				comp.append(Component.literal(String.valueOf(c)).withStyle(s->s.withColor(COLOR_BRACE)));
				i++;
			}else if(c=='['||c==']'){
				comp.append(Component.literal(String.valueOf(c)).withStyle(s->s.withColor(COLOR_BRACKET)));
				i++;
			}else if(c=='='){
				comp.append(Component.literal("=").withStyle(s->s.withColor(COLOR_EQUALS)));
				i++;
			}else if(c==':'||c==','){
				comp.append(Component.literal(String.valueOf(c)).withStyle(s->s.withColor(COLOR_DELIMITER)));
				i++;
			}else if(c=='\''){
				int start=i++;
				while(i<len&&val.charAt(i)!='\''){
					if(val.charAt(i)=='\\'&&i+1<len) i++;
					i++;
				}
				if(i<len) i++;
				comp.append(Component.literal(val.substring(start,i)).withStyle(s->s.withColor(COLOR_COMP_STRING)));
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
				comp.append(Component.literal(str).withStyle(s->s.withColor(isKey?COLOR_KEY:COLOR_STRING)));
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
					comp.append(Component.literal(token).withStyle(s->s.withColor(COLOR_KEY)));
				}else if(token.equalsIgnoreCase("true")||token.equalsIgnoreCase("false")||token.equalsIgnoreCase("1b")||token.equalsIgnoreCase("0b")){
					comp.append(Component.literal(token).withStyle(s->s.withColor(COLOR_BOOLEAN)));
				}else if(token.matches("^-?\\d+(\\.\\d+)?[fFdDlLsSbB]?$")){
					comp.append(Component.literal(token).withStyle(s->s.withColor(COLOR_NUMBER)));
				}else{
					comp.append(Component.literal(token).withStyle(s->s.withColor(COLOR_STRING)));
				}
			}
		}
		return comp;
	}
	/**
	 * Zformátuje SNBT hodnotu na obarvený Component.
	 */
	public static MutableComponent highlightValue(String val){
		MutableComponent comp=Component.empty();
		int len=val.length();
		int i=0;
		while(i<len){
			char c=val.charAt(i);
			// Mezery
			if(Character.isWhitespace(c)){
				comp.append(Component.literal(String.valueOf(c)));
				i++;
				continue;
			}
			// Závorky a oddělovače
			if(c=='{'||c=='}'){
				comp.append(Component.literal(String.valueOf(c)).withStyle(s->s.withColor(COLOR_BRACE)));
				i++;
			}else if(c=='['||c==']'){
				comp.append(Component.literal(String.valueOf(c)).withStyle(s->s.withColor(COLOR_BRACKET)));
				i++;
			}else if(c==':'||c==','){
				comp.append(Component.literal(String.valueOf(c)).withStyle(s->s.withColor(COLOR_DELIMITER)));
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
				comp.append(Component.literal(val.substring(start,i)).withStyle(s->s.withColor(COLOR_COMP_STRING)));
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
				comp.append(Component.literal(str).withStyle(s->s.withColor(isKey?COLOR_KEY:COLOR_STRING)));
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
					comp.append(Component.literal(token).withStyle(s->s.withColor(COLOR_KEY)));
				}else if(token.equalsIgnoreCase("true")||token.equalsIgnoreCase("false")||token.equalsIgnoreCase("1b")||token.equalsIgnoreCase("0b")){
					comp.append(Component.literal(token).withStyle(s->s.withColor(COLOR_BOOLEAN)));
				}else if(token.matches("^-?\\d+(\\.\\d+)?[fFdDlLsSbB]?$")){
					comp.append(Component.literal(token).withStyle(s->s.withColor(COLOR_NUMBER)));
				}else{
					comp.append(Component.literal(token).withStyle(s->s.withColor(COLOR_STRING)));
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
}