package coolalias.skillsmod.skills;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeInstance;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import coolalias.skillsmod.SkillInfo;

/**
 * 
 * @author coolAlias
 *
 * Each SkillAttribute object is an individually leveling skill, maintaining its own experience
 * count and leveling up automatically when enough xp accumulates.
 * 
 * If the SkillAttribute has an AttributeModifier, the modifier will be applied at a set amount
 * each level to the supplied entity Attribute.
 */
public final class SkillAttribute extends SkillBase 
{
	/** If not null, the modifier is applied each time the SkillAttribute levels up */
	private AttributeModifier modifier;
	
	/** SharedMonsterAttribute or other Minecraft entity Attribute to be modified by AttributeModifier */
	private Attribute toModify;
	
	/** Amount AttributeModifier will apply per skill Level */
	private double amount;
	
	/** Stores current xp and xp needed for next level */
	private float xp = 0F, nextXp;
	
	/** Constructs immutable SkillAttribute instance and registers it to the skill database */
	protected SkillAttribute(String name, AttributeCode code) {
		this(name, code, null, null, 0.0D);
	}
	
	/**
	 * Constructs instance of SkillAttribute with corresponding AttributeModifier, adding 'amount' to Attribute at each level
	 */
	protected SkillAttribute(String name, AttributeCode code, AttributeModifier modifier, Attribute toModify, double amount)
	{
		super(name, (byte) code.ordinal(), code, (byte) 0, MAX_ATTRIBUTE, true);
		nextXp = calculateNextXp();
		this.modifier = modifier;
		if (this.modifier != null) { this.modifier.setSaved(true); }
		this.toModify = toModify;
		this.amount = amount;
	}
	
	private SkillAttribute(SkillAttribute skill)
	{
		super(skill.name, skill.id, skill.attribute, skill.tier, skill.maxLevel, false);
		nextXp = calculateNextXp();
		this.modifier = skill.modifier;
		this.toModify = skill.toModify;
		this.amount = skill.amount;
	}
	
	@Override
	public final SkillAttribute newInstance() { return new SkillAttribute(this); }
	
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
	}
	
	@Override
	public final SkillAttribute loadFromNBT(NBTTagCompound compound) {
		SkillAttribute skill = new SkillAttribute((SkillAttribute) skillsList[compound.getByte("id")]);
		skill.readFromNBT(compound);
		return skill;
	}
	
	@Override
	public final void writeToStream(DataOutputStream outputStream) throws IOException {
		super.writeToStream(outputStream);
		outputStream.writeFloat(xp);
	}
	
	@Override
	public final SkillAttribute loadFromStream(byte id, DataInputStream inputStream) throws IOException
	{
		SkillAttribute skill = new SkillAttribute((SkillAttribute) skillsList[id]);
		skill.level = inputStream.readByte();
		skill.xp = inputStream.readFloat();
		skill.nextXp = skill.calculateNextXp();
		return skill;
	}
	
	/** Returns current XP amount for an instance of SkillAttribute */
	public final float getXp() { return xp; }
	
	/** Returns current XP required for next level for an instance of SkillAttribute */
	public final float getNextXp() { return nextXp; }
	
	/** Adds amount to XP, even if negative. Won't go below zero. */
	private final void addXp(float amount) { if (level < maxLevel) xp = MathHelper.clamp_float(xp + amount, 0, Float.MAX_VALUE); }
	
	/** Calculates amount of XP needed to achieve the next level */
	// TODO refine leveling algorithm
	private final float calculateNextXp() { return (float) Math.pow(this.level, 2) + 1; }
	
	/**
	 * Adds XP and increases skill level if applicable, as well as calling the player's SkillInfo levelUp method
	 * @return true if skill level was incremented
	 */
	public final void addXp(EntityPlayer player, float amount) {
		addXp(amount);
		if (grantSkill(player)) { SkillInfo.get(player).levelUp(); }
	}
	
	/**
	 * Levels the SkillAttribute and reduces Xp accordingly until no more levels can be gained
	 */
	@Override
	protected final void levelUp(EntityPlayer player, int targetLevel)
	{
		float oldXp = nextXp;	
		++level;
		nextXp = calculateNextXp();
		addXp(player, -oldXp);
		addAttributeModifiers(player);
	}
	
	/**
	 * Levels up and applies the skill's AttributeModifier, if any
	 */
	private void addAttributeModifiers(EntityPlayer player)
	{
		if (this.modifier != null && this.toModify != null) {
			AttributeInstance attributeinstance = player.getEntityAttribute(this.toModify);
			if (attributeinstance.getModifier(this.modifier.getID()) != null) { attributeinstance.removeModifier(this.modifier); }
			AttributeModifier newModifier = (new AttributeModifier(this.modifier.getID(), this.modifier.getName(), level * this.amount, 0)).setSaved(true);
			attributeinstance.applyModifier(newModifier);
			player.addChatMessage("Current damage bonus: +" + newModifier.getAmount());
		}
	}
}
