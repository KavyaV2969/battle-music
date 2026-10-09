package dev.battlemusic.server;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.*;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMusicPacket;
import dev.battlemusic.*;
import dev.battlemusic.net.*;
import dev.battlemusic.rules.*;
import dev.battlemusic.rules.EntranceSelection.Candidate;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.battles.interpreter.instructions.InitializeInstruction;
import com.cobblemon.mod.common.battles.interpreter.instructions.SwitchInstruction;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_2960;
import kotlin.Unit;
import java.util.*;

/** All access is on the logical server thread. Pending cancelled battles are weakly referenced. */
public final class Sessions {
    private static final Map<PokemonBattle,Map<UUID,Session>> PENDING=new WeakHashMap<>();
    private static final Map<UUID,Map<UUID,Session>> ACTIVE=new HashMap<>();
    private static final Set<String> DEFAULTS=Set.of("cobblemon:battle.pvw.default","cobblemon:battle.pvn.default","cobblemon:battle.pvp.default");
    public static final class Session {
        final RuleSet rules;final PlayerBattleActor viewer;final String type;final String fallback;
        final Map<UUID,ContextFactory.Location> locations=new HashMap<>();
        final Map<ActiveBattlePokemon,UUID> recalling=new IdentityHashMap<>();
        final Map<ActiveBattlePokemon,BattlePokemon> initialActive=new IdentityHashMap<>();
        public final String baseRule;String base;float volume=1,pitch=1;long revision=0;MusicState last;
        String entranceRule="none";List<Context> activeContexts=List.of();
        Session(RuleSet r,PlayerBattleActor v,String t,String fallback,String base,String baseRule){rules=r;viewer=v;type=t;this.fallback=fallback;this.base=base;this.baseRule=baseRule;}
    }
    public static void prepare(PokemonBattle battle){
        if(RaidGuard.excluded(battle))return;
        var prepared=new HashMap<UUID,Session>();
        for(var actor:battle.getActors())if(actor instanceof PlayerBattleActor viewer){
            var player=viewer.getEntity();if(player==null||!ServerPlayNetworking.canSend(player,MusicPayload.ID))continue;
            String nativeSound=viewer.getBattleTheme()==null?null:viewer.getBattleTheme().toString();
            if(nativeSound==null||nativeSound.startsWith("lumymon:")||nativeSound.startsWith("cobblemonraiddens:"))continue;
            var opponents=opponents(battle,viewer);
            if(opponents.stream().anyMatch(a->a instanceof EntityBackedBattleActor<?> e&&RaidGuard.excluded(e.getEntity())))continue;
            if(opponents.isEmpty()||opponents.stream().anyMatch(a->a.getType()==ActorType.PLAYER))continue;
            String type=opponents.stream().anyMatch(a->a.getType()==ActorType.NPC)?"trainer":"wild";
            RuleSet snapshot=BattleMusicMod.rules;Rule chosen=null;
            var locations=new HashMap<UUID,ContextFactory.Location>();
            for(var enemy:opponents){
                var loc=ContextFactory.location(viewer,enemy);locations.put(enemy.getUuid(),loc);
                for(var p:enemy.getPokemonList()){
                    var context=ContextFactory.create(type,enemy,p.getOriginalPokemon(),p.getEffectedPokemon(),loc);
                    Rule candidate=snapshot.select("base",context).orElse(null);
                    if(candidate!=null&&(chosen==null||better(candidate,chosen)))chosen=candidate;
                }
            }
            boolean nativeExplicit=!DEFAULTS.contains(nativeSound);
            boolean use=chosen!=null&&(!nativeExplicit||chosen.priority()>400);
            String selected=use&&chosen.sound()!=null?chosen.sound():nativeSound;
            Session session=new Session(snapshot,viewer,type,nativeSound,selected,use?chosen.id():nativeExplicit?"native:explicit":"native:default");
            session.locations.putAll(locations);prepared.put(viewer.getUuid(),session);
            if(!selected.equals(nativeSound))viewer.setBattleTheme(class_2960.method_60654(selected));
        }
        if(!prepared.isEmpty()){
            PENDING.put(battle,prepared);
            battle.getOnEndHandlers().add(b->{end(b);return Unit.INSTANCE;});
        }
    }
    private static boolean better(Rule a,Rule b){return a.priority()>b.priority()||(a.priority()==b.priority()&&a.id().compareTo(b.id())<0);}
    private static List<BattleActor> opponents(PokemonBattle b,PlayerBattleActor viewer){var result=new ArrayList<BattleActor>();for(var a:b.getActors())if(a.getSide()!=viewer.getSide())result.add(a);return result;}
    /** Read Showdown's actual opening switches, without changing Cobblemon's battle state. */
    public static void initializing(PokemonBattle battle,InitializeInstruction initial){
        var sessions=PENDING.get(battle);if(sessions==null)return;
        for(var instruction:initial.getInstructionSet().getSubsequentInstructions(initial))if(instruction instanceof SwitchInstruction change){
            var message=change.getPublicMessage();
            var slot=message.actorAndActivePokemon(0,battle);var pokemon=message.battlePokemon(0,battle);
            if(slot!=null&&pokemon!=null)for(var s:sessions.values())if(slot.getFirst().getSide()!=s.viewer.getSide())
                s.initialActive.put(slot.getSecond(),pokemon);
        }
    }
    /** Called before the native packet is sent on the same ordered connection. */
    public static void beforeMusic(PlayerBattleActor viewer,BattleMusicPacket packet){
        var battle=viewer.getBattle();var sessions=ACTIVE.get(battle.getBattleId());
        if(sessions==null){sessions=PENDING.remove(battle);if(sessions!=null)ACTIVE.put(battle.getBattleId(),sessions);}
        if(sessions==null)return;Session s=sessions.get(viewer.getUuid());if(s==null)return;
        if(packet.getMusic()==null){sendEnd(battle,s);sessions.remove(viewer.getUuid());return;}
        s.base=packet.getMusic().toString();s.volume=packet.getVolume();s.pitch=packet.getPitch();
        publish(battle,s,true);
    }
    public static void tick(PokemonBattle battle){var sessions=ACTIVE.get(battle.getBattleId());if(sessions!=null)for(var s:List.copyOf(sessions.values()))publish(battle,s,false);}
    public static void recalling(PokemonBattle battle,BattleActor actor,ActiveBattlePokemon slot){
        var bp=slot.getBattlePokemon();var sessions=ACTIVE.get(battle.getBattleId());
        if(bp==null||sessions==null)return;
        for(var s:List.copyOf(sessions.values()))if(actor.getSide()!=s.viewer.getSide()){
            s.recalling.put(slot,bp.getOriginalPokemon().getUuid());publish(battle,s,false);
        }
    }
    public static void fainting(PokemonBattle battle,String pnx){
        var slot=battle.getActorAndActiveSlotFromPNX(pnx);recalling(battle,slot.getFirst(),slot.getSecond());
    }
    private static void publish(PokemonBattle battle,Session s,boolean force){
        if(s.viewer.getEntity()==null)return;
        var candidates=new ArrayList<Candidate>();var contexts=new ArrayList<Context>();int slot=0;
        for(var enemy:opponents(battle,s.viewer))for(var active:enemy.getSide().getActivePokemon()){
            if(active.getActor()!=enemy)continue;
            var bp=active.getBattlePokemon();int currentSlot=slot++;
            if(bp==null)bp=s.initialActive.get(active);else s.initialActive.remove(active);
            UUID outgoing=s.recalling.get(active);
            if(outgoing!=null){
                if(bp==null||outgoing.equals(bp.getOriginalPokemon().getUuid()))continue;
                s.recalling.remove(active);
            }
            if(bp==null||bp.getGone()||bp.getHealth()<=0)continue;
            var loc=s.locations.get(enemy.getUuid());if(loc==null)continue;
            var context=ContextFactory.create(s.type,enemy,bp.getOriginalPokemon(),bp.getEffectedPokemon(),loc);
            contexts.add(context);
            var rule=s.rules.select("entrance",context).orElse(null);
            if(rule!=null&&rule.sound()!=null)candidates.add(new Candidate(rule,bp.getOriginalPokemon().getUuid(),currentSlot));
        }
        boolean champion=s.type.equals("trainer")&&s.locations.values().stream().anyMatch(l->TrainerMusicPolicy.isChampion(l.trainer()));
        Candidate chosen=champion?null:EntranceSelection.choose(candidates,s.last==null?null:s.last.pokemon());
        s.activeContexts=List.copyOf(contexts);s.entranceRule=chosen==null?"none":chosen.rule().id();
        String entrance=chosen==null?null:chosen.rule().sound();UUID pokemon=chosen==null?null:chosen.pokemon();
        if(Objects.equals(entrance,s.base)){entrance=null;pokemon=null;}
        int fade=chosen==null?4:chosen.rule().transitionTicks();
        if(!force&&s.last!=null&&Objects.equals(entrance,s.last.entrance())&&Objects.equals(pokemon,s.last.pokemon())&&Objects.equals(s.base,s.last.base()))return;
        MusicState state=new MusicState(1,battle.getBattleId(),++s.revision,s.base,s.fallback,s.volume,s.pitch,entrance,pokemon,fade,false,s.type.equals("trainer"));
        s.last=state;ServerPlayNetworking.send(s.viewer.getEntity(),new MusicPayload(state.json()));
        BattleMusicMod.LOG.debug("Battle {} player {} base {} ({}) entrance {}",battle.getBattleId(),s.viewer.getUuid(),s.base,s.baseRule,entrance);
    }
    private static void sendEnd(PokemonBattle battle,Session s){if(s.viewer.getEntity()!=null)ServerPlayNetworking.send(s.viewer.getEntity(),new MusicPayload(new MusicState(1,battle.getBattleId(),++s.revision,s.base,s.fallback,s.volume,s.pitch,null,null,60,true).json()));}
    public static void end(PokemonBattle battle){PENDING.remove(battle);var sessions=ACTIVE.remove(battle.getBattleId());if(sessions!=null)sessions.values().forEach(s->sendEnd(battle,s));}
    public static String explain(UUID player){for(var sessions:ACTIVE.values()){var s=sessions.get(player);if(s!=null)return "Rule revision "+s.rules.revision()+"; base rule "+s.baseRule+"; entrance rule "+s.entranceRule+"; state "+(s.last==null?"pending":s.last.json())+"; active opposing context "+s.activeContexts;}return "No managed battle. Loaded rule revision "+BattleMusicMod.rules.revision()+" ("+BattleMusicMod.rules.rules().size()+" rules).";}
    public static void clear(){PENDING.clear();ACTIVE.clear();}
}
