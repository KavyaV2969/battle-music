package dev.battlemusic.mixin;
import net.minecraft.*;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=class_1140.class,remap=false)
public interface SoundSystemAccessor {
    @Accessor("field_18950") Map<class_1113,class_4235.class_4236> battlemusic$channels();
    @Invoker("method_4853") float battlemusic$volume(class_1113 sound);
}
