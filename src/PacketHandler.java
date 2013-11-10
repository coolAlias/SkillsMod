package coolalias.skillsmod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;

public class PacketHandler implements IPacketHandler
{
	/** Defining packet ids allow for subtypes of Packet250CustomPayload all on single channel */
	public static final byte SKILLS_PACKET = 1;

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
			case SKILLS_PACKET: handleExtendedProperties(packet, player, inputStream); break;
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
	 * Sends packet to synchronize ExtendedPlayer properties
	 */
	public static final void sendSyncSkillsPacket(EntityPlayer player)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream outputStream = new DataOutputStream(bos);

		try {
			outputStream.writeByte(SKILLS_PACKET);
			SkillInfo.get(player).writeToStream(outputStream);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			closeStream(outputStream);
			closeStream(bos);
		}

		PacketDispatcher.sendPacketToPlayer(PacketDispatcher.getPacket("skillsmod", bos.toByteArray()), (Player) player);
	}

	/**
	 * Handles extended properties packets; note that DataInputStream is now a parameter and not opened
	 * from within the method (unlike in the IExtendedEntityProperties tutorial)
	 */
	private void handleExtendedProperties(Packet250CustomPayload packet, Player player, DataInputStream inputStream)
	{
		try {
			SkillInfo.get((EntityPlayer) player).readFromStream(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {
			closeStream(inputStream);
		}
	}
}