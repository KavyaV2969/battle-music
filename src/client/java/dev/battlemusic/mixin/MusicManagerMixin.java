package dev.battlemusic.mixin;
import net.minecraft.class_1142;
import dev.battlemusic.client.Playback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=class_1142.class,remap=false)
public abstract class MusicManagerMixin {
    @Inject(method="method_18669",at=@At("HEAD"),cancellable=true)
    private void tick(CallbackInfo ci){if(Playback.INSTANCE.suppressBackground())ci.cancel();}
}
