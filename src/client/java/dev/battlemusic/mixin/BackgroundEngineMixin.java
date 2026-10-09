package dev.battlemusic.mixin;

import dev.battlemusic.client.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Closing a battle screen resumes the engine globally; preserve our paused voices. */
@Mixin(value=class_1140.class,remap=false,priority=1100)
public abstract class BackgroundEngineMixin {
    @Inject(method="method_19762",at=@At("HEAD"),cancellable=true)
    private void resume(CallbackInfo ci){
        var playback=Playback.INSTANCE;
        if(!playback.suppressBackground())return;
        ((SoundSystemAccessor)this).battlemusic$channels().forEach((sound,channel)->{
            if(!playback.filtered(sound)&&!(sound instanceof ManagedSound managed&&managed.paused))
                channel.method_19735(class_4224::method_19654);
        });
        ci.cancel();
    }
}
