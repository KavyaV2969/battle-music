package dev.battlemusic.client;

import com.terraformersmc.modmenu.api.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2561;
import me.shedaniel.clothconfig2.api.ConfigBuilder;

public final class MusicModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory(){
        if(!FabricLoader.getInstance().isModLoaded("cloth-config"))return null;
        return parent->{
            var builder=ConfigBuilder.create().setParentScreen(parent).setTitle(class_2561.method_43471("battlemusic.settings.title"));
            var category=builder.getOrCreateCategory(class_2561.method_43471("battlemusic.settings.trainer"));
            category.addEntry(builder.entryBuilder().startBooleanToggle(class_2561.method_43471("battlemusic.settings.trainer_entrances"),ClientSettings.trainerEntrances())
                .setDefaultValue(false).setTooltip(class_2561.method_43471("battlemusic.settings.trainer_entrances.tooltip"))
                .setSaveConsumer(ClientSettings::setTrainerEntrances).build());
            return builder.setSavingRunnable(ClientSettings::save).build();
        };
    }
}
