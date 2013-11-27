package coolalias.skillsmod.entity

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentThorns;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet70GameEvent;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 
 * @author coolAlias
 * 
 * Base custom Arrow class, operates exactly as EntityArrow except it provides an easy framework
 * from which to extend and manipulate, specifically by breaking up the onUpdate method into
 * multiple overridable steps
 *
 */
public class EntityCustomArrow extends EntityArrow implements IProjectile
{
	/** Private fields from EntityArrow are now protected instead */
	protected int xTile = -1, yTile = -1, zTile = -1, inTile, inData;
	protected int ticksInGround, ticksInAir;
	protected boolean inGround;

	/** damage and knockback have getters and setters, so can be private */
	private double damage = 2.0D;
	private int knockbackStrength;

	/** Basic constructor is necessary */
	public EntityCustomArrow(World world) { super(world); }

	/** Constructs an arrow at a position, but with no heading or velocity */
	public EntityCustomArrow(World world, double x, double y, double z) { super(world, x, y, z); }

	/** Constructs an arrow with heading based on shooter and given velocity */
	public EntityCustomArrow(World world, EntityLivingBase shooter, float velocity) { super(world, shooter, velocity); }

	/**
	 * Constructs an arrow heading towards target's initial position with given velocity, but abnormal Y trajectory;
	 * Not recommended to use for players unless the constructor is overridden to be more player friendly
	 * @param wobble amount of deviation from base trajectory, used by Skeletons and the like; set to 1.0F for no x/z deviation
	 */
	public EntityCustomArrow(World world, EntityLivingBase shooter, EntityLivingBase target, float velocity, float wobble)
	{
		super(world);
		this.shootingEntity = shooter;

		if (shooter instanceof EntityPlayer) { this.canBePickedUp = 1; }

		this.posY = shooter.posY + (double) shooter.getEyeHeight() - 0.10000000149011612D;
		double d0 = target.posX - shooter.posX;
		double d1 = target.boundingBox.minY + (double)(target.height / 3.0F) - this.posY;
		double d2 = target.posZ - shooter.posZ;
		double d3 = (double) MathHelper.sqrt_double(d0 * d0 + d2 * d2);

		if (d3 >= 1.0E-7D)
		{
			float f2 = (float)(Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
			float f3 = (float)(-(Math.atan2(d1, d3) * 180.0D / Math.PI));
			double d4 = d0 / d3;
			double d5 = d2 / d3;
			this.setLocationAndAngles(shooter.posX + d4, this.posY, shooter.posZ + d5, f2, f3);
			this.yOffset = 0.0F;
			float f4 = (float) d3 * 0.2F;
			this.setThrowableHeading(d0, d1 + (double) f4, d2, velocity, wobble);
		}
	}

	@Override
	public void onUpdate()
	{
		// This calls the Entity class' update method directly, circumventing EntityArrow
		super.onEntityUpdate();
		this.updateAngles();
		this.checkInGround();
		if (this.arrowShake > 0) { --this.arrowShake; }
		if (this.inGround) { this.updateInGround(); }
		else { this.updateInAir(); }
	}

	/**
	 * @param wobble value of 1.0F sets true heading; other values will cause trajectory to vary from expected (Skeletons use this to simulate 'missing')
	 */
	@Override
	public void setThrowableHeading(double vecX, double vecY, double vecZ, float velocity, float wobble)
	{
		float f2 = MathHelper.sqrt_double(vecX * vecX + vecY * vecY + vecZ * vecZ);
		vecX /= (double) f2;
		vecY /= (double) f2;
		vecZ /= (double) f2;
		vecX += this.rand.nextGaussian() * (double)(this.rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * (double) wobble;
		vecY += this.rand.nextGaussian() * (double)(this.rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * (double) wobble;
		vecZ += this.rand.nextGaussian() * (double)(this.rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * (double) wobble;
		vecX *= (double) velocity;
		vecY *= (double) velocity;
		vecZ *= (double) velocity;
		this.motionX = vecX;
		this.motionY = vecY;
		this.motionZ = vecZ;
		float f3 = MathHelper.sqrt_double(vecX * vecX + vecZ * vecZ);
		this.prevRotationYaw = this.rotationYaw = (float)(Math.atan2(vecX, vecZ) * 180.0D / Math.PI);
		this.prevRotationPitch = this.rotationPitch = (float)(Math.atan2(vecY, (double) f3) * 180.0D / Math.PI);
		this.ticksInGround = 0;
	}

	/**
	 * Sets the position and rotation. Only difference from the other one is no bounding on the rotation. Args: posX,
	 * posY, posZ, yaw, pitch
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int par9) {
		this.setPosition(x, y, z);
		this.setRotation(yaw, pitch);
	}

	/**
	 * Sets the velocity to the args. Args: x, y, z
	 */
	@Override
	@SideOnly(Side.CLIENT)    
	public void setVelocity(double x, double y, double z)
	{
		this.motionX = x;
		this.motionY = y;
		this.motionZ = z;

		if (this.prevRotationPitch == 0.0F && this.prevRotationYaw == 0.0F)
		{
			float f = MathHelper.sqrt_double(x * x + z * z);
			this.prevRotationYaw = this.rotationYaw = (float)(Math.atan2(x, z) * 180.0D / Math.PI);
			this.prevRotationPitch = this.rotationPitch = (float)(Math.atan2(y, (double)f) * 180.0D / Math.PI);
			this.prevRotationPitch = this.rotationPitch;
			this.prevRotationYaw = this.rotationYaw;
			this.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
			this.ticksInGround = 0;
		}
	}

	/**
	 * Called by a player entity when they collide with an entity
	 */
	@Override
	public void onCollideWithPlayer(EntityPlayer player)
	{
		if (!this.worldObj.isRemote && this.inGround && this.arrowShake <= 0)
		{
			boolean flag = this.canBePickedUp == 1 || this.canBePickedUp == 2 && player.capabilities.isCreativeMode;

			if (this.canBePickedUp == 1 && !player.inventory.addItemStackToInventory(new ItemStack(Item.arrow, 1)))
			{
				flag = false;
			}

			if (flag)
			{
				this.playSound("random.pop", 0.2F, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
				player.onItemPickup(this, 1);
				this.setDead();
			}
		}
	}

	@Override
	protected boolean canTriggerWalking() { return false; }

	@Override
	@SideOnly(Side.CLIENT)
	public float getShadowSize() { return 0.0F; }

	/** Sets the amount of damage the arrow will inflict when it hits a mob */
	public void setDamage(double vecX) { this.damage = vecX; }

	/** Returns the amount of damage the arrow will inflict when it hits a mob */
	public double getDamage() { return this.damage; }

	/** Sets the amount of knockback the arrow applies when it hits a mob. */
	public void setKnockbackStrength(int vecX) { this.knockbackStrength = vecX; }

	/** Returns the amount of knockback the arrow applies when it hits a mob */
	public int getKnockbackStrength() { return this.knockbackStrength; }

	@Override
	public boolean canAttackWithItem() { return false; }

	/**
	 * Whether the arrow has a stream of critical hit particles flying behind it.
	 */
	@Override
	public void setIsCritical(boolean isCrit) {
		byte b0 = this.dataWatcher.getWatchableObjectByte(16);
		if (isCrit) { this.dataWatcher.updateObject(16, Byte.valueOf((byte)(b0 | 1))); }
		else { this.dataWatcher.updateObject(16, Byte.valueOf((byte)(b0 & -2))); }
	}

	/**
	 * Whether the arrow has a stream of critical hit particles flying behind it.
	 */
	public boolean getIsCritical() {
		byte b0 = this.dataWatcher.getWatchableObjectByte(16);
		return (b0 & 1) != 0;
	}

	/**
	 * Updates yaw and pitch based on current motion
	 */
	protected void updateAngles()
	{
		if (this.prevRotationPitch == 0.0F && this.prevRotationYaw == 0.0F) {
			float f = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
			this.prevRotationYaw = this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);
			this.prevRotationPitch = this.rotationPitch = (float)(Math.atan2(this.motionY, (double) f) * 180.0D / Math.PI);
		}
	}

	/**
	 * Updates the arrow's position and angles
	 */
	protected void updatePosition()
	{
		this.posX += this.motionX;
		this.posY += this.motionY;
		this.posZ += this.motionZ;
		float f2 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
		this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);

		for (this.rotationPitch = (float)(Math.atan2(this.motionY, (double) f2) * 180.0D / Math.PI); this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F)
		{ ; }

		while (this.rotationPitch - this.prevRotationPitch >= 180.0F)
		{ this.prevRotationPitch += 360.0F; }

		while (this.rotationYaw - this.prevRotationYaw < -180.0F)
		{ this.prevRotationYaw -= 360.0F; }

		while (this.rotationYaw - this.prevRotationYaw >= 180.0F)
		{ this.prevRotationYaw += 360.0F; }

		this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * 0.2F;
		this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * 0.2F;
		float f4 = 0.99F;
		float f1 = 0.05F;

		if (this.isInWater())
		{
			for (int i = 0; i < 4; ++i) {
				float f3 = 0.25F;
				this.worldObj.spawnParticle("bubble", this.posX - this.motionX * (double) f3, this.posY - this.motionY * (double) f3, this.posZ - this.motionZ * (double) f3, this.motionX, this.motionY, this.motionZ);
			}

			f4 = 0.8F;
		}

		this.updateMotion(f4, f1);
		this.setPosition(this.posX, this.posY, this.posZ);
	}

	/**
	 * Adjusts arrow's motion: multiplies each by factor, subtracts adjustY from motionY
	 */
	protected void updateMotion(float factor, float adjustY) {
		this.motionX *= (double) factor;
		this.motionY *= (double) factor;
		this.motionZ *= (double) factor;
		this.motionY -= (double) adjustY;
	}

	/**
	 * Checks if entity is colliding with a block and if so, sets inGround to true
	 */
	protected void checkInGround()
	{
		int i = this.worldObj.getBlockId(this.xTile, this.yTile, this.zTile);

		if (i > 0)
		{
			Block.blocksList[i].setBlockBoundsBasedOnState(this.worldObj, this.xTile, this.yTile, this.zTile);
			AxisAlignedBB axisalignedbb = Block.blocksList[i].getCollisionBoundingBoxFromPool(this.worldObj, this.xTile, this.yTile, this.zTile);

			if (axisalignedbb != null && axisalignedbb.isVecInside(this.worldObj.getWorldVec3Pool().getVecFromPool(this.posX, this.posY, this.posZ)))
			{
				this.inGround = true;
			}
		}
	}

	/**
	 * If entity is in ground, updates ticks in ground or adjusts position if block no longer in world
	 */
	protected void updateInGround()
	{
		int j = this.worldObj.getBlockId(this.xTile, this.yTile, this.zTile);
		int k = this.worldObj.getBlockMetadata(this.xTile, this.yTile, this.zTile);

		if (j == this.inTile && k == this.inData)
		{
			++this.ticksInGround;
			if (this.ticksInGround == 1200) { this.setDead(); }
		}
		else
		{
			this.inGround = false;
			this.motionX *= (double)(this.rand.nextFloat() * 0.2F);
			this.motionY *= (double)(this.rand.nextFloat() * 0.2F);
			this.motionZ *= (double)(this.rand.nextFloat() * 0.2F);
			this.ticksInGround = 0;
			this.ticksInAir = 0;
		}
	}

	/**
	 * Checks for impacts, spawns trailing particles and updates entity position
	 */
	protected void updateInAir()
	{
		++this.ticksInAir;
		MovingObjectPosition mop = checkForImpact();
		if (mop != null) { this.onImpact(mop); }
		this.spawnTrailingParticles();
		this.updatePosition();
		this.doBlockCollisions();
	}

	/**
	 * Spawns trailing particles, if any; vanilla behavior requires critical hit to be true
	 * NOTE: Register this class as well as all child classes to the EntityRegistry, with tracking,
	 * or the particles will not spawn due to the client not having the correct information
	 */
	protected void spawnTrailingParticles()
	{
		if (this.getIsCritical()) {
			for (int i = 0; i < 4; ++i) {
				this.worldObj.spawnParticle("crit",
						this.posX + this.motionX * (double) i / 4.0D,
						this.posY + this.motionY * (double) i / 4.0D,
						this.posZ + this.motionZ * (double) i / 4.0D,
						-this.motionX, -this.motionY + 0.2D, -this.motionZ);
			}
		}
	}

	/**
	 * Returns MovingObjectPosition of Entity or Block impacted, or null if nothing was struck
	 */
	protected MovingObjectPosition checkForImpact()
	{
		Vec3 vec3 = this.worldObj.getWorldVec3Pool().getVecFromPool(this.posX, this.posY, this.posZ);
		Vec3 vec31 = this.worldObj.getWorldVec3Pool().getVecFromPool(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
		MovingObjectPosition mop = this.worldObj.rayTraceBlocks_do_do(vec3, vec31, false, true);
		vec3 = this.worldObj.getWorldVec3Pool().getVecFromPool(this.posX, this.posY, this.posZ);
		vec31 = this.worldObj.getWorldVec3Pool().getVecFromPool(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

		if (mop != null) {
			vec31 = this.worldObj.getWorldVec3Pool().getVecFromPool(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
		}

		Entity entity = null;
		List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ).expand(1.0D, 1.0D, 1.0D));
		double d0 = 0.0D;
		double hitBox = 0.3D;

		for (int i = 0; i < list.size(); ++i)
		{
			Entity entity1 = (Entity) list.get(i);

			if (entity1.canBeCollidedWith() && (entity1 != this.shootingEntity || this.ticksInAir >= 5))
			{
				AxisAlignedBB axisalignedbb = entity1.boundingBox.expand(hitBox, hitBox, hitBox);
				MovingObjectPosition mop1 = axisalignedbb.calculateIntercept(vec3, vec31);

				if (mop1 != null)
				{
					double d1 = vec3.distanceTo(mop1.hitVec);

					if (d1 < d0 || d0 == 0.0D) {
						entity = entity1;
						d0 = d1;
					}
				}
			}
		}

		if (entity != null) { mop = new MovingObjectPosition(entity); }

		if (mop != null && mop.entityHit instanceof EntityPlayer)
		{
			EntityPlayer entityplayer = (EntityPlayer) mop.entityHit;
			if (entityplayer.capabilities.disableDamage || this.shootingEntity instanceof EntityPlayer
					&& !((EntityPlayer) this.shootingEntity).canAttackPlayer(entityplayer))
			{
				mop = null;
			}
		}

		return mop;
	}

	/**
	 * Called when custom arrow impacts an entity or block
	 */
	protected void onImpact(MovingObjectPosition mop) {
		if (mop.entityHit != null) { onImpactEntity(mop); }
		else { onImpactBlock(mop); }
	}

	/**
	 * Called when custom arrow impacts another entity
	 */
	protected void onImpactEntity(MovingObjectPosition mop)
	{
		if (mop.entityHit != null)
		{
			DamageSource damagesource = new EntityDamageSourceIndirect("arrow", this, this.shootingEntity).setProjectile();
			int dmg = calculateDamage();

			if (this.isBurning() && !(mop.entityHit instanceof EntityEnderman)) {
				mop.entityHit.setFire(5);
			}

			if (mop.entityHit.attackEntityFrom(damagesource, (float) dmg))
			{
				if (mop.entityHit instanceof EntityLivingBase)
				{
					handlePostDamageEffects((EntityLivingBase) mop.entityHit);

					if (this.shootingEntity instanceof EntityPlayerMP && mop.entityHit != this.shootingEntity && mop.entityHit instanceof EntityPlayer) {
						((EntityPlayerMP) this.shootingEntity).playerNetServerHandler.sendPacketToPlayer(new Packet70GameEvent(6, 0));
					}
				}

				this.playSound("random.bowhit", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));

				if (!(mop.entityHit instanceof EntityEnderman)) { this.setDead(); }
			}
			else
			{
				this.motionX *= -0.10000000149011612D;
				this.motionY *= -0.10000000149011612D;
				this.motionZ *= -0.10000000149011612D;
				this.rotationYaw += 180.0F;
				this.prevRotationYaw += 180.0F;
				this.ticksInAir = 0;
			}
		}
	}

	/**
	 * Called when custom arrow impacts a block
	 */
	protected void onImpactBlock(MovingObjectPosition mop)
	{
		this.xTile = mop.blockX;
		this.yTile = mop.blockY;
		this.zTile = mop.blockZ;
		this.inTile = this.worldObj.getBlockId(this.xTile, this.yTile, this.zTile);
		this.inData = this.worldObj.getBlockMetadata(this.xTile, this.yTile, this.zTile);
		this.motionX = (double)((float)(mop.hitVec.xCoord - this.posX));
		this.motionY = (double)((float)(mop.hitVec.yCoord - this.posY));
		this.motionZ = (double)((float)(mop.hitVec.zCoord - this.posZ));
		float f2 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
		this.posX -= this.motionX / (double) f2 * 0.05000000074505806D;
		this.posY -= this.motionY / (double) f2 * 0.05000000074505806D;
		this.posZ -= this.motionZ / (double) f2 * 0.05000000074505806D;
		this.playSound("random.bowhit", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));
		this.inGround = true;
		this.arrowShake = 7;
		this.setIsCritical(false);

		if (this.inTile != 0) {
			Block.blocksList[this.inTile].onEntityCollidedWithBlock(this.worldObj, this.xTile, this.yTile, this.zTile, this);
		}
	}

	/**
	 * Returns amount of damage arrow will inflict to entity impacted
	 */
	protected int calculateDamage()
	{
		float f2 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
		int dmg = MathHelper.ceiling_double_int((double) f2 * this.damage);

		if (this.getIsCritical()) { dmg += this.rand.nextInt(dmg / 2 + 2); }

		return dmg;
	}

	/**
	 * Handles all secondary effects if entity hit was damaged, such as knockback, thorns, etc.
	 */
	protected void handlePostDamageEffects(EntityLivingBase entityHit)
	{
		if (!this.worldObj.isRemote) {
			entityHit.setArrowCountInEntity(entityHit.getArrowCountInEntity() + 1);
		}

		if (this.knockbackStrength > 0)
		{
			float f3 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);

			if (f3 > 0.0F) {
				double knockback = (double) this.knockbackStrength * 0.6000000238418579D / (double) f3;
				entityHit.addVelocity(this.motionX * knockback, 0.1D, this.motionZ * knockback);
			}
		}

		if (this.shootingEntity != null) {
			EnchantmentThorns.func_92096_a(this.shootingEntity, entityHit, this.rand);
		}
	}

	@Override
	protected void entityInit() { this.dataWatcher.addObject(16, Byte.valueOf((byte) 0)); }

	@Override
	public void writeEntityToNBT(NBTTagCompound compound)
	{
		compound.setShort("xTile", (short)this.xTile);
		compound.setShort("yTile", (short)this.yTile);
		compound.setShort("zTile", (short)this.zTile);
		compound.setByte("inTile", (byte)this.inTile);
		compound.setByte("inData", (byte)this.inData);
		compound.setByte("shake", (byte)this.arrowShake);
		compound.setByte("inGround", (byte)(this.inGround ? 1 : 0));
		compound.setByte("pickup", (byte)this.canBePickedUp);
		compound.setDouble("damage", this.damage);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound)
	{
		this.xTile = compound.getShort("xTile");
		this.yTile = compound.getShort("yTile");
		this.zTile = compound.getShort("zTile");
		this.inTile = compound.getByte("inTile") & 255;
		this.inData = compound.getByte("inData") & 255;
		this.arrowShake = compound.getByte("shake") & 255;
		this.inGround = compound.getByte("inGround") == 1;
		if (compound.hasKey("damage")) { this.damage = compound.getDouble("damage"); }
		if (compound.hasKey("pickup")) { this.canBePickedUp = compound.getByte("pickup"); }
		else if (compound.hasKey("player")) { this.canBePickedUp = compound.getBoolean("player") ? 1 : 0; }
	}
}
