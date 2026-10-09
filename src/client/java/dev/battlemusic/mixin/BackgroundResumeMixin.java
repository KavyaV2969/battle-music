package dev.battlemusic.mixin;

import com.cobblemon.mod.common.util.SoundExtensionsKt;
import dev.battlemusic.client.Playback;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cobblemon's own ending sound must not resume paused BGM during our outro. */
@Mixin(value=SoundExtensionsKt.class,remap=false)
public abstract class BackgroundResumeMixin {
    @Inject(method="resumeSounds",at=@At("HEAD"),cancellable=true)
    private static void resume(class_1144 manager,class_2960 event,class_3419 category,CallbackInfo ci){
        if(Playback.INSTANCE.blocksBackgroundResume(category))ci.cancel();
    }
}
