package coolalias.skillsmod.skills;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import coolalias.skillsmod.SkillInfo;

/**
 * 
 * @author coolAlias
 * 
 * Base class for active skills. Extend this class to add specific functionality.
 * 
 * Inherited fields are immutable, but local class fields are mutable.
 * 
 * Note that any additional fields in child classes should probably be immutable.
 *
 */
public abstract class SkillActive extends SkillBase
{
	/** The amount of time it takes before the skill can be used again */
	protected final int cooldown;
	
	/** Duration the skill effect will remain active, if any;
	 * effect duration decremented on the affected entity's update */
	protected final int duration;
	
	/** Timer for cooldowns; skill can be used when the countdown is at zero */
	protected int countdown = 0;
	
	/** If true, no active skills will be usable while this skill is cooling down */
	private final boolean isGlobal;
	
	/**
	 * Constructs a new immutable active skill with zero duration and registers it to the skill database
	 * @param cooldown value is in seconds
	 */
	protected SkillActive(String name, byte id, AttributeCode attribute, byte tier, int cooldown) {
		this(name, id, attribute, tier, cooldown, 0, false);
	}
	
	/**
	 * Constructs a new immutable active skill and registers it to the skill database
	 * @param cooldown value is in seconds
	 * @param duration value is in seconds
	 */
	protected SkillActive(String name, byte id, AttributeCode attribute, byte tier, int cooldown, int duration) {
		this(name, id, attribute, tier, cooldown, duration, false);
	}
	
	/**
	 * Constructs a new immutable active skill and registers it to the skill database
	 * @param cooldown value is in seconds
	 * @param duration value is in seconds
	 */
	protected SkillActive(String name, byte id, AttributeCode attribute, byte tier, int cooldown, int duration, boolean isGlobal) {
		super(name, id, attribute, tier, MAX_LEVEL, true);
		this.cooldown = cooldown * 20;
		this.duration = duration * 20;
		this.isGlobal = isGlobal;
	}
	
	protected SkillActive(SkillActive skill) {
		super(skill.name, skill.id, skill.attribute, skill.tier, skill.maxLevel, false);
		this.cooldown = skill.cooldown;
		this.duration = skill.duration;
		this.isGlobal = skill.isGlobal;
	}
	
	/**
	 * This method is called when the skill is used; override to specify effect(s), but be
	 * sure to make a call to super class
	 * @return true if skill was successfully activated
	 */
	public boolean activate(World world, EntityPlayer player) {
		// TODO implement global cooldown differently
		if (isGlobal) SkillInfo.get(player).setGlobalCooldown(cooldown);
		setCooldown(cooldown);
		// TODO remove debug
		player.addChatMessage(this.name + " used! Cooldown time set to " + getCooldown() + " ticks. World remote? " + player.worldObj.isRemote);
		return true;
	}
	
	/** Returns true if this skill can currently be used by the player; override to add further conditions */
	public boolean canUse(EntityPlayer player) { return !isCooling(); }
	
	/** This method should be called from the player's update tick */
	public void onUpdate(EntityPlayer player) { decrementCooldown(); }
	
	/** Returns time (in ticks) required before this skill may be used again */
	public final int getCooldown() { return countdown; }
	
	/** Sets time (in ticks) required until this skill can be activated again */
	public final void setCooldown(int time) { countdown = time; }
	
	/** Returns maximum duration of this skill's effect (in ticks) */
	public final int getDuration() { return duration; }
	
	/** Returns true if skill is currently cooling down */
	public boolean isCooling() { return countdown > 0; }
	
	/**
	 * Decrements countdown timer tracking cooldown
	 */
	protected void decrementCooldown() {
		// TODO cooldown rate should be affected by other skills, but only calculate when setting the initial time to cool down
		if (isCooling()) { --countdown; }
	}
	
	@Override
	public boolean canLearn(EntityPlayer player, int targetLevel) {
		return level + 1 == targetLevel && targetLevel <= maxLevel;
	}
	
	@Override
	public void writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		// TODO remove debug
		System.out.println("Writing SkillActive to NBT");
		compound.setInteger("countdown", countdown);
	}
	
	@Override
	protected boolean levelUp(EntityPlayer player) { return canLearn(player, 1); }
	
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		countdown = compound.getInteger("countdown");
	}
	
	@Override
	public void writeToStream(DataOutputStream outputStream) throws IOException {
		super.writeToStream(outputStream);
		outputStream.writeInt(countdown);
	}
	
	@Override
	public SkillActive loadFromNBT(NBTTagCompound compound) {
		SkillActive skill = (SkillActive) skillsList[compound.getByte("id")].newInstance();
		skill.readFromNBT(compound);
		return skill;
	}
	
	@Override
	public SkillActive loadFromStream(byte id, DataInputStream inputStream) throws IOException {
		SkillActive skill = (SkillActive) skillsList[id].newInstance();
		skill.level = inputStream.readByte();
		skill.countdown = inputStream.readInt();
		return skill;
	}
}
