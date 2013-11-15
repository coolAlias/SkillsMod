package coolalias.skillsmod.skills.passive;

import java.util.UUID;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeInstance;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import coolalias.skillsmod.skills.SkillPassive;

/**
 * 
 * @author coolAlias
 *
 */
public class SkillIronFlesh extends SkillPassive
{
	// TODO assign a non-random UUID, though it seems to work with saving/loading as is
	private static final UUID ironFleshUUID = UUID.randomUUID();
	/** Base health modifier */
	private static final AttributeModifier ironFleshModifier = (new AttributeModifier(ironFleshUUID, "Iron Flesh", 1.0D, 2)).setSaved(true);
	
	public SkillIronFlesh(String name, byte id, AttributeCode attribute, byte tier) {
		super(name, id, attribute, tier);
	}

	public SkillIronFlesh(String name, byte id, AttributeCode attribute, byte tier, byte maxLevel, boolean register) {
		super(name, id, attribute, tier, maxLevel, register);
	}
	
	private SkillIronFlesh(SkillIronFlesh skill) { super(skill); }
	
	@Override
	public SkillIronFlesh newInstance() { return new SkillIronFlesh(this); }
	
	@Override
	protected void levelUp(EntityPlayer player, int targetLevel)
	{
		while (level < targetLevel && canIncreaseLevel(player, level + 1)) { ++level; }
		AttributeInstance attributeinstance = player.getEntityAttribute(SharedMonsterAttributes.maxHealth);
		if (attributeinstance.getModifier(ironFleshUUID) != null) { attributeinstance.removeModifier(ironFleshModifier); }
		AttributeModifier newModifier = (new AttributeModifier(ironFleshUUID, "Iron Flesh", level * 2.0D, 0)).setSaved(true);
		attributeinstance.applyModifier(newModifier);
	}
}
