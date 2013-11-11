package coolalias.skillsmod.skills;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import coolalias.skillsmod.SkillInfo;

/**
 * 
 * @author coolAlias
 * 
 * Made abstract to guarantee following subclass methods are called:
 * Point 1: Override canLearn to add further complexity and control to the leveling up process
 * Point 2: Override levelUp method to add modifiers to player's vanilla attribute map
 * 
 * Note that any further fields will not be saved / loaded, so must be immutable or calculated
 * at run-time based on available data (e.g. level)
 * 
 * TODO consider adding AttributeModifier as class field, then create SkillGenericPassive that
 * applies this attribute modifier each level (like Iron Flesh skill already does). Will also
 * need to store AttributeInstance to apply it to, perhaps as SharedMonsterAttribute field
 *
 */
public abstract class SkillPassive extends SkillBase
{
	protected SkillPassive(String name, byte id, AttributeCode attribute, byte tier) {
		super(name, id, attribute, tier);
	}

	protected SkillPassive(String name, byte id, AttributeCode attribute, byte tier, byte maxLevel, boolean register) {
		super(name, id, attribute, tier, maxLevel, register);
	}

	protected SkillPassive(SkillBase skill) {
		this(skill.name, skill.id, skill.attribute, skill.tier, skill.maxLevel, false);
	}
	
	@Override
	public final boolean canIncreaseLevel(EntityPlayer player, int targetLevel) {
		SkillBase base = SkillInfo.get(player).getBaseSkills().get(attribute.ordinal());
		return base != null ? base.level >= (targetLevel - 1) * 5 : false;
	}
	
	@Override
	public final SkillPassive loadFromNBT(NBTTagCompound compound) {
		SkillPassive skill = (SkillPassive) skillsList[compound.getByte("id")].newInstance();
		skill.readFromNBT(compound);
		return skill;
	}
	
	@Override
	public final SkillPassive loadFromStream(byte id, DataInputStream inputStream) throws IOException {
		SkillPassive skill = (SkillPassive) skillsList[id].newInstance();
		skill.level = inputStream.readByte();
		return skill;
	}
}
