package coolalias.skillsmod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import coolalias.skillsmod.skills.SkillBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;

public class PacketHandler implements IPacketHandler
{
	/** Defining packet ids allow for subtypes of Packet250CustomPayload all on single channel */
	public static final byte SYNC_PLAYER_INFO = 1, OPEN_SERVER_GUI = 2, ADD_XP = 3, UPDATE_ATTRIBUTE = 4;

	@Override
	public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player)
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(packet.data);
		DataInputStream inputStream = new DataInputStream(bis);
		
		byte packetType;

		try {
			packetType = inputStream.readByte();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		if (packet.channel.equals("skillsmod"))
		{
			switch(packetType) {
			case SYNC_PLAYER_INFO: handleSyncPlayerInfo(player, inputStream); break;
			case OPEN_SERVER_GUI: handleOpenGuiPacket((EntityPlayer) player, inputStream); break;
			case ADD_XP: handleAddXpPacket((EntityPlayer) player, inputStream); break;
			case UPDATE_ATTRIBUTE: handleUpdateAttribute((EntityPlayer) player, inputStream); break;
			default: System.out.println("[PACKET][WARNING] Unknown packet type " + packetType);
			}
		}
		
		closeStream(bis);
	}
	
	/**
	 * Closes an input or output stream
	 */
	public static void closeStream(Closeable c)
	{
		try {
			if(c != null) {
				c.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends a packet to the server telling it to open gui for player
	 */
	public static final void sendOpenGuiPacket(int guiId)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream outputStream = new DataOutputStream(bos);
		
		try {
			outputStream.writeByte(OPEN_SERVER_GUI);
			outputStream.writeInt(guiId);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			closeStream(outputStream);
			closeStream(bos);
		}
		
		PacketDispatcher.sendPacketToServer(PacketDispatcher.getPacket("skillsmod", bos.toByteArray()));
	}
	
	/**
	 * Sends packet to synchronize ExtendedPlayer properties
	 */
	public static final void sendSyncSkillsPacket(EntityPlayer player)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream outputStream = new DataOutputStream(bos);
		
		try {
			outputStream.writeByte(SYNC_PLAYER_INFO);
			SkillInfo.get(player).writeToStream(outputStream);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			closeStream(outputStream);
			closeStream(bos);
		}

		PacketDispatcher.sendPacketToPlayer(PacketDispatcher.getPacket("skillsmod", bos.toByteArray()), (Player) player);
	}
	
	public static final void sendAttributePacket(EntityPlayer player, SkillBase attribute)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream outputStream = new DataOutputStream(bos);
		System.out.println("Sending attribute update packet for " + attribute.name);
		try {
			outputStream.writeByte(UPDATE_ATTRIBUTE);
			attribute.writeToStream(outputStream);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			closeStream(outputStream);
			closeStream(bos);
		}

		PacketDispatcher.sendPacketToPlayer(PacketDispatcher.getPacket("skillsmod", bos.toByteArray()), (Player) player);
	}
	
	public static final void sendAddXpPacket(EntityPlayer player, float amount, byte id)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream outputStream = new DataOutputStream(bos);

		try {
			outputStream.writeByte(ADD_XP);
			outputStream.writeFloat(amount);
			outputStream.writeByte(id);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			closeStream(outputStream);
			closeStream(bos);
		}
		
		if (player.worldObj.isRemote) PacketDispatcher.sendPacketToServer(PacketDispatcher.getPacket("skillsmod", bos.toByteArray()));
		else {
			System.out.println("WARNING: Sending addXP packet from server to client... shouldn't be happening");
			//PacketDispatcher.sendPacketToPlayer(PacketDispatcher.getPacket("skillsmod", bos.toByteArray()), (Player) player);
		}
	}

	/**
	 * Handles extended properties packets; note that DataInputStream is now a parameter and not opened
	 * from within the method (unlike in the IExtendedEntityProperties tutorial)
	 */
	private void handleSyncPlayerInfo(Player player, DataInputStream inputStream)
	{
		try {
			SkillInfo.get((EntityPlayer) player).readFromStream(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeStream(inputStream);
		}
	}
	
	private void handleAddXpPacket(EntityPlayer player, DataInputStream inputStream)
	{
		if (player.worldObj.isRemote) System.out.println("WARNING: addXP packet received on client side!!!");
		else System.out.println("Handling addXP packet on the server");
		try {
			float amount = inputStream.readFloat();
			byte id = inputStream.readByte();
			SkillInfo.get(player).addXp(amount, id);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeStream(inputStream);
		}
	}
	
	private void handleUpdateAttribute(EntityPlayer player, DataInputStream inputStream)
	{
		try {
			try {
				SkillInfo.get(player).updateAttributeFromStream(inputStream);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				closeStream(inputStream);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method will open the appropriate server gui element for the player
	 */
	private void handleOpenGuiPacket(EntityPlayer player, DataInputStream inputStream)
	{
		int guiId;

		try {
			guiId = inputStream.readInt();
			player.openGui(SkillsMod.instance, guiId, player.worldObj, (int) player.posX, (int) player.posY, (int) player.posZ);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeStream(inputStream);
		}
	}
}