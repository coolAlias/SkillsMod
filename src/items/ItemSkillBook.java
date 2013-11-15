package coolalias.skillsmod.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import coolalias.skillsmod.SkillInfo;
import coolalias.skillsmod.SkillsMod;
import coolalias.skillsmod.skills.SkillActive;
import coolalias.skillsmod.skills.SkillBase;
import coolalias.skillsmod.skills.SkillBase.AttributeCode;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 
 * @author coolAlias
 * 
 * Skill Books will grant an active skill to the player upon use provided the player
 * meets the skill prerequisites. It is consumed if the player's skill level was increased.
 * 
 * Use the static method 'getSkillBook(skill, level)' to generate the appropriate ItemStack
 * to give the player. Note that the method will return null if the passed in skill is not
 * a valid Active Skill, so take appropriate measures when using it.
 *
 */
public class ItemSkillBook extends ItemBook
{
	public ItemSkillBook(int par1) {
		super(par1);
		setHasSubtypes(true);
		setMaxDamage(0);
		// TODO use custom texture or just use vanilla book?
		setTextureName("book_normal");
		setCreativeTab(CreativeTabs.tabMisc);
	}
	
	/**
	 * Returns a new ItemStack skill book teaching given skill at given level or null if skill is not an active skill
	 */
	public static ItemStack getSkillBook(SkillBase skill, byte level)
	{
		if (skill instanceof SkillActive) {
			ItemStack book = new ItemStack(SkillsMod.skillBook,1,skill.id);
			setLevel(book, level);
			return book;
		}
		return null;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(int index, CreativeTabs tab, List list) {
		for (int i = 0; i < SkillBase.skillsList.length; ++i)
			if (getSkillFromDamage(i) != null) { list.add(new ItemStack(index, 1, i)); }
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return super.getUnlocalizedName().substring(5) + "." + getSkillFromStack(stack).id + "." + getSkillLevel(stack);
	}
	
	@Override
	public String getItemStackDisplayName(ItemStack stack)
	{
		SkillActive skill = getSkillFromStack(stack);
		String name = "Generic Skill Book";
		if (skill != null) { name = "Skill Book of " + skill.name + getLevelForDisplay(stack); }
		return name;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
		SkillActive skill = getSkillFromStack(stack);
		if (skill != null) {
			list.add("Use to learn or activate");
			list.addAll(skill.getDescription());
		}
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
	{
		if (!world.isRemote) { SkillInfo.get(player).addXp(100.0F, AttributeCode.AGI); }
		//SkillInfo.get(player).grantSkill(SkillBase.ironFlesh.id, (byte) (SkillInfo.get(player).getSkillLevel(SkillBase.ironFlesh) + 1));
		
		SkillActive skill = getSkillFromStack(stack);
		if (skill != null)
		{
			SkillInfo info = SkillInfo.get(player);
			if (info.grantSkill(skill.id, getSkillLevel(stack))) {
				if (!player.capabilities.isCreativeMode) { --stack.stackSize; }
			} else if (info.getSkillLevel(skill) >= getSkillLevel(stack)) {
				info.activateSkill(world, skill.id);
				//skill.activate(world, player);
			} else {
				player.addChatMessage("Failed to learn " + skill.name);
			}
		}
		
		return stack;
	}
	
	/**
	 * Sets the skill level this book will teach; will auto-cap at the appropriate level if necessary 
	 */
	public static void setLevel(ItemStack stack, byte level)
	{
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		SkillActive skill = getSkillFromStack(stack);
		byte maxLevel = skill != null ? skill.getMaxLevel() : 1;
		stack.getTagCompound().setByte("skillLevel", level < 1 ? 1 : level > maxLevel ? maxLevel : level);
	}
	
	/**
	 * Returns the active skill from id
	 */
	private static SkillActive getSkillFromDamage(int id)
	{
		if (id < SkillBase.skillsList.length && SkillBase.skillsList[id] != null) {
			if (SkillBase.skillsList[id] instanceof SkillActive) return (SkillActive) SkillBase.skillsList[id];
			else System.out.println("WARNING: Skill book is for non-active skill " + SkillBase.skillsList[id].name);
		} else {
			System.out.println("WARNING: Skill book's itemstack contains invalid id " + id);
		}
		return null;
	}
	
	/**
	 * Returns the active skill stored in this itemstack or null if damage value is incorrect
	 */
	private static SkillActive getSkillFromStack(ItemStack stack) {
		return getSkillFromDamage(stack.getItemDamage());
	}
	
	/**
	 * Returns string version of the  skill level this stack will teach when used, or "" if no level has been set
	 */
	private static String getLevelForDisplay(ItemStack stack) {
		return (" (Lvl " + getSkillLevel(stack) + ")");
	}
	
	/**
	 * Returns the skill level this stack will teach when used, or 1 if no level was set
	 */
	private static byte getSkillLevel(ItemStack stack)
	{
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("skillLevel"))
			return stack.getTagCompound().getByte("skillLevel");
		return 1;
	}
}
