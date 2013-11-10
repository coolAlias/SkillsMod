package coolalias.skillsmod;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import coolalias.tutorial.inventory.ContainerCustomPlayer;
import coolalias.tutorial.inventory.GuiCustomPlayerInventory;
import coolalias.tutorial.steamfurnace.ContainerSteamFurnace;
import coolalias.tutorial.steamfurnace.GuiSteamFurnace;
import coolalias.tutorial.steamfurnace.TileEntitySteamFurnace;
import cpw.mods.fml.common.network.IGuiHandler;

public class CommonProxy implements IGuiHandler
{
	/** Used to store IExtendedEntityProperties data temporarily between player death and respawn or dimension change */
	private static final Map<String, NBTTagCompound> extendedEntityData = new HashMap<String, NBTTagCompound>();

	public void registerRenderers() {}

	@Override
	public Object getServerGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z)
	{
		// if (guiId == SkillsMod.GUI_CUSTOM_INV) return new ContainerCustomPlayer(player, player.inventory, ExtendedPlayer.get(player).inventory);
		
		return null;
	}

	@Override
	public Object getClientGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z)
	{
		// if (guiId == SkillsMod.GUI_CUSTOM_INV) return new GuiCustomPlayerInventory(player, player.inventory, ExtendedPlayer.get(player).inventory);
		
		return null;
	}

	/**
	 * Adds an entity's custom data to the map for temporary storage
	 */
	public static void storeEntityData(String name, NBTTagCompound compound) {
		extendedEntityData.put(name, compound);
	}

	/**
	 * Removes the compound from the map and returns the NBT tag stored for name or null if none exists
	 */
	public static NBTTagCompound getEntityData(String name) {
		return extendedEntityData.remove(name);
	}
}
