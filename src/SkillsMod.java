package coolalias.skillsmod;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import coolalias.skillsmod.entity.skill.EntityFireBlast;
import coolalias.skillsmod.items.ItemSkillBook;
import coolalias.skillsmod.skills.SkillActive;
import coolalias.skillsmod.skills.SkillBase;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;

@Mod(modid = "skillsmod", name = "Skills Mod", version = "0.1.0")
@NetworkMod(clientSideRequired=true, serverSideRequired=false, channels = {"skillsmod"}, packetHandler = PacketHandler.class)

/**
 * 
 * @author coolAlias
 * 
 * Simple mod demonstrating adding player skills, attributes and xp
 *
 */
public final class SkillsMod
{
	@Instance("skillsmod")
	public static SkillsMod instance = new SkillsMod();

	@SidedProxy(clientSide = "coolalias.skillsmod.ClientProxy", serverSide = "coolalias.skillsmod.CommonProxy")
	public static CommonProxy proxy;

	/** This is used to keep track of GUIs that we make*/
	private static int modGuiIndex = 0;
	
	private static int modItemIndex = 9192, modEntityIndex = 0;
	
	public static final Item skillBook = new ItemSkillBook(modItemIndex++).setUnlocalizedName("skillBook");

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		EntityRegistry.registerModEntity(EntityFireBlast.class, "Fire Blast", ++modEntityIndex, this, 64, 10, true);
	}

	@EventHandler
	public void load(FMLInitializationEvent event)
	{
		proxy.registerRenderers();
		addNames();
		addRecipes();
		MinecraftForge.EVENT_BUS.register(new SkillsHandler());
		NetworkRegistry.instance().registerGuiHandler(this, new CommonProxy());
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
	}
	
	private void addNames() {
		for (int i = 0; i < SkillBase.skillsList.length; ++i) {
			if (SkillBase.skillsList[i] instanceof SkillActive) {
				for (int j = 1; j <= SkillBase.skillsList[i].getMaxLevel(); ++j) {
					ItemStack book = ItemSkillBook.getSkillBook(SkillBase.skillsList[i], (byte) j);
					if (book != null) { LanguageRegistry.addName(book, ((ItemSkillBook) book.getItem()).getItemStackDisplayName(book)); }
				}
			}
		}
	}
	
	private void addRecipes() {
		GameRegistry.addShapelessRecipe(ItemSkillBook.getSkillBook(SkillBase.skillsList[SkillBase.fireBlast.id], (byte) 1), Item.book, Item.gunpowder);
		GameRegistry.addShapelessRecipe(ItemSkillBook.getSkillBook(SkillBase.skillsList[SkillBase.fireBlast.id], (byte) 2), Item.book, Item.gunpowder, Item.gunpowder);
		GameRegistry.addShapelessRecipe(ItemSkillBook.getSkillBook(SkillBase.skillsList[SkillBase.fireBlast.id], (byte) 3), Item.book, Item.gunpowder, Item.gunpowder, Item.gunpowder);
		GameRegistry.addShapelessRecipe(ItemSkillBook.getSkillBook(SkillBase.skillsList[SkillBase.fireBlast.id], (byte) 4), Item.book, Item.gunpowder, Item.gunpowder, Item.gunpowder, Item.gunpowder);
		GameRegistry.addShapelessRecipe(ItemSkillBook.getSkillBook(SkillBase.skillsList[SkillBase.fireBlast.id], (byte) 5), Item.book, Item.gunpowder, Item.gunpowder, Item.gunpowder, Item.gunpowder, Item.gunpowder);
	}
}