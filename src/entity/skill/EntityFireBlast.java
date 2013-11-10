package coolalias.skillsmod.entity.skill;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * 
 * @author coolAlias
 *
 * Extending EntityFireball results in some derpy collision-detection and
 * other oddities. Perhaps try EntityThrowable instead
 * 
 */
public class EntityFireBlast extends EntityFireball
{
	private static final int BURN_TIME = 4;
	private float damage = 5.0F;
	private static final float SPEED = 1.5F;
	
	public EntityFireBlast(World world) {
		super(world);
	}
	
	/**
	 * Constructs EntityFireBlast with position and motion set based off of shooting entity
	 */
	public EntityFireBlast(World world, EntityLivingBase entity) {
		super(world);
		this.shootingEntity = entity;
		Vec3 vec3 = entity.getLookVec().normalize();
		this.posX = entity.posX + vec3.xCoord * 4.0D;
		// TODO refine y position to better match cross-hair location
		this.posY = entity.posY + vec3.yCoord + entity.getEyeHeight() / 2.0D;
		this.posZ = entity.posZ + vec3.zCoord * 4.0D;
		setMotion(entity.getLookVec());
	}

	public EntityFireBlast(World world, double posX, double posY, double posZ, double motX, double motY, double motZ) {
		super(world, posX, posY, posZ, motX, motY, motZ);
	}

	/**
	 * This constructor sets the fireball's target position in addition to it's initial position
	 * and should therefore only be used if a target is already acquired
	 * (Ghast's use it to shoot at the player, for example)
	 */
	public EntityFireBlast(World world, EntityLivingBase entity, double par3, double par5, double par7) {
		super(world, entity, par3, par5, par7);
	}
	
	/**
	 * Sets shooting entity; returns itself for convenience
	 */
	public EntityFireBlast setShootingEntity(EntityLivingBase entity) {
		this.shootingEntity = entity;
		return this;
	}
	
	/**
	 * Adds 1 point of damage per level; returns itself for convenience
	 */
	public EntityFireBlast setLevel(int level) {
		this.damage += (float) level;
		return this;
	}
	
	/**
	 * Sets entity in motion based on vector; uses pre-defined speed value
	 */
	public void setMotion(Vec3 vec3) {
		vec3.normalize();
		this.motionX = vec3.xCoord * SPEED;
		this.motionY = vec3.yCoord * SPEED;
		this.motionZ = vec3.zCoord * SPEED;
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) { return false; }

	@Override
	protected void onImpact(MovingObjectPosition movingobjectposition)
	{
		if (!this.worldObj.isRemote)
        {
            if (movingobjectposition.entityHit != null)
            {
                if (!movingobjectposition.entityHit.isImmuneToFire() && movingobjectposition.entityHit.attackEntityFrom(DamageSource.causeFireballDamage(this, this.shootingEntity), this.damage))
                {
                    movingobjectposition.entityHit.setFire(BURN_TIME);
                }
            }
            else
            {
                int i = movingobjectposition.blockX;
                int j = movingobjectposition.blockY;
                int k = movingobjectposition.blockZ;

                switch (movingobjectposition.sideHit)
                {
                    case 0:
                        --j;
                        break;
                    case 1:
                        ++j;
                        break;
                    case 2:
                        --k;
                        break;
                    case 3:
                        ++k;
                        break;
                    case 4:
                        --i;
                        break;
                    case 5:
                        ++i;
                }

                if (this.worldObj.isAirBlock(i, j, k))
                {
                    this.worldObj.setBlock(i, j, k, Block.fire.blockID);
                }
            }

            this.setDead();
        }
	}
}
