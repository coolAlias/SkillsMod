package coolalias.skillsmod.skills;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import coolalias.skillsmod.skills.active.SkillFireBlast;
import coolalias.skillsmod.skills.passive.SkillIronFlesh;

/**
 * @author coolAlias
 *
 * Abstract base skill class provides foundation for both passive and active skills,
 * as well as attributes. Most class fields are immutable, with the sole
 * exception of level.
 */
public abstract class SkillBase
{
	/** Enumerated Attribute codes; use CODE.ordinal for position */
	public static enum AttributeCode{STR,AGI,INT,CHA};
	
	/** Number of skills available, though not a firm maximum as more can be added to the ArrayList */
	public static final byte MAX_LEVEL = 5, MAX_ATTRIBUTE = 30, NUM_ATTRIBUTES = 4, NUM_PASSIVE_SKILLS = 28, MAX_NUM_SKILLS = 64;
	
	/** For convenience in providing initial id values */
	private static int skillIndex = NUM_ATTRIBUTES;
	
	/** Similar to itemsList in Item, giving easy access to any Skill */
	public static final SkillBase[] skillsList = new SkillBase[MAX_NUM_SKILLS];
	// TODO add and test descriptions or move getDescription to sub-classes to provide more specific information
	
	/** Construct and register base skill versions similar to vanilla Item class */
	/* ATTRIBUTES don't need to be public as they will be referenced by AttributeCode only */
	private static final SkillBase str = new Attribute("Strength", AttributeCode.STR).addDescription("Increases physical damage");
	private static final SkillBase agi = new Attribute("Agility", AttributeCode.AGI).addDescription("Increases speed and accuracy");
	private static final SkillBase wis = new Attribute("Intelligence", AttributeCode.INT);
	private static final SkillBase cha = new Attribute("Charisma", AttributeCode.CHA);
	
	/* PASSIVE SKILLS */
	public static final SkillBase ironFlesh = new SkillIronFlesh("Iron Flesh", (byte) skillIndex++, AttributeCode.STR, (byte) 1);
	//public static final SkillBase powerStrike = new SkillPassive("Power Strike", (byte) skillIndex++, AttributeCode.STR, (byte) 2);
	
	/* ACTIVE SKILLS */
	public static final SkillBase fireBlast = new SkillFireBlast("Fire Blast", (byte) skillIndex++, AttributeCode.INT, (byte) 1, 15);
	
	/** Contains descriptions for tooltip display */
	private List<String> tooltip = new ArrayList<String>();
	
	/** Skill's display name */
	public final String name;
	
	/** Internal id for skill; needed mainly for prerequisites */
	public final byte id;
	
	/** ID of the attribute skill tree to which this skill belongs; valid values are 0-3 */
	protected final AttributeCode attribute;
	
	/** Skill tier */
	protected final byte tier;
	
	/** Max level this skill can reach */
	protected final byte maxLevel;
	
	/** Mutable field storing current level for this instance of SkillBase */
	protected byte level = 0;
	
	/** List of ids for any skills required prior to acquiring this skill */
	// TODO implement prerequisites if other than generic algorithm
	// (e.g. base attribute score to unlock tier and at least one skill point in previous tier skill)
	// MOVE to non-abstract classes, add abstract getter() to SkillBase
	// protected List<Byte> prerequisites;
	
	/**
	 * Constructs immutable base skill with default max level and registers the skill to database
	 */
	public SkillBase(String name, byte id, AttributeCode attribute, byte tier) {
		this(name, id, attribute, tier, MAX_LEVEL, true);
	}
	
	/**
	 * Constructs immutable base skill with specified max level
	 * @param register - if true, the skill attempts to register to the database
	 */
	public SkillBase(String name, byte id, AttributeCode attribute, byte tier, byte maxLevel, boolean register)
	{
		this.name = name;
		this.id = id;
		this.attribute = attribute;
		this.tier = tier;
		this.maxLevel = maxLevel;
		
		if (register) {
			if (skillsList[id] != null) { System.out.println("CONFLICT @ " + id + " skill id already occupied by " + skillsList[id].name + " while adding " + this.name); }
			skillsList[id] = this;
		}
	}
	
	/** Returns a new instance of the skill with appropriate class type without registering it to the Skill database */
	public abstract SkillBase newInstance();
	
	/** Returns a new instance from NBT */
	public abstract SkillBase loadFromNBT(NBTTagCompound compound);
	
	/** Returns a new instance from a DataInputStream */
	public abstract SkillBase loadFromStream(byte id, DataInputStream inputStream) throws IOException;
	
	/** Returns skill tier */
	public final byte getTier() { return tier; }
	
	/** Returns max skill level */
	public final byte getMaxLevel() { return maxLevel; }
	
	/** Returns current skill level */
	public final byte getLevel() { return level; }
	
	/** Returns a copy of the list containing Strings for tooltip display */
	public final List<String> getDescription() { return new ArrayList<String>(tooltip); }
	
	/** Adds a single string to the skill's tooltip display */
	protected final SkillBase addDescription(String string) { tooltip.add(string); return this; }
	
	/** Adds all entries in the provided list to the skill's tooltip display */
	protected final SkillBase addDescription(List<String> list) { tooltip.addAll(list); return this; }
	
	/** Returns this skill's icon resource location */
	// TODO use generic path/name.png format to simplify classes
	public ResourceLocation getIconTexture() { return null; }
	
	/** Returns true if player meets requirements to learn this skill at target level */
	protected abstract boolean canIncreaseLevel(EntityPlayer player, int targetLevel);
	
	/** Increments the skill's level and applies any bonuses, reduction of Xp, etc. that is needed; only called if 'canIncreaseLevel' returns true */
	protected abstract void levelUp(EntityPlayer player, int targetLevel);
	
	/** Shortcut method to grant skill at current level + 1 */
	public final boolean grantSkill(EntityPlayer player) { return grantSkill(player, level + 1); }
	
	/**
	 * Returns true if skill's level has increased
	 */
	public final boolean grantSkill(EntityPlayer player, int targetLevel) {
		if (level <= targetLevel) { return false; }
		byte oldLevel = level;
		if (canIncreaseLevel(player, targetLevel)) {
			// TODO remove debug / integrate into HUD
			player.addChatMessage(name + " leveled up! Now level " + (level + 1));
			levelUp(player, targetLevel);
		}
		return oldLevel < level;
	}
	
	/**
	 * Writes mutable data to NBT. When overriding, make sure to call the super method as well.
	 */
	public void writeToNBT(NBTTagCompound compound) {
		// TODO remove debug
		System.out.println("Writing " + name + " to NBT with id " + id + " and level " + level);
		compound.setByte("id", id);
		compound.setByte("level", level);
	}
	
	/**
	 * Reads mutable data from NBT. When overriding, make sure to call the super method as well.
	 */
	public void readFromNBT(NBTTagCompound compound) {
		level = compound.getByte("level");
		// TODO remove debug
		System.out.println(this.name + " level from NBT: " + level);
	}
	
	/**
	 * Writes mutable data for this skill instance to the output stream; override to add further data
	 */
	public void writeToStream(DataOutputStream outputStream) throws IOException {
		outputStream.writeByte(id);
		outputStream.writeByte(level);
	}
}
