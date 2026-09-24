package cz.maxtechnik.opm.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("unused")
public class OpmButton extends Button{
	private final Font font;
	private Component text,hoverText;
	private boolean underline=false,backGround=true;
	private int textColor=OpmColors.GRAY3, hoverTextColor=OpmColors.WHITE2, backGroudColor=OpmColors.GRAY, backGroudOutlineColor=OpmColors.BLACK;
	public OpmButton(Font font,Component text,int x,int y,int width,int height,OnPress onPress){
		super(x,y,width,height,text,onPress,DEFAULT_NARRATION);
		this.font=font;
		this.text=text;
		this.hoverText=text;
	}
	public void setTextColor(int textColor){
		this.textColor=textColor;
	}
	private void setHoverTextColor(int hoverTextColor){
		this.hoverTextColor=hoverTextColor;
	}
	public void setTextColors(int textColor){
		this.textColor=textColor;
		this.hoverTextColor=textColor;
	}
	public void setTextColors(int textColor,int hoverTextColor){
		this.textColor=textColor;
		this.hoverTextColor=hoverTextColor;
	}
	public void setUnderline(boolean underline){
		this.underline=underline;
	}
	public void setText(Component text){
		this.text=text;
		this.hoverText=text;
	}
	public void setBackGroud(boolean backGround){
		this.backGround=backGround;
	}
	public void setBackGroudColor(int backGroudColor){
		this.backGroudColor=backGroudColor;
	}
	public void setBackGroudOutlineColor(int backGroudOutlineColor){
		this.backGroudOutlineColor=backGroudOutlineColor;
	}
	public void setBackGroundColors(int backGroudColor,int backGroudOutlineColor){
		this.backGroudColor=backGroudColor;
		this.backGroudOutlineColor=backGroudOutlineColor;
	}
	public void setBackGroundColors(int backGroudColor){
		this.backGroudColor=backGroudColor;
		this.backGroudOutlineColor=backGroudColor;
	}
	@Override
	public void renderWidget(@NotNull GuiGraphics gui,int mouseX,int mouseY,float partialTicks){
		int x=getX(),y=getY();
		if(backGround){
			OpmModUtil.drawBoxWithSize(gui,x,y,getWidth(),getHeight(),backGroudOutlineColor);
			OpmModUtil.drawBoxWithSize(gui,x+1,y+1,getWidth()-2,getHeight()-2,backGroudColor);
			x=x+width/2-font.width(isHovered()?hoverText:text)/2;
			y=y+height/2-4;
		}
		if(underline) hoverText=ComponentUtils.mergeStyles(hoverText.copy(),Style.EMPTY.withUnderlined(true));
		gui.drawString(font,isHovered()?hoverText:text,x,y,isHovered()?hoverTextColor:textColor);
	}
}
