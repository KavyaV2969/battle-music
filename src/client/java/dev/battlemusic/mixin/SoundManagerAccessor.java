package dev.battlemusic.mixin;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=class_1144.class,remap=false)
public interface SoundManagerAccessor {@Accessor("field_5590") class_1140 battlemusic$engine();}
