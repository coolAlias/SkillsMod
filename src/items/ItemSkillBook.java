package coolalias.skillsmod.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import coolalias.skillsmod.skills.SkillActive;
import coolalias.skillsmod.skills.SkillBase;
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
	public static ItemStack getSkillBook(SkillBase skill, int tier) {
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
		String name = getItemStackDisplayName(stack);
		return name.substring(0, name.indexOf("."));
	}
	
	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		SkillActive skill = getSkillFromStack(stack);
		if (skill != null) { return "Skill Book of " + skill.name + getTierForDisplay(stack); }
		return "Generic Skill Book";
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4) {
		SkillActive skill = getSkillFromStack(stack);
		if (skill != null) { list.addAll(skill.getDescription()); }
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
	{
		SkillActive skill = getSkillFromStack(stack);
		if (skill != null) {
			
			if (!player.capabilities.isCreativeMode) {
				--stack.stackSize;
			}
		}
		return stack;
	}
	
	/**
	 * Sets the skill tier this book will teach; will auto-cap at the appropriate level if necessary 
	 */
	public static void setTier(ItemStack stack, int tier) {
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		SkillActive skill = getSkillFromStack(stack);
		int maxLevel = skill != null ? skill.getMaxLevel() : 1;
		stack.getTagCompound().setInteger("skillTier", tier < 1 ? 1 : tier > maxLevel ? maxLevel : tier);
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
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("skillTier"))
			return " Tier " + stack.getTagCompound().getInteger("skillTier");
		return "";
	}
	
	/**
	 * Returns the skill tier this stack will teach when used, or 1 if no tier was set
	 */
	private static int getSkillTier(ItemStack stack) {
		return 1;
	}
}
