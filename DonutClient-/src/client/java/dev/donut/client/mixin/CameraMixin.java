package dev.donut.client.mixin;

import dev.donut.client.freecam.FreecamController;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("RETURN"))
    private void donut$freeCamera(Level level, Entity entity, boolean thirdPerson, boolean reverse, float partialTick, CallbackInfo ci) {
        if (!FreecamController.enabled()) return;
        FreecamController freecam = FreecamController.getInstance();
        setPosition(freecam.interpolatedPosition(partialTick));
        setRotation(freecam.yaw(), freecam.pitch());
        detached = true;
    }
}
