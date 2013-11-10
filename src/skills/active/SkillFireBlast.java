package coolalias.skillsmod.skills.active;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import coolalias.skillsmod.entity.skill.EntityFireBlast;
import coolalias.skillsmod.skills.SkillActive;

/**
 * 
 * @author coolAlias
 *
 * TODO make constructors at least protected; requires moving SkillActive class to same package
 * and instantiating new types of active skills there, then referencing them from SkillBase 
 */
public class SkillFireBlast extends SkillActive
{
	public SkillFireBlast(String name, byte id, AttributeCode attribute, byte tier, int cooldown) {
		super(name, id, attribute, tier, cooldown);
	}
	
	public SkillFireBlast(String name, byte id, AttributeCode attribute, byte tier, int cooldown, int duration) {
		super(name, id, attribute, tier, cooldown, duration);
	}
	
	private SkillFireBlast(SkillActive skill) { super(skill); }
	
	@Override
	public SkillFireBlast newInstance() { return new SkillFireBlast(this); }
	
	@Override
	public ResourceLocation getIconTexture() { return null; }
	
	@Override
	public boolean activate(World world, EntityPlayer player)
	{
		if (!world.isRemote) {
			EntityFireBlast fireball = new EntityFireBlast(world, player).setLevel(level);
            world.spawnEntityInWorld(fireball);
		}
		
		return super.activate(world, player);
	}
}
