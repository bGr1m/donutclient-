package dev.donut.client.mixin;

import dev.donut.client.freecam.FreecamController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void donut$hideBodyAroundCamera(Entity entity, Frustum frustum,
                                          double cameraX, double cameraY, double cameraZ,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (!FreecamController.enabled() || entity != Minecraft.getInstance().player) return;
        // Use the actual interpolated render camera, rather than its next simulation position.
        Vec3 camera = new Vec3(cameraX, cameraY, cameraZ);
        if (entity.getBoundingBox().inflate(0.1).contains(camera)
                || entity.getEyePosition().distanceToSqr(camera) < 0.65 * 0.65) {
            cir.setReturnValue(false);
        }
    }
}
