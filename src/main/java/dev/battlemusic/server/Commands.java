package dev.battlemusic.server;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.battlemusic.BattleMusicApi;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.*;

public final class Commands {
    public static void register(){CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->
        dispatcher.register(class_2170.method_9247("battlemusic").requires(s->s.method_9259(2))
            .then(class_2170.method_9247("explain").executes(c->{String text=Sessions.explain(c.getSource().method_9207().method_5667());c.getSource().method_9226(()->class_2561.method_43470(text),false);return 1;}))
            .then(class_2170.method_9247("encounter").then(class_2170.method_9244("pokemon",class_2186.method_9309())
                .then(class_2170.method_9247("clear").executes(c->set(c,null)))
                .then(class_2170.method_9247("set").then(class_2170.method_9244("key",StringArgumentType.word()).executes(c->set(c,StringArgumentType.getString(c,"key")))))))));
    }
    private static int set(com.mojang.brigadier.context.CommandContext<class_2168> c,String key)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        if(!(class_2186.method_9313(c,"pokemon") instanceof PokemonEntity entity))throw new SimpleCommandExceptionType(class_2561.method_43470("Select a loaded Pokemon entity.")).create();
        try{BattleMusicApi.setEncounterKey(entity.getPokemon(),key);}catch(IllegalArgumentException e){throw new SimpleCommandExceptionType(class_2561.method_43470(e.getMessage())).create();}
        c.getSource().method_9226(()->class_2561.method_43470("Encounter key "+(key==null?"removed":key)+" for "+entity.getPokemon().getUuid()),true);return 1;
    }
}
