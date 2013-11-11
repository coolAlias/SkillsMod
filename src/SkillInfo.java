package coolalias.skillsmod;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import coolalias.skillsmod.skills.Attribute;
import coolalias.skillsmod.skills.SkillActive;
import coolalias.skillsmod.skills.SkillBase;
import coolalias.skillsmod.skills.SkillBase.AttributeCode;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 
 * @author coolAlias
 *
 * This class is a placeholder until I port over to Quest's ISection 'SectionSkills'
 * 
 */
public class SkillInfo implements IExtendedEntityProperties
{
	public final static String EXT_PROP_NAME = "SkillInfo";
	
	/** Maximum number of skill points that can be gained by increasing Attribute levels */
	private static final int MAX_SKILL_POINTS = 100;
	
	/** Stores information on the player's Attributes and Passive Skills */
	private final Map<Byte, SkillBase> baseSkills = new HashMap<Byte, SkillBase>(SkillBase.NUM_PASSIVE_SKILLS);
	private final Map<Byte, SkillActive> activeSkills = new HashMap<Byte, SkillActive>();
	
	private final EntityPlayer player;
	
	/** Keeps track of total character level (sum of all Attribute levels) */
	private byte totalLevel = 0;
	
	/** Number of skill points currently unallocated */
	private byte skillPoints = 0;
	
	/** Keeps track of global cooldown, if any */
	private int globalCooldown = 0;

	public SkillInfo(EntityPlayer player)
	{
		this.player = player;
		
		for (int i = 0; i < SkillBase.NUM_ATTRIBUTES; ++i)
			baseSkills.put(SkillBase.skillsList[i].id, SkillBase.skillsList[i].newInstance());
		
		if (player.worldObj.isRemote) { initXpBuffer(); }
	}
	
	/** Returns a copy of this player's base skills map */
	public Map<Byte, SkillBase> getBaseSkills() { return new HashMap<Byte, SkillBase>(baseSkills); }
	
	/** Returns a copy of this player's active skills map */
	public Map<Byte, SkillActive> getActiveSkills() { return new HashMap<Byte, SkillActive>(activeSkills); }
	
	/** Returns true if the player has at least one level in the specified skill (of any class) */
	public boolean hasSkill(byte id) { return baseSkills.containsKey(id) || activeSkills.containsKey(id); }
	
	/**
	 * Grants skill to player regardless of whether player meets base requirements
	 */
	public void grantSkill(byte id)
	{
		Map map = SkillBase.skillsList[id] instanceof SkillActive ? activeSkills : baseSkills;
		SkillBase skill = map.containsKey(id) ? (SkillBase) map.remove(id) : SkillBase.skillsList[id].newInstance();
		
		if (skill.grantSkill(player)) { player.addChatMessage(skill.name + " level has increased to level " + skill.getLevel() + "!"); }
		else { player.addChatMessage("Skill " + skill.name + " is already at the maximum level."); }
		
		map.put(id, skill);
	}
	
	public boolean activateSkill(World world, byte id)
	{
		player.addChatMessage("Activating skill " + id + ". World remote? " + player.worldObj.isRemote);
		if (activeSkills.containsKey(id)) {
			// TODO add canUse check to prevent using while cooling down
			// && ((SkillActive) skills[id]).canUse(player)) {
			return ((SkillActive) activeSkills.get(id)).activate(world, player);
		} else {
			System.out.println("WARNING: attempting to activate unlearned or non-active skill " + id);
			return false;
		}
	}
	
	/** Returns current total character level */
	public byte getCharacterLevel() { return totalLevel; }
	
	/**
	 * Recalculates total character level and stores the value in class field 'totalLevel'
	 */
	// could just save the value in NBT; this way provides method of validation in case of
	// save corruption / editing (probably unnecessary)
	private void calculateCharacterLevel() {
		totalLevel = 0;
		for (int i = 0; i < SkillBase.NUM_ATTRIBUTES; ++i) { totalLevel += baseSkills.get(i).getLevel(); }
	}
	
	/** Returns current number of skill points */
	public byte getSkillPoints() { return skillPoints; }
	
	/**
	 * Returns true if skill point decremented successfully
	 */
	// TODO might want to change this implementation depending on how gui wants to interact
	public boolean spendSkillPoint()
	{
		if (skillPoints > 0) {
			--skillPoints;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Adds Xp amount to the corresponding attribute by enum type
	 */
	public void addXp(float amount, AttributeCode attribute) { addXp(amount, (byte) attribute.ordinal()); }
	
	/**
	 * Adds Xp amount to the corresponding attribute by id (ordinal position in the enum type)
	 */
	public void addXp(float amount, byte id) throws IllegalArgumentException
	{
		if (id < SkillBase.NUM_ATTRIBUTES && baseSkills.containsKey(id))
		{
			if (this.player.worldObj.isRemote) {
				addClientXp(amount, id);
			} else {
				Attribute attribute = (Attribute) baseSkills.get(id);
				if (attribute.addXp(player, amount)) {
					++totalLevel;
					if (totalLevel <= MAX_SKILL_POINTS)
						++skillPoints;
					// TODO send levelUp packet to client to sync character level and skill points
					// TODO remove chat message; integrate with level up message in HUD
					player.addChatMessage("Current unallocated skill points: " + skillPoints);
				}
				PacketHandler.sendAttributePacket(player, attribute);
				baseSkills.put(id, attribute);
			}
		} else {
			throw new IllegalArgumentException("SEVERE: ID value of " + id + " is not a valid attribute id!");
		}
	}
	
	/** Buffer holding small amounts of XP; when it reaches the THRESHOLD, will be sent to the server */
	@SideOnly(Side.CLIENT)
	private float[] xpBuffer;
	
	/** Amount of Xp after which the client will send update packet to server */
	//@SideOnly(Side.CLIENT) // can't make this final and client side at the same time
	private static final float THRESHOLD = 0.01F;
	
	/** Initializes xpBuffer array and sets initial values to zero */
	@SideOnly(Side.CLIENT)
	private void initXpBuffer() {
		xpBuffer = new float[SkillBase.NUM_ATTRIBUTES];
		for (int i = 0; i < xpBuffer.length; ++i) { xpBuffer[i] = 0.0F; }
	}
	
	/**
	 * Client side accumulates XP in a buffer, sending packet to server when it exceeds a certain threshold
	 */
	@SideOnly(Side.CLIENT)
	private void addClientXp(float amount, byte id) {
		xpBuffer[id] += amount;
		if (xpBuffer[id] > THRESHOLD) {
			System.out.println("Client xp " + xpBuffer[id] + " exceeds threshold, sending to server and clearing buffer");
			PacketHandler.sendAddXpPacket(player, xpBuffer[id], id);
			xpBuffer[id] = 0.0F;
		}
	}
	
	/**
	 * Reads single Attribute from stream and updates the local baseSkills map
	 * Should only be needed on the client side to update from server
	 * TODO generalize to update any type of skill (should only be needed for Attributes, though, as gaining other skills is done on both sides?)
	 */
	public void updateAttributeFromStream(DataInputStream inputStream) throws IOException, IllegalArgumentException
	{
		byte id = inputStream.readByte();
		if (id < SkillBase.NUM_ATTRIBUTES) {
			baseSkills.put(id, SkillBase.skillsList[id].loadFromStream(id, inputStream));
			System.out.println("Attribute read from stream: " + baseSkills.get(id).name + ", current level: " + baseSkills.get(id).getLevel() + ", current XP: " + ((Attribute) baseSkills.get(id)).getXp());
			//Attribute attribute = (Attribute) SkillBase.skillsList[id].loadFromStream(id, inputStream);
			// TODO this way won't update character level / skill points
			//baseSkills.put(id, attribute);
		} else {
			throw new IllegalArgumentException("Updating attribute from packet contains invalid id " + id);
		}
	}
	
	/**
	 * This method should be called every update tick;
	 * currently called from LivingUpdateEvent
	 */
	public void onUpdate()
	{
		if (isCooling()) {
			System.out.println("Global cooldown in effect; time remaining " + globalCooldown);
			decrementCooldown();
		} else {
			for (SkillActive skill : activeSkills.values()) {
				System.out.println("Updating active skill " + skill.name);
				skill.onUpdate(player);
			}
		}
	}
	
	/** Sets global cooldown timer */
	public void setGlobalCooldown(int time) { globalCooldown = time; }
	
	/** Returns true if global cooldown currently in effect */
	public boolean isCooling() { return globalCooldown > 0; }
	
	/** Decrements global cooldown; doesn't check if isCooling */
	private void decrementCooldown() { --globalCooldown; }

	/**
	 * Used to register these extended properties for the player during EntityConstructing event
	 */
	public static final void register(EntityPlayer player) {
		player.registerExtendedProperties(SkillInfo.EXT_PROP_NAME, new SkillInfo(player));
	}

	/**
	 * Returns ExtendedPlayer properties for player
	 */
	public static final SkillInfo get(EntityPlayer player) {
		return (SkillInfo) player.getExtendedProperties(EXT_PROP_NAME);
	}

	@Override
	public final void saveNBTData(NBTTagCompound compound)
	{
		NBTTagList baseList = new NBTTagList();
		for (SkillBase skill : baseSkills.values()) {
			NBTTagCompound skillTag = new NBTTagCompound();
			skill.writeToNBT(skillTag);
			baseList.appendTag(skillTag);
		}
		
		NBTTagList activeList = new NBTTagList();
		for (SkillBase skill : activeSkills.values()) {
			NBTTagCompound skillTag = new NBTTagCompound();
			skill.writeToNBT(skillTag);
			activeList.appendTag(skillTag);
		}
		
		compound.setTag("BaseSkills", baseList);
		compound.setTag("ActiveSkills", activeList);
		compound.setByte("SkillPoints", skillPoints);
		compound.setInteger("GlobalCooldown", globalCooldown);
	}

	@Override
	public final void loadNBTData(NBTTagCompound compound)
	{
		NBTTagList skills = compound.getTagList("BaseSkills");
		for (int i = 0; i < skills.tagCount(); ++i) {
			NBTTagCompound skill = (NBTTagCompound) skills.tagAt(i);
			byte id = skill.getByte("id");
			baseSkills.put(id, SkillBase.skillsList[id].loadFromNBT(skill));
		}
		
		skills = compound.getTagList("ActiveSkills");
		for (int i = 0; i < skills.tagCount(); ++i) {
			NBTTagCompound skill = (NBTTagCompound) skills.tagAt(i);
			byte id = skill.getByte("id");
			activeSkills.put(id, ((SkillActive) SkillBase.skillsList[id]).loadFromNBT(skill));
		}
		
		calculateCharacterLevel();
		this.skillPoints = compound.getByte("SkillPoints");
		this.globalCooldown = compound.getInteger("GlobalCooldown");
	}
	
	/**
	 * Writes all pertinent data to output stream
	 */
	public void writeToStream(DataOutputStream outputStream) throws IOException
	{
		outputStream.writeInt(baseSkills.size());
		for (SkillBase skill : baseSkills.values()) { skill.writeToStream(outputStream); }
		outputStream.writeInt(activeSkills.size());
		for (SkillActive skill : activeSkills.values()) { skill.writeToStream(outputStream); }
		outputStream.writeByte(skillPoints);
	}
	
	/**
	 * Loads all pertinent data from input stream into current instance
	 */
	public void readFromStream(DataInputStream inputStream) throws IOException
	{
		int count = inputStream.readInt();
		
		for (int i = 0; i < count; ++i) {
			byte id = inputStream.readByte();
			baseSkills.put(id, SkillBase.skillsList[id].loadFromStream(id, inputStream));
		}
		
		count = inputStream.readInt();
		for (int i = 0; i < count; ++i) {
			byte id = inputStream.readByte();
			activeSkills.put(id, ((SkillActive) SkillBase.skillsList[id]).loadFromStream(id, inputStream));
		}
		
		skillPoints = inputStream.readByte();
	}

	@Override
	public void init(Entity entity, World world) {}

	/**
	 * Makes it look nicer in the methods save/loadProxyData
	 */
	private static final String getSaveKey(EntityPlayer player) {
		return player.username + ":" + EXT_PROP_NAME;
	}

	/**
	 * Does everything I did in onLivingDeathEvent and it's static,
	 * so you now only need to use the following in the above event:
	 * ExtendedPlayer.saveProxyData((EntityPlayer) event.entity));
	 */
	public static final void saveProxyData(EntityPlayer player)
	{
		SkillInfo playerData = SkillInfo.get(player);
		NBTTagCompound savedData = new NBTTagCompound();

		playerData.saveNBTData(savedData);
		CommonProxy.storeEntityData(getSaveKey(player), savedData);
	}

	/**
	 * This cleans up the onEntityJoinWorld event by replacing most of the code
	 * with a single line: ExtendedPlayer.loadProxyData((EntityPlayer) event.entity));
	 */
	public static final void loadProxyData(EntityPlayer player)
	{
		SkillInfo playerData = SkillInfo.get(player);
		NBTTagCompound savedData = CommonProxy.getEntityData(getSaveKey(player));

		if (savedData != null) { playerData.loadNBTData(savedData); }
		
		playerData.sync();
	}

	public final void sync() {
		System.out.println("Syncing skill info.");
		if (!player.worldObj.isRemote) { PacketHandler.sendSyncSkillsPacket(player); }
	}
}