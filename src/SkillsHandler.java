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
	/*
	Strength
	DONE, scales with block hardness 1. Mining  - very low
	2. Building - very low
		TODO something similar to onBlockPlacedBy?
	DONE, scales with damage 3. Blocking (while being attacked) - low
	DONE, scales with damage 4. Taking damage - low
	DONE, scales with damage 5. Dealing damage - low
	PART, implemented but no scaling yet 6. Killing mobs (W/o a bow) - medium (respective to the strength of the mob, higher lvl mob, higher XP)
	// custom:
	 * 7. Farming (using a hoe)?
	
	Agility 
	DONE, applied per tick 1. Sprinting - low
	DONE, applied per tick 2. Swimming  - low
	DONE 3. Jumping - low
	4. Flying - low
		TODO easy to implement if we decide we want it
	DONE for any item that uses ArrowLooseEvent 5. Shooting a bow/projectile - medium (changed to low)
	PART, implemented but no scaling yet 6. Killing mobs with a bow/projectile - medium (respective to the strength of the mob, higher lvl mob, higher XP)
	// custom:
	DONE, scales with damage 7. Dealing or receiving projectile damage of any type
	DONE, scales with damage 8. Taking fall damage
	DONE, applied per tick 9. Sneaking - low
	
	Intelligence
	1. Enchanting - medium (higher enchant, higher XP gain)
		TODO how to detect this? intercept Packet108EnchantItem?
	2. Brewing - medium
		TODO how to detect this? PotionBrewedEvent has no Entity.
	3. Writing in a book - low
		TODO how to detect this? intercept the "MC|BSign" packet sent to the server from GuiScreenBook on signing? is that possible?
	4. Talking to NPCs - low
		TODO EntityInteractEvent if EntityVillager and canInteract
	// custom:
	DONE, scales with damage 5. Dealing or receiving magic damage of any type, such as with Fire Blast skill
	
	Charisma 
	1. Talking to NPCs - low
		TODO EntityInteractEvent if EntityVillager and canInteract
	2. Trading with NPCs - high
		TODO how to detect a trade? EntityInteractEvent with Villager?
		* could implement a custom Event for custom NPCs
		* similar to Writing a book, intercept the "MC|TrSel" packet sent from GuiMerchant when a transaction occurs
	3. Buying - Low-High (depends on the value of the object)
		TODO see #2
	4. Selling - Low-High (depends on the value of the object)
		TODO see #2
	DONE, applied per tick 5. Riding a horse - low
	 */
	/*
	Strength:
		Iron-flesh: increases max health by another heart.
			TODO look at extra health potions
		Power Strike : increases melee damage by 3%
			TODO AttackEntityEvent, check skill level and increase damage
		Mighty Throw: allows you to throw more items with force. Increases thrown projectiles damage by 10% (excludes arrows)
			TODO AttackEntityEvent? what do thrown item's return as damage source type?
		Counterattack: If attacked the player can quickly attack dealing extra damage.
			TODO AttackEntityEvent, if hurt resistant time > 0 && skill.getLevel() > 0, add extra damage
		Blocking: increases blocking resistance by 5%
			TODO LivingHurtEvent reduce damage further if blocking
		Berserk: chance of hitting another enemy close by the target.
			TODO AttackEntityEvent get entities in expanded bounding box
		
	Agility:  (requires 1.6.1 on some of them)
		Sprinting: Increases your speed by 3%
			TODO LivingUpdateEvent if player.isSprinting() multiply movement vector by amount
		Riding: Increases horse speed, jump distance and jump height. Increases acceleration and deceleration.
			TODO look into horses
		Horse Archery: reduces accuracy penalty while riding horses
			TODO there's an accuracy penalty?
		Looting: increases loot amount by x%. Multiplies with the enchantment.
			TODO LivingDropsEvent may have player reference in DamageSource
		Controlled Draw : increases bow damage by 5%
			TODO EntityArrow not yet spawned in ArrowLooseEvent, maybe use LivingHurtEvent to increase damage instead?
		Precision : allows arrows to slightly 'home' towards an enemy so long it is within a certain distance from the crosshair.
			TODO hmmm... not sure
		
	Intelligence:
		Tracking : allows you to track mobs
			TODO what does this mean?
		Spotting: allows you to easily see mobs, even from far away or behind walls.
			TODO probably client side only; how?
		Surgery: When your pet dies, it has 5% per point to instead teleport around 500 meters from its  death point, sitting. Retains attributes and ownership, but with one health.
			TODO LivingDeathEvent check if Ownable and hasOwner, heal, cancel, teleport and set sitting
		First Aid: When you die, you will instead heal yourself by 1 per skill point. 20 minute cooldown.
			TODO LivingDeathEvent, implement like my resurrection spell
		Engineer: extends your building reach.
			TODO PlayerInteractEvent, team probably has experience with extending reach
		Learner: increases the rate you gain proficiencies.
			TODO easy; multiply xp gained in addXP method of SkillInfo (i.e. SectionSkills)
		
	Charisma:
		Leadership: Allows more pets to follow you. If the amount of pets standing goes higher than the limit, some of your pets will sit immediately.
			TODO will require getting all entities around player every few ticks, counting if the entities are owned by the player
			and not already sitting; once the limit is reached, any further owned entities will be told to sit
		Trade: Decreases the amount of items you need to give when trading by 5%.
			TODO not sure about this one
		Weapon Master: Combos in the 'Art of the Sword' and 'Way of the Ninja' skill sets deal more damage, and give more fame
			TODO implement the active skills first, then worry about this
		Persuasion: Allows villagers to think rarer trades as more common.
			TODO how do we manipulate merchant lists on the fly?
		Tamer:  Increases animal tame chance by 10% 
			TODO EntityInteractEvent? might have to calculate separately >.<
		Recovery: Changes the rate you regenerate by 3% of base regen.
			TODO Look into health regeneration code
	 */
	
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
	
	@ForgeSubscribe
	public void onArrowLooseEvent(ArrowLooseEvent event) {
		SkillInfo.get(event.entityPlayer).addXP(XP_LOW, AttributeCode.AGI);
	}
	
	/**
	 * This event is called when an entity is attacked by another entity
	 */
	@ForgeSubscribe
	public void onAttacked(LivingAttackEvent event)
	{
		if (event.entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.entity;
			if (player.isBlocking()) { SkillInfo.get(player).addXP(XP_LOW * event.ammount * XP_DMG_TAKE, AttributeCode.STR); }
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
					SkillInfo.get((EntityPlayer) event.entity).addXP(XP_LOW * event.ammount * XP_DMG_TAKE, (byte) xpType);
				}
				// Entity damaged by a player
				if (event.source.getEntity() instanceof EntityPlayer) {
					System.out.println("getEntity Player damaged entity for " + event.ammount + " damage.");
					SkillInfo.get((EntityPlayer) event.source.getEntity()).addXP(XP_LOW * event.ammount * XP_DMG_DEAL, (byte) xpType);
				}
				// TODO remove the following; only for debug as SourceOfDamage isn't needed
				if (event.source.getSourceOfDamage() instanceof EntityPlayer) {
					System.out.println("getSourceOfDamge Player damaged entity for " + event.ammount + " damage.");
					SkillInfo.get((EntityPlayer) event.source.getSourceOfDamage()).addXP(XP_LOW * event.ammount * XP_DMG_DEAL, (byte) xpType);
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
				SkillInfo.get(player).addXP(XP_HIGH, (byte) getXpType(event.source));
		}
		
		if (!event.entity.worldObj.isRemote && event.entity instanceof EntityPlayer) {
			SkillInfo.saveProxyData((EntityPlayer) event.entity);
		}
	}
	
	@ForgeSubscribe
	public void onJump(LivingJumpEvent event)
	{
		// TODO jumping is client side only, so need to add this xp to player's server data somehow
		if (event.entity instanceof EntityPlayer) {
			SkillInfo.get((EntityPlayer) event.entity).addXP(XP_LOW, AttributeCode.AGI);
		}
	}
	
	@ForgeSubscribe
	public void onBreakBlock(HarvestDropsEvent event)
	{
		if (event.harvester != null) {
			float hardness = event.block.blockHardness;
			if (hardness > 0) { SkillInfo.get(event.harvester).addXP(XP_LOW * hardness * XP_MINING, AttributeCode.STR); }
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
				SkillInfo.get(player).addXP(XP_LOW * XP_TICK, AttributeCode.AGI);
			}
			
			if (player.isInWater() && isPlayerMoving(player)) {
				//player.addChatMessage("Side == client? " + player.worldObj.isRemote);
				SkillInfo.get(player).addXP(XP_LOW * XP_TICK, AttributeCode.STR);
			}
			
			if (player.isRiding() && isPlayerMoving(player)) {
				SkillInfo.get(player).addXP(XP_LOW * XP_TICK, AttributeCode.CHA);
			}
		}
	}
	
	/**
	 * Returns true if player is moving (excludes Y-axis to avoid always returning true)
	 */
	public static boolean isPlayerMoving(EntityPlayer player) {
		// TODO figure out how to detect movement on the server side
		//player.addChatMessage("Side == client? " + player.worldObj.isRemote);
		//player.addChatMessage("Player motionX " + player.motionX + ", motionZ " + player.motionZ);
		//NOPE: return player.motionX != 0 || player.motionZ != 0;
		//NOPE: return player.moveForward != 0 || player.moveStrafing != 0;
		//NOPE: return player.posX != player.lastTickPosX || player.posZ != player.lastTickPosZ;
		//NOPE: player's server position is an integer, not precise enough to be useful
		return true;
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