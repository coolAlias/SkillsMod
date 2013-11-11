package coolalias.skillsmod.skills;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import coolalias.skillsmod.SkillInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;

/**
 * 
 * @author coolAlias
 *
 * Inherited fields are immutable, but local fields are mutable
 * 
 * Final class, but if using SharedMonsterAttributes or something similar to add modifiers
 * to the player rather than tickhandler / event stuff, it might be better to make this
 * class abstract instead allowing each sub-class to specify level up behavior by calling
 * an abstract method from within the final levelUp(EntityPlayer) to maintain xp behavior
 * 
 */
public final class Attribute extends SkillBase 
{
	/** Stores current xp and xp needed for next level */
	private float xp = 0F, nextXp;
	
	/**
	 * Constructs immutable Attribute instance and registers it to the skill database
	 */
	protected Attribute(String name, AttributeCode code) {
		super(name, (byte) code.ordinal(), code, (byte) 0, MAX_ATTRIBUTE, true);
		nextXp = calculateNextXp();
	}
	
	private Attribute(SkillBase skill) {
		super(skill.name, skill.id, skill.attribute, skill.tier, skill.maxLevel, false);
		nextXp = calculateNextXp();
	}
	
	@Override
	public final Attribute newInstance() { return new Attribute(this); }
	
	@Override
	public final boolean canIncreaseLevel(EntityPlayer player, int targetLevel) { return xp >= nextXp && level < maxLevel; }
	
	@Override
	public final void writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setFloat("xp", xp);
	}
	
	@Override
	public final void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		xp = compound.getFloat("xp");
		nextXp = calculateNextXp();
		// TODO remove debug; integrate XP display into HUD
		System.out.println(this.name + " XP from NBT: " + xp + "/" + nextXp);
	}
	
	@Override
	public final Attribute loadFromNBT(NBTTagCompound compound) {
		Attribute skill = new Attribute(skillsList[compound.getByte("id")]);
		skill.readFromNBT(compound);
		return skill;
	}
	
	@Override
	public final void writeToStream(DataOutputStream outputStream) throws IOException {
		super.writeToStream(outputStream);
		outputStream.writeFloat(xp);
	}
	
	@Override
	public final Attribute loadFromStream(byte id, DataInputStream inputStream) throws IOException {
		Attribute skill = new Attribute(skillsList[id]);
		skill.level = inputStream.readByte();
		skill.xp = inputStream.readFloat();
		skill.nextXp = skill.calculateNextXp();
		return skill;
	}
	
	/** Returns current XP amount for an instance of Attribute */
	public final float getXp() { return xp; }
	
	/** Returns current XP required for next level for an instance of Attribute */
	public final float getNextXp() { return nextXp; }
	
	/** Adds amount to XP, even if negative. Won't go below zero. */
	private final void addXp(float amount) { xp = MathHelper.clamp_float(xp + amount, 0, Float.MAX_VALUE); }
	
	/** Calculates amount of XP needed to achieve the next level */
	// TODO refine leveling algorithm
	private final float calculateNextXp() { return (float) Math.pow(this.level, 2) + 1; }
	
	/**
	 * Adds XP and increases skill level if applicable, as well as calling the player's SkillInfo levelUp method
	 * @return true if skill level was incremented
	 */
	public final void addXp(EntityPlayer player, float amount)
	{
		addXp(amount);
		// TODO remove debug / integrate messages into HUD display
		System.out.println("Client? " + player.worldObj.isRemote + ", " + amount + " " + name + " xp gained! Current XP: " + xp + "/" + nextXp);
		if (grantSkill(player)) { SkillInfo.get(player).levelUp(); }
	}
	
	/**
	 * Levels the Attribute and reduces Xp accordingly until no more levels can be gained
	 */
	@Override
	protected final void levelUp(EntityPlayer player, int targetLevel) {
		float oldXp = nextXp;	
		++level;
		nextXp = calculateNextXp();
		addXp(player, -oldXp);
	}
}
