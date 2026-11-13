package cz.maxtechnik.opm.config;

public class Anchor{
	public enum X{
		LEFT,
		MIDDLE,
		RIGHT,
	}
	public enum Y{
		TOP,
		MIDDLE,
		BOTTOM,
	}

	/**
	 * Určí vodorovnou kotvu podle středu widgetu v třetinách obrazovky.
	 */
	public static X snapX(int centerX,int screenWidth){
		if(centerX<screenWidth/3) return X.LEFT;
		if(centerX>(screenWidth*2)/3) return X.RIGHT;
		return X.MIDDLE;
	}

	/**
	 * Určí svislou kotvu podle středu widgetu v třetinách obrazovky.
	 */
	public static Y snapY(int centerY,int screenHeight){
		if(centerY<screenHeight/3) return Y.TOP;
		if(centerY>(screenHeight*2)/3) return Y.BOTTOM;
		return Y.MIDDLE;
	}

	/**
	 * Spočítá pixelový offset od vybrané vodorovné kotvy pro uložení do configu.
	 */
	public static int offsetX(X anchor,int boxX,int boxWidth,int screenWidth){
		return switch(anchor){
			case LEFT -> boxX;
			case RIGHT -> screenWidth-boxWidth-boxX;
			case MIDDLE -> (boxX+boxWidth/2)-(screenWidth/2);
		};
	}

	/**
	 * Spočítá pixelový offset od vybrané svislé kotvy pro uložení do configu.
	 */
	public static int offsetY(Y anchor,int boxY,int boxHeight,int screenHeight){
		return switch(anchor){
			case TOP -> boxY;
			case BOTTOM -> screenHeight-boxHeight-boxY;
			case MIDDLE -> (boxY+boxHeight/2)-(screenHeight/2);
		};
	}

	/**
	 * Převede kotvu a offset zpět na levý okraj X na obrazovce.
	 */
	public static int resolveX(X anchor,int offsetX,int boxWidth,int screenWidth){
		int x=switch(anchor){
			case LEFT -> offsetX;
			case RIGHT -> screenWidth-boxWidth-offsetX;
			case MIDDLE -> (screenWidth/2)+offsetX-(boxWidth/2);
		};
		return Math.clamp(x,0,Math.max(0,screenWidth-boxWidth));
	}

	/**
	 * Převede kotvu a offset zpět na horní okraj Y na obrazovce.
	 */
	public static int resolveY(Y anchor,int offsetY,int boxHeight,int screenHeight){
		int y=switch(anchor){
			case TOP -> offsetY;
			case BOTTOM -> screenHeight-boxHeight-offsetY;
			case MIDDLE -> (screenHeight/2)+offsetY-(boxHeight/2);
		};
		return Math.clamp(y,0,Math.max(0,screenHeight-boxHeight));
	}
}
