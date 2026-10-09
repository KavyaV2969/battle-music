package dev.battlemusic.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import dev.battlemusic.server.Sessions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=PokemonBattle.class,remap=false)
public abstract class BattleTickMixin {
    @Inject(method="tick",at=@At("RETURN")) private void afterDispatch(CallbackInfo ci){Sessions.tick((PokemonBattle)(Object)this);}
    @Inject(method={"end","stop"},at=@At("HEAD")) private void beforeEnd(CallbackInfo ci){Sessions.end((PokemonBattle)(Object)this);}
}
