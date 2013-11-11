package coolalias.skillsmod.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import coolalias.skillsmod.SkillInfo;
import coolalias.skillsmod.SkillsMod;
import coolalias.skillsmod.skills.SkillActive;
import coolalias.skillsmod.skills.SkillBase;
import coolalias.skillsmod.skills.SkillBase.AttributeCode;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemSkillBook extends ItemBook
{
	public ItemSkillBook(int par1) {
		super(par1);
		setHasSubtypes(true);
		setMaxDamage(0);
		// TODO use custom texture or just use vanilla book?
		setTextureName("book_normal");
		// TODO set to one of Quest's creative tabs
		setCreativeTab(CreativeTabs.tabMisc);
	}
	
	/**
	 * Returns a new ItemStack skill book teaching given skill at given tier
	 */
	public static ItemStack getSkillBook(SkillBase skill, byte tier) {
		ItemStack book = new ItemStack(SkillsMod.skillBook,1,skill.id);
		setTier(book, tier);
		return book;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(int index, CreativeTabs tab, List list) {
		for (int i = 0; i < SkillBase.skillsList.length; ++i) {
			if (getSkillFromDamage(i) != null) { list.add(new ItemStack(index, 1, i)); }
		}
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return getItemStackDisplayName(stack);
	}
	
	// TODO this still doesn't get rid of the '.name' appended to the tooltip display
	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		SkillActive skill = getSkillFromStack(stack);
		String name = "Generic Skill Book";
		if (skill != null) { name = "Skill Book of " + skill.name + getTierForDisplay(stack); }
		return name.contains(".") ? name.substring(0, name.indexOf(".")) : name;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4) {
		SkillActive skill = getSkillFromStack(stack);
		if (skill != null) {
			list.add("Use to learn or activate");
			list.addAll(skill.getDescription());
		}
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
	{
		//if (!world.isRemote) { SkillInfo.get(player).addXp(10.0F, AttributeCode.CHA); }
		
		SkillActive skill = getSkillFromStack(stack);
		if (skill != null)
		{
			SkillInfo info = SkillInfo.get(player);
			if (info.grantSkill(skill.id, getSkillTier(stack))) {
				if (!player.capabilities.isCreativeMode) { --stack.stackSize; }
			} else if (info.getSkillLevel(skill) >= getSkillTier(stack)) {
				skill.activate(world, player);
			}
		}
		
		return stack;
	}
	
	/**
	 * Sets the skill tier this book will teach; will auto-cap at the appropriate level if necessary 
	 */
	public static void setTier(ItemStack stack, byte tier) {
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		SkillActive skill = getSkillFromStack(stack);
		byte maxLevel = skill != null ? skill.getMaxLevel() : 1;
		stack.getTagCompound().setByte("skillTier", tier < 1 ? 1 : tier > maxLevel ? maxLevel : tier);
		// TODO remove debug
		System.out.println("Tier for " + stack.getDisplayName() + " set to " + getTierForDisplay(stack));
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
	 * Returns string version of the  skill tier this stack will teach when used, or "" if no tier has been set
	 */
	private static String getTierForDisplay(ItemStack stack) {
		return EnumChatFormatting.ITALIC + (" (Lvl " + getSkillTier(stack) + ")");
	}
	
	/**
	 * Returns the skill tier this stack will teach when used, or 1 if no tier was set
	 */
	private static byte getSkillTier(ItemStack stack) {
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("skillTier"))
			return stack.getTagCompound().getByte("skillTier");
		return 1;
	}
}
