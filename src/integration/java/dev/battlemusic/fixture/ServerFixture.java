package dev.battlemusic.fixture;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.*;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.srcmc.rctapi.api.RCTApi;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import dev.battlemusic.BattleMusicMod;
import com.google.gson.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.*;
import java.nio.file.*;
import java.util.*;

public final class ServerFixture implements ModInitializer {
    private PokemonBattle battle;private PokemonEntity wild;private class_1309 trainerEntity;
    private UUID geodude,arceus;
    private int phase,ticks,ready,switches,eliteMember;private boolean finished;
    @Override public void onInitialize(){
        if(!Path.of("").toAbsolutePath().normalize().endsWith(Path.of("battle-music/integration/game")))throw new IllegalStateException("Fixture refuses to run outside its isolated game directory");
        ServerTickEvents.END_SERVER_TICK.register(server->{if(finished)return;try{
            if(server.method_3760().method_14571().isEmpty()||BattleMusicMod.rules.revision()==0)return;
            if(++ready<160)return;if(++ticks>2400)throw new IllegalStateException("Timeout phase "+phase+" expectation "+Shared.expect);
            if(Shared.error!=null)throw new IllegalStateException(Shared.error);
            var player=server.method_3760().method_14571().getFirst();
            switch(phase){
                case 0 -> {startWild(player,"rattata");Shared.request("wild");phase++;}
                case 1 -> {if(done()){stop();Shared.request("ended");phase++;}}
                case 2 -> {if(done()){startWild(player,"arceus");Shared.request("special");phase++;}}
                case 3 -> {if(done()){stop();Shared.request("ended");phase++;}}
                case 4 -> {if(done()){startTrainer(player);Shared.request("trainer");phase++;}}
                case 5 -> {if(done()&&act("splash",true)){Shared.request("overlay");phase++;}}
                case 6 -> {if(done()){if(!Shared.expect.equals("pause-retained"))Shared.request("pause-retained");else{Shared.request("toggle-off");phase++;}}}
                case 7 -> {if(done()){
                    if(switches==0&&act("splash",Integer.valueOf(0))){Shared.request("switch-resumed");switches++;}
                    else if(switches==1&&act("splash",true)){Shared.request("reentered");switches++;}
                    else if(switches==2&&act("seismictoss",false)){Shared.request("resumed");phase++;}
                }}
                case 8 -> {if(done()){if(!Shared.expect.equals("rapid"))Shared.request("rapid");else{Shared.request("stale");phase++;}}}
                case 9 -> {if(done()){Shared.request("missing");phase++;}}
                case 10 -> {if(done()){stop();Shared.request("ended");phase++;}}
                case 11 -> {if(done()){startTrainer(player,"kanto_champion_blue",true);Shared.request("champion");phase++;}}
                case 12 -> {
                    Shared.championArceusActive=false;
                    for(var actor:battle.getActors())if(!(actor instanceof PlayerBattleActor))for(var active:actor.getSide().getActivePokemon()){
                        var bp=active.getBattlePokemon();
                        if(bp!=null&&bp.getHealth()>0&&bp.getEffectedPokemon().getSpecies().getResourceIdentifier().toString().equals("cobblemon:arceus"))Shared.championArceusActive=true;
                    }
                    if(done()){stop();Shared.request("ended");phase++;}
                }
                case 13 -> {if(done()){
                    if(eliteMember==4)finish(null);
                    else{String member=List.of("lorelei","bruno","agatha","lance").get(eliteMember++);startTrainer(player,"kanto_league_"+member,false);Shared.request("elite-"+member);phase++;}
                }}
                case 14 -> {if(done()){stop();Shared.request("ended");phase=13;}}
            }
        }catch(Throwable e){finish(e);}});
    }
    private boolean done(){return Shared.expect.equals(Shared.ack);}
    private Pokemon pokemon(class_3222 player,String species,int level){var props=new PokemonProperties();props.setSpecies(species);props.setLevel(level);props.setMoves(List.of("splash","seismictoss"));return props.create(player);}
    private void party(class_3222 player){var party=Cobblemon.INSTANCE.getStorage().getParty(player);for(int i=0;i<6;i++)if(party.get(i)!=null)party.remove(party.get(i));party.add(pokemon(player,"mew",100));}
    private void startWild(class_3222 player,String species){
        party(player);var world=player.method_51469();var props=new PokemonProperties();props.setSpecies(species);props.setLevel(5);props.setMoves(List.of("splash"));
        wild=props.createEntity(world);wild.method_5814(player.method_23317()+2,player.method_23318(),player.method_23321());wild.method_5977(true);world.method_8649(wild);
        var party=Cobblemon.INSTANCE.getStorage().getParty(player);
        var result=BattleBuilder.INSTANCE.pve(player,wild,party.get(0).getUuid(),BattleFormat.Companion.getGEN_9_SINGLES(),false,false,-1f,party);
        if(!(result instanceof SuccessfulBattleStart success))throw new IllegalStateException("Wild battle rejected: "+result);battle=success.getBattle();
    }
    private void startTrainer(class_3222 player)throws Exception{
        startTrainer(player,"kanto_brock",false);
    }
    private void startTrainer(class_3222 player,String trainerId,boolean specialLead)throws Exception{
        party(player);
        var entry=RCTApi.getInstances().filter(e->e.getValue().getTrainerRegistry().getById(trainerId) instanceof TrainerNPC).findFirst().orElseThrow();
        var api=entry.getValue();var trainer=(TrainerNPC)api.getTrainerRegistry().getById(trainerId);
        var team=trainer.getTeam();team[0]=pokemon(player,specialLead?"arceus":"geodude",5);team[1]=pokemon(player,specialLead?"geodude":"arceus",5);
        // Some supplied boss teams exceed Cobblemon's clone-time EV limit. This fixture
        // runs only in the guarded isolated directory and never saves trainer templates.
        for(var member:team)if(member!=null)member.setEvs$common(new com.cobblemon.mod.common.pokemon.EVs());
        Class<?> mob=Class.forName("com.gitlab.srcmc.rctmod.world.entities.TrainerMob");
        trainerEntity=(class_1309)((class_1299<?>)mob.getMethod("getEntityType").invoke(null)).method_5883(player.method_51469());
        mob.getMethod("setTrainerId",String.class).invoke(trainerEntity,trainerId);trainerEntity.method_5814(player.method_23317()+2,player.method_23318(),player.method_23321());player.method_51469().method_8649(trainerEntity);trainer.setEntity(trainerEntity);
        var me=api.getTrainerRegistry().getById("battlemusic_fixture",com.gitlab.srcmc.rctapi.api.trainer.TrainerPlayer.class);
        if(me==null)me=api.getTrainerRegistry().registerPlayer("battlemusic_fixture",player);
        if(!api.getBattleManager().startSingle(me,trainer))throw new IllegalStateException("RCT battle rejected: "+trainerId);
        battle=BattleRegistry.getBattleByParticipatingPlayer(player);
        if(battle==null)throw new IllegalStateException("RCT battle missing");
        // RCT creates battle copies, whose UUIDs differ from the registry team templates.
        for(var actor:battle.getActors())if(!(actor instanceof PlayerBattleActor))for(var bp:actor.getPokemonList()){
            String id=bp.getOriginalPokemon().getSpecies().getResourceIdentifier().toString();
            if(id.equals("cobblemon:geodude"))geodude=bp.getUuid();if(id.equals("cobblemon:arceus"))arceus=bp.getUuid();
        }
        if(geodude==null||arceus==null)throw new IllegalStateException("Missing fixture opponent UUIDs");
        System.out.println("BATTLEMUSIC RCT registry: "+entry.getKey());
    }
    private boolean act(String move,boolean switchOpponent){
        return act(move,switchOpponent?Integer.valueOf(1):null);
    }
    private boolean act(String move,Integer switchSlot){
        for(var actor:battle.getActors())if(actor.getRequest()==null||actor instanceof PlayerBattleActor&&!actor.getMustChoose())return false;
        for(var actor:battle.getActors()){
            ShowdownActionResponse response=actor instanceof PlayerBattleActor?new MoveActionResponse(move,null,null):switchSlot!=null?new SwitchActionResponse(switchSlot==0?geodude:arceus):new MoveActionResponse("splash",null,null);
            actor.setResponses(new ArrayList<>(List.of(response)));actor.setMustChoose(false);
        }
        battle.checkForInputDispatch();return true;
    }
    private void stop(){
        if(battle!=null){
            var manager=com.gitlab.srcmc.rctapi.api.battle.BattleManager.of(battle);
            if(manager!=null)manager.end(battle.getBattleId(),true);else battle.stop();
        }
        battle=null;if(wild!=null)wild.method_31472();wild=null;if(trainerEntity!=null)trainerEntity.method_31472();trainerEntity=null;
    }
    private void finish(Throwable error){
        finished=true;var report=new JsonObject();report.addProperty("success",error==null);report.addProperty("phase",phase);report.add("checks",new Gson().toJsonTree(Shared.checks));report.add("measurements",Shared.measurements);
        if(error!=null){report.addProperty("error",error.toString());if(battle!=null)report.add("battleLog",new Gson().toJsonTree(battle.getBattleLog()));error.printStackTrace();}
        try{Path out=Path.of("battlemusic-live.json");Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){throw new RuntimeException(e);}
    }
}
