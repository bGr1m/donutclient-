package dev.donut.client.mixin;

import dev.donut.client.freecam.FreecamController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void donut$turnCamera(LocalPlayer player, double dx, double dy) {
        if (FreecamController.enabled()) FreecamController.getInstance().turn(dx, dy);
        else player.turn(dx, dy);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void donut$preventHotbarScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (FreecamController.enabled() && Minecraft.getInstance().screen == null) ci.cancel();
    }
}
