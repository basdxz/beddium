package org.embeddedt.embeddium.impl.chunk;

import com.mojang.blaze3d.vertex.PoseStack;
import org.embeddedt.embeddium.impl.Embeddium;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.embeddedt.embeddium.api.ChunkMeshEvent;

@Mod.EventBusSubscriber(modid = Embeddium.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AddSectionGeometryHandler {
    private static final ThreadLocal<PoseStack> DUMMY_POSE_STACK = ThreadLocal.withInitial(PoseStack::new);

    //TODO: [VEN] Forge needs a ChunkMeshEvent event fr
//    @SubscribeEvent
//    public static void onChunkMesh(ChunkMeshEvent meshEvent) {
//        AddSectionGeometryEvent geometryEvent = new AddSectionGeometryEvent(meshEvent.getSectionOrigin().origin(), meshEvent.getWorld());
//        MinecraftForge.EVENT_BUS.post(geometryEvent);
//        if(!geometryEvent.getAdditionalRenderers().isEmpty()) {
//            for (var renderer : geometryEvent.getAdditionalRenderers()) {
//                meshEvent.addMeshAppender(ctx -> {
//                    renderer.render(new AddSectionGeometryEvent.SectionRenderingContext(ctx.vertexConsumerProvider(), ctx.blockRenderView(), DUMMY_POSE_STACK.get()));
//                });
//            }
//        }
//    }
}
