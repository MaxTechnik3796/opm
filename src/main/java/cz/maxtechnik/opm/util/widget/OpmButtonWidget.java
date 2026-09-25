package cz.maxtechnik.opm.util.widget;

import cz.maxtechnik.opm.util.OpmColors;
import cz.maxtechnik.opm.util.OpmUtil;
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
public class OpmButtonWidget extends Button{
	private final Font font;
	private Component text, hoverText;
	private boolean underline=false, backGround=true;
	private int textColor=OpmColors.LIGHT_GRAY, hoverTextColor=OpmColors.WHITE2, backGroundColor=OpmColors.GRAY, backGroundOutlineColor=OpmColors.BLACK, backgroundHoverColor=OpmColors.GRAY;
	public OpmButtonWidget(Font font,Component text,int x,int y,int width,int height,OnPress onPress){
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
	public void setBackGround(boolean backGround){
		this.backGround=backGround;
	}
	public void setBackGroundColor(int backGroundColor){
		this.backGroundColor=backGroundColor;
	}
	public void setBackGroundOutlineColor(int backGroundOutlineColor){
		this.backGroundOutlineColor=backGroundOutlineColor;
	}
	public void setBackGroundColors(int backGroudColor,int backGroudOutlineColor){
		this.backGroundColor=backGroudColor;
		this.backGroundOutlineColor=backGroudOutlineColor;
		this.backgroundHoverColor=backGroudColor;
	}
	public void setBackGroundColors(int backGroudColor,int backGroudOutlineColor,int backgroundHoverColor){
		this.backGroundColor=backGroudColor;
		this.backGroundOutlineColor=backGroudOutlineColor;
		this.backgroundHoverColor=backgroundHoverColor;
	}
	public void setBackGroundColors(int backGroudColor){
		this.backGroundColor=backGroudColor;
		this.backGroundOutlineColor=backGroudColor;
		this.backgroundHoverColor=backGroudColor;
	}
	@Override
	public void renderWidget(@NotNull GuiGraphics gui,int mouseX,int mouseY,float partialTicks){
		int x=getX(), y=getY();
		if(backGround){
			gui.renderOutline(x,y,getWidth(),getHeight(),backGroundOutlineColor);
			OpmUtil.drawBoxWithSize(gui,x+1,y+1,getWidth()-2,getHeight()-2,isHovered?backgroundHoverColor:backGroundColor);
			x=x+width/2-font.width(isHovered()?hoverText:text)/2;
			y=y+height/2-4;
		}
		if(underline) hoverText=ComponentUtils.mergeStyles(hoverText.copy(),Style.EMPTY.withUnderlined(true));
		gui.drawString(font,isHovered()?hoverText:text,x,y,isHovered()?hoverTextColor:textColor);
	}
}
