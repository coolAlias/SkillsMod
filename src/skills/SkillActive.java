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
		this(name, id, attribute, tier, MAX_LEVEL, cooldown, duration, isGlobal);
	}
	
	/**
	 * Full constructor for setting custom max level; registers new skill to the database
	 */
	protected SkillActive(String name, byte id, AttributeCode attribute, byte tier, byte maxLevel, int cooldown, int duration, boolean isGlobal) {
		super(name, id, attribute, tier, maxLevel, true);
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
	
	/** Returns true if this skill is currently active, however that is defined by the child class */
	public abstract boolean isActive();
	
	/**
	 * This method is called when the skill is used; override to specify effect(s), but be
	 * sure to make a call to super class.
	 * NOTE that it can be used to activate a skill the player does not have - use SkillInfo's
	 * activateSkill method instead to ensure the skill used is the player's
	 * @return true if skill was successfully activated
	 */
	public boolean activate(World world, EntityPlayer player)
	{
		if (canUse(player)) {
			// TODO implement global cooldown differently
			if (isGlobal) SkillInfo.get(player).setGlobalCooldown(cooldown);
			setCooldown(player, cooldown);
			// TODO remove debug
			player.addChatMessage(this.name + " used! Cooldown time set to " + getCooldown() + " ticks. World remote? " + player.worldObj.isRemote);
			return true;
		}
		player.addChatMessage("Can't currently use " + name);
		return false;
	}
	
	/** Returns true if this skill can currently be used by the player; override to add further conditions */
	public boolean canUse(EntityPlayer player) { return !isCooling(); }
	
	/** This method should be called from the player's update tick */
	public void onUpdate(EntityPlayer player) { decrementCooldown(); }
	
	/** Returns time (in ticks) required before this skill may be used again */
	public final int getCooldown() { return countdown; }
	
	/** Sets time (in ticks) required until this skill can be activated again */
	public void setCooldown(EntityPlayer player, int time) { countdown = time - (SkillInfo.get(player).getSkillLevel(SkillBase.skillsList[AttributeCode.INT.ordinal()]) * 4); }
	
	/** Returns maximum duration of this skill's effect (in ticks) */
	public final int getDuration() { return duration; }
	
	/** Returns true if skill is currently cooling down */
	public boolean isCooling() { return countdown > 0; }
	
	/**
	 * Decrements countdown timer tracking cooldown
	 */
	protected void decrementCooldown() {
		if (isCooling()) {
			--countdown;
			// TODO remove debug
			if (countdown % 20 == 0)
				System.out.println(name + " cooling down, " + countdown / 20 + " seconds remaining.");
		}
	}
	
	@Override
	public boolean canIncreaseLevel(EntityPlayer player, int targetLevel) {
		return level + 1 == targetLevel && targetLevel <= maxLevel && checkPrerequisites(player);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setInteger("countdown", countdown);
	}
	
	// TODO handle targetLevel possibilities other than just level + 1
	@Override
	protected void levelUp(EntityPlayer player, int targetLevel) { ++level; }
	
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
