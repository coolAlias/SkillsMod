package coolalias.skillsmod;

import net.minecraft.client.renderer.entity.RenderFireball;
import coolalias.skillsmod.entity.skill.EntityFireBlast;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy
{
	@Override
	public void registerRenderers() {
		RenderingRegistry.registerEntityRenderingHandler(EntityFireBlast.class, new RenderFireball(1.0F));
	}
}
