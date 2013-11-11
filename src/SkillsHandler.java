package coolalias.skillsmod;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent;
import coolalias.skillsmod.skills.SkillBase.AttributeCode;

/**
 * 
 * @author coolAlias
 *
 * Handles granting XP for attributes under various conditions.
 * 
 * NOTE: Movement ONLY grants xp client side in some cases and is currently not saved
 * This applies to jumping, swimming (only when checking motion and not just isInWater()),
 * sneaking but NOT to sprinting
 * 
 * NOTE: Killing an entity with anything other than a bow or physical damage may not register
 * the player as the entity inflicting the damage, preventing the player from receiving xp even
 * if responsible (such as setting a mob on fire with flint and steel)
 *
 */
public class SkillsHandler
{	
	/** Base XP values for low, medium and high value activities */
	public static final float XP_LOW = 0.01F, XP_MED = 0.5F, XP_HIGH = 1F;
	/** Modifier for sustained activities; using for 10 ticks gives same experience as a single use type */
	public static final float XP_TICK = 0.01F;
	/** Modifier for damage dealt. Compensates for multiplying by the damage amount so XP value isn't too high */
	public static final float XP_DMG_DEAL = 0.5F;
	/** Modifier for damage received. Also applies to blocking. Compensates for multiplying by the damage amount so XP value isn't too high */
	public static final float XP_DMG_TAKE = 0.25F;
	/** Modifier for mining. Compensates for multiplying by the block's hardness to keep XP value reasonable. */
	public static final float XP_MINING = 0.1F;
	
	/** Flag to check whether to add XP or not on damage dealt / received */
	public static final int NO_TYPE = -1;
	
	/** DamageSource type to attributeXP Map; can use both for dealing and receiving damage */
	private static final Map<String, AttributeCode> xpTypeFromDamage = new HashMap<String, AttributeCode>();
	
	public static final void addDamageToAttributeMapping(DamageSource source, AttributeCode attribute) {
		addDamageToAttributeMapping(source.getDamageType(), attribute);
	}
	
	public static final void addDamageToAttributeMapping(String damageType, AttributeCode attribute)
	{
		if (xpTypeFromDamage.containsKey(attribute)) { System.out.println("WARNING: Overriding current mapping for " + damageType + " with attribute id " + attribute); }
		xpTypeFromDamage.put(damageType, attribute);
	}
	
	static {
		addDamageToAttributeMapping("player", AttributeCode.STR);
		addDamageToAttributeMapping("mob", AttributeCode.STR);
		addDamageToAttributeMapping("fall", AttributeCode.AGI);
		addDamageToAttributeMapping("magic", AttributeCode.INT);
		addDamageToAttributeMapping("fireball", AttributeCode.INT);
	}
	
	/**
	 * Determines attribute to which xp should be added, or returns NO_TYPE (-1) if not applicable
	 */
	private int getXpType(DamageSource source)
	{
		int xpType = xpTypeFromDamage.containsKey(source.getDamageType()) ? xpTypeFromDamage.get(source.getDamageType()).ordinal() : NO_TYPE;
		
		// catch unmapped generic types if possible
		if (xpType == NO_TYPE) {
			if (source.isMagicDamage()) { xpType = AttributeCode.INT.ordinal(); }
			else if (source.isProjectile()) { xpType = AttributeCode.AGI.ordinal(); }
		}
		
		return xpType;
	}
	
	// TODO this may not be a problem, test further: make sure to add Xp on only one side to prevent double Xp in single player
	
	@ForgeSubscribe
	public void onArrowLooseEvent(ArrowLooseEvent event) {
		SkillInfo.get(event.entityPlayer).addXp(XP_LOW, AttributeCode.AGI);
	}
	
	/**
	 * This event is called when an entity is attacked by another entity
	 */
	@ForgeSubscribe
	public void onAttacked(LivingAttackEvent event)
	{
		if (event.entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.entity;
			if (player.isBlocking()) { SkillInfo.get(player).addXp(XP_LOW * event.ammount * XP_DMG_TAKE, AttributeCode.STR); }
		}
	}
	
	@ForgeSubscribe
	public void onHurt(LivingHurtEvent event)
	{
		// probably don't need this first check...
		if (event.ammount > 0F)
		{
			int xpType = getXpType(event.source);
			
			if (xpType != NO_TYPE)
			{
				// Damaged entity is a player
				if (event.entity instanceof EntityPlayer) {
					System.out.println("Player suffered " + event.ammount + " damage.");
					SkillInfo.get((EntityPlayer) event.entity).addXp(XP_LOW * event.ammount * XP_DMG_TAKE, (byte) xpType);
				}
				// Entity damaged by a player
				if (event.source.getEntity() instanceof EntityPlayer) {
					System.out.println("getEntity Player damaged entity for " + event.ammount + " damage.");
					SkillInfo.get((EntityPlayer) event.source.getEntity()).addXp(XP_LOW * event.ammount * XP_DMG_DEAL, (byte) xpType);
				}
				// TODO remove the following; only for debug as SourceOfDamage isn't needed
				if (event.source.getSourceOfDamage() instanceof EntityPlayer) {
					System.out.println("getSourceOfDamge Player damaged entity for " + event.ammount + " damage.");
					SkillInfo.get((EntityPlayer) event.source.getSourceOfDamage()).addXp(XP_LOW * event.ammount * XP_DMG_DEAL, (byte) xpType);
				}
			}
		}
	}
	
	@ForgeSubscribe
	public void onDeath(LivingDeathEvent event)
	{
		// Player killed the entity
		if (event.source.getEntity() instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.source.getEntity();
			// TODO physical blows return damage type of "player", bows "arrow", and fire doesn't register player as the source of damage
			System.out.println(player.username + " killed a " + event.entityLiving.getEntityName() + " with damage type " + event.source.damageType);
			if (getXpType(event.source) != NO_TYPE)
				SkillInfo.get(player).addXp(XP_HIGH, (byte) getXpType(event.source));
		}
		
		if (!event.entity.worldObj.isRemote && event.entity instanceof EntityPlayer) {
			SkillInfo.saveProxyData((EntityPlayer) event.entity);
		}
	}
	
	@ForgeSubscribe
	public void onJump(LivingJumpEvent event)
	{
		if (event.entity instanceof EntityPlayer) {
			SkillInfo.get((EntityPlayer) event.entity).addXp(XP_LOW, AttributeCode.AGI);
		}
	}
	
	@ForgeSubscribe
	public void onBreakBlock(HarvestDropsEvent event)
	{
		if (event.harvester != null) {
			float hardness = event.block.blockHardness;
			if (hardness > 0) { SkillInfo.get(event.harvester).addXp(XP_LOW * hardness * XP_MINING, AttributeCode.STR); }
		}
	}
	
	@ForgeSubscribe
	public void onLivingUpdate(LivingUpdateEvent event)
	{
		if (event.entity instanceof EntityPlayer)
		{
			EntityPlayer player = (EntityPlayer) event.entity;
			SkillInfo.get(player).onUpdate();
			
			if (player.isSprinting() || (player.isSneaking() && isPlayerMoving(player))) {
				// AttributeModifier sprintingSpeedBoostModifier = (new AttributeModifier(sprintingSpeedBoostModifierUUID, "Sprinting speed boost", 0.30000001192092896D, 2)).setSaved(false);
				/*
				AttributeInstance attributeinstance = player.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
				if (attributeinstance.getModifier(sprintingSpeedBoostModifierUUID) != null)
		        {
		            attributeinstance.removeModifier(sprintingSpeedBoostModifier);
		        }

		        if (par1)
		        {
		            attributeinstance.applyModifier(sprintingSpeedBoostModifier);
		        }
		        */
				//player.addChatMessage("Side == client? " + player.worldObj.isRemote);
				SkillInfo.get(player).addXp(XP_LOW * XP_TICK, AttributeCode.AGI);
			}
			
			if (player.isInWater() && isPlayerMoving(player)) {
				SkillInfo.get(player).addXp(XP_LOW * XP_TICK, AttributeCode.STR);
			}
			
			if (player.isRiding() && isPlayerMoving(player)) {
				SkillInfo.get(player).addXp(XP_LOW * XP_TICK, AttributeCode.CHA);
			}
		}
	}
	
	/**
	 * Returns true if player is moving (excludes Y-axis to avoid always returning true)
	 * NOTE: only returns true on the client side, but synced with packets now so it's fine
	 */
	public static boolean isPlayerMoving(EntityPlayer player) {
		return player.motionX != 0 || player.motionZ != 0;
	}
	
	@ForgeSubscribe
	public void onEntityConstructing(EntityConstructing event)
	{
		if (event.entity instanceof EntityPlayer)
		{
			if (SkillInfo.get((EntityPlayer) event.entity) == null)
				SkillInfo.register((EntityPlayer) event.entity);
		}
	}

	@ForgeSubscribe
	public void onEntityJoinWorld(EntityJoinWorldEvent event)
	{
		if (!event.entity.worldObj.isRemote && event.entity instanceof EntityPlayer) {
			SkillInfo.loadProxyData((EntityPlayer) event.entity);
		}
	}
}