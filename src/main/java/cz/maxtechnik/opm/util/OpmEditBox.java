package cz.maxtechnik.opm.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
@SuppressWarnings("unused")
public class OpmEditBox extends EditBox{
	private int backgroundColor=OpmColors.GRAY;
	private int borderColor=OpmColors.BLACK;
	private int focusedBorderColor=OpmColors.BLACK;
	private boolean drawBackground=true;
	private boolean drawBorder=true;
	private int paddingX=4;
	public OpmEditBox(Font font,int x,int y,int width,int height,Component text){
		super(font,x,y,width,height,text);
		super.setBordered(false);
	}
	public void setBackgroundColor(int backgroundColor){
		this.backgroundColor=backgroundColor;
		this.drawBackground=true;
	}
	public void setBorderColor(int borderColor){
		this.borderColor=borderColor;
		this.focusedBorderColor=borderColor;
		this.drawBorder=true;
	}
	public void setBorderColor(int borderColor,int focusedBorderColor){
		this.borderColor=borderColor;
		this.focusedBorderColor=focusedBorderColor;
		this.drawBorder=true;
	}
	public void setBackground(boolean background){
		this.drawBackground=background;
	}
	public void setBorder(boolean border){
		this.drawBorder=border;
	}
	public void setPaddingX(int paddingX){
		this.paddingX=paddingX;
	}
	@Override
	public int getInnerWidth(){
		return Math.max(1,this.width-this.paddingX*2);
	}
	private int getPaddingY(){
		return Math.max(0,(this.height-9)/2);
	}
	@Override
	public void renderWidget(@NotNull GuiGraphics gui,int mouseX,int mouseY,float partialTick){
		if(!this.isVisible()) return;
		if(this.drawBackground) gui.fill(this.getX(),this.getY(),this.getX()+this.width,this.getY()+this.height,this.backgroundColor);
		if(this.drawBorder) gui.renderOutline(this.getX(),this.getY(),this.width,this.height,this.isFocused()?this.focusedBorderColor:this.borderColor);
		gui.pose().pushPose();
		gui.pose().translate(this.paddingX,getPaddingY(),0);
		super.renderWidget(gui,mouseX,mouseY,partialTick);
		gui.pose().popPose();
	}
	@Override
	public boolean mouseClicked(double mouseX,double mouseY,int button){
		if(!this.active||!this.visible) return false;
		if(this.isMouseOver(mouseX,mouseY)){
			double clampedX=Math.clamp(this.getX()+this.width-1,this.getX(),mouseX-this.paddingX);
			return super.mouseClicked(clampedX,mouseY,button);
		}
		return super.mouseClicked(mouseX,mouseY,button);
	}
	@Override
	public boolean mouseDragged(double mouseX,double mouseY,int button,double dragX,double dragY){
		if(!this.active||!this.visible||!this.isFocused()) return false;
		double clampedX=Math.clamp(this.getX()+this.width-1,this.getX(),mouseX-this.paddingX);
		return super.mouseDragged(clampedX,mouseY,button,dragX,dragY);
	}
}