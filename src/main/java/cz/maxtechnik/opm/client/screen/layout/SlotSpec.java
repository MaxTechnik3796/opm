package cz.maxtechnik.opm.client.screen.layout;

public final class SlotSpec{
	private final boolean hasCount;
	private final boolean hasChance;
	private final boolean isFluid;
	private SlotSpec(boolean hasCount,boolean hasChance,boolean isFluid){
		this.hasCount=hasCount;
		this.hasChance=hasChance;
		this.isFluid=isFluid;
	}
	public static SlotSpec item(){
		return new SlotSpec(false,false,false);
	}
	public static SlotSpec result(){
		return new SlotSpec(false,false,false);
	}
	public static SlotSpec fluid(){
		return new SlotSpec(false,false,true);
	}
	public SlotSpec withCount(){
		return new SlotSpec(true,this.hasChance,this.isFluid);
	}
	public SlotSpec withChance(){
		return new SlotSpec(this.hasCount,true,this.isFluid);
	}
	public boolean hasCount(){
		return hasCount;
	}
	public boolean hasChance(){
		return hasChance;
	}
	public boolean isFluid(){
		return isFluid;
	}
}