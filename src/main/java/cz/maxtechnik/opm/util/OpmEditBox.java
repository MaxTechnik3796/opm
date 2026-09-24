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
	private int paddingY=-1;
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
	public void setPaddingY(int paddingY){
		this.paddingY=paddingY;
	}
	public void setPadding(int paddingX,int paddingY){
		this.paddingX=paddingX;
		this.paddingY=paddingY;
	}
	@Override
	public int getInnerWidth(){
		return Math.max(1,this.width-this.paddingX*2);
	}
	public int getPaddingY(){
		if(this.paddingY>=0) return this.paddingY;
		return Math.max(0,(this.height-8)/2);
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
			double clampedX=Math.clamp(mouseX-this.paddingX,this.getX(),this.getX()+this.width-1);
			double clampedY=Math.clamp(mouseY-getPaddingY(),this.getY(),this.getY()+this.height-1);
			return super.mouseClicked(clampedX,clampedY,button);
		}
		return super.mouseClicked(mouseX,mouseY,button);
	}
	@Override
	public boolean mouseDragged(double mouseX,double mouseY,int button,double dragX,double dragY){
		if(!this.active||!this.visible||!this.isFocused()) return false;
		double clampedX=Math.clamp(mouseX-this.paddingX,this.getX(),this.getX()+this.width-1);
		double clampedY=Math.clamp(mouseY-getPaddingY(),this.getY(),this.getY()+this.height-1);
		return super.mouseDragged(clampedX,clampedY,button,dragX,dragY);
	}
}