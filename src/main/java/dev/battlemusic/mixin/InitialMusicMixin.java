package dev.battlemusic.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.interpreter.instructions.InitializeInstruction;
import dev.battlemusic.server.Sessions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Trainer leads may not have entities/active slots yet when native music starts. */
@Mixin(value=InitializeInstruction.class,remap=false)
public abstract class InitialMusicMixin {
    @Inject(method="invoke",at=@At("HEAD"))
    private void initial(PokemonBattle battle,CallbackInfo ci){
        var instruction=(InitializeInstruction)(Object)this;
        Sessions.initializing(battle,instruction);
    }
}
