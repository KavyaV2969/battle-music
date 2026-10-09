package dev.battlemusic.mixin;

import dev.battlemusic.client.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applied to the actual SoundManager alongside Cobblemon's filters, with its controller inactive. */
@Mixin(value=class_1144.class,remap=false)
public abstract class SoundFilterMixin {
    @Inject(method="method_4873",at=@At("HEAD"),cancellable=true)
    private void play(class_1113 sound,CallbackInfo ci){if(Playback.INSTANCE.filtered(sound))ci.cancel();}
    @Inject(method="method_4872",at=@At("HEAD"),cancellable=true)
    private void delayed(class_1113 sound,int delay,CallbackInfo ci){if(Playback.INSTANCE.filtered(sound))ci.cancel();}
}
