package dev.battlemusic.mixin;

import dev.battlemusic.client.Playback;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies the background envelope to startup, tickable updates, and slider changes. */
@Mixin(value=class_1140.class,remap=false)
public abstract class BackgroundVolumeMixin {
    @Shadow protected abstract float method_43222(float volume,class_3419 category);

    @Redirect(method="method_4854",at=@At(value="INVOKE",target="Lnet/minecraft/class_1140;method_43222(FLnet/minecraft/class_3419;)F"))
    private float initial(class_1140 engine,float volume,class_3419 category,class_1113 sound){
        return method_43222(volume,category)*Playback.INSTANCE.backgroundGain(sound);
    }

    @Inject(method="method_4853",at=@At("RETURN"),cancellable=true)
    private void volume(class_1113 sound,CallbackInfoReturnable<Float> ci){
        ci.setReturnValue(ci.getReturnValueF()*Playback.INSTANCE.backgroundGain(sound));
    }
}
