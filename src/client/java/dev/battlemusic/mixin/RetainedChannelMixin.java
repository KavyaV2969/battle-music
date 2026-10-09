package dev.battlemusic.mixin;

import dev.battlemusic.client.ManagedSound;
import net.minecraft.*;
import java.util.concurrent.CompletableFuture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** The reserved streaming pool has only 2-8 sources. Retained voices use the
 * general source pool; their audio still uses the normal streaming decoder path. */
@Mixin(value=class_1140.class,remap=false)
public abstract class RetainedChannelMixin {
    @Redirect(method="method_4854",at=@At(value="INVOKE",target="Lnet/minecraft/class_4235;method_19723(Lnet/minecraft/class_4225$class_4105;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<class_4235.class_4236> allocate(class_4235 channels,class_4225.class_4105 pool,class_1113 sound){
        return channels.method_19723(sound instanceof ManagedSound?class_4225.class_4105.field_18352:pool);
    }
}
