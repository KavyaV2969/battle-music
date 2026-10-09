package dev.battlemusic.client;

import com.cobblemon.mod.common.util.SoundExtensionsKt;
import dev.battlemusic.BattleMusicMod;
import dev.battlemusic.mixin.*;
import dev.battlemusic.net.MusicState;
import dev.battlemusic.rules.TrainerMusicPolicy;
import net.minecraft.*;
import java.util.*;

/** Retained streamed channels for the base and each opposing Pokemon/event pair. */
public final class Playback {
    public static final Playback INSTANCE=new Playback();
    private static final Set<class_3419> FILTERED=Set.of(class_3419.field_15253,class_3419.field_15247,class_3419.field_15256);
    private final LinkedHashSet<UUID> retired=new LinkedHashSet<>();
    private final List<ManagedSound> outgoing=new ArrayList<>();
    private record Voice(UUID pokemon,String sound){}
    private final Map<Voice,ManagedSound> retainedEntrances=new HashMap<>();
    private final Envelope backgroundGain=new Envelope(1);
    private boolean backgroundFading,trainerSession;
    private volatile MusicState state;private ManagedSound base,entrance;private boolean ending;private volatile boolean backgroundPaused;
    private int age,outroSilence;private UUID entrancePokemon;private long resourceRevision;
    private class_1144 manager(){return class_310.method_1551().method_1483();}
    public boolean owns(){return state!=null;}
    public boolean suppressBackground(){return backgroundPaused;}
    public boolean blocksBackgroundResume(class_3419 category){return suppressBackground()&&(category==null||FILTERED.contains(category));}
    public boolean filtered(class_1113 sound){return suppressBackground()&&!(sound instanceof ManagedSound)&&FILTERED.contains(sound.method_4774());}
    public boolean backgroundFading(){return backgroundFading;}
    public float backgroundGain(){return backgroundGain.value();}
    public float backgroundGain(class_1113 sound){return backgroundFading&&backgroundSound(sound)?Math.max(.0001f,backgroundGain.value()):1;}
    private boolean backgroundSound(class_1113 sound){return !(sound instanceof ManagedSound)&&FILTERED.contains(sound.method_4774());}
    public MusicState state(){return state;}
    public ManagedSound base(){return base;}
    public ManagedSound entrance(){return entrance;}
    public long resourceRevision(){return resourceRevision;}
    private boolean available(String event){
        if(event==null)return false;
        var set=manager().method_4869(class_2960.method_60654(event));
        return set!=null&&set.method_4894()>0;
    }
    private ManagedSound start(String event,float volume,float pitch,float initial){
        if(!available(event))return null;
        ManagedSound sound=new ManagedSound(event,volume,pitch,initial);
        sound.method_4783(manager());
        if(class_310.method_1551().method_1478().method_14486(sound.method_4776().method_4766()).isEmpty())return null;
        manager().method_4873(sound);return sound;
    }
    public void accept(MusicState next){
        if(retired.contains(next.battle()))return;
        if(state!=null&&state.battle().equals(next.battle())&&next.revision()<=state.revision())return;
        if(next.ended()){
            if(state!=null&&state.battle().equals(next.battle())){state=next;end();}
            else retire(next.battle());return;
        }
        boolean fresh=state==null||!state.battle().equals(next.battle());
        if(fresh){if(state!=null)retire(state.battle());reset(false);state=next;trainerSession=next.trainerBattle();pauseBackground();age=0;}
        else state=next;
        if(base==null||!base.method_4775().toString().equals(next.base())&&!base.method_4775().toString().equals(next.fallback())){
            boolean openingEntrance=fresh&&desiredEntrance(next)!=null;
            ManagedSound replacement=start(next.base(),next.volume(),next.pitch(),openingEntrance?0:1);
            if(replacement==null){BattleMusicMod.LOG.warn("Missing base music {}; falling back to {}",next.base(),next.fallback());replacement=start(next.fallback(),next.volume(),next.pitch(),1);}
            if(replacement==null){BattleMusicMod.LOG.warn("No playable base music for battle {}",next.battle());reset();return;}
            if(base!=null){resume(base);base.gain.to(0,next.transitionTicks());outgoing.add(base);}
            base=replacement;
        }
        chooseEntrance(next,fresh);
    }
    private String desiredEntrance(MusicState next){
        String desired=TrainerMusicPolicy.entranceAllowed(next.trainerBattle(),ClientSettings.trainerEntrances())?next.entrance():null;
        return desired!=null&&base!=null&&desired.equals(base.method_4775().toString())?null:desired;
    }
    private void chooseEntrance(MusicState next){chooseEntrance(next,false);}
    private void chooseEntrance(MusicState next,boolean opening){
        String desired=desiredEntrance(next);
        boolean same=entrance!=null&&Objects.equals(desired,entrance.method_4775().toString())&&Objects.equals(next.pokemon(),entrancePokemon);
        if(same)return;
        if(entrance!=null){entrance.gain.to(0,next.transitionTicks());if(!outgoing.contains(entrance))outgoing.add(entrance);entrance=null;}
        entrancePokemon=null;
        if(desired!=null){
            Voice key=new Voice(next.pokemon(),desired);
            entrance=retainedEntrances.get(key);
            if(entrance!=null&&entrance.paused&&channel(entrance)==null){stop(entrance);retainedEntrances.remove(key);entrance=null;}
            if(entrance==null){entrance=start(desired,1,1,opening?1:0);if(entrance!=null)retainedEntrances.put(key,entrance);}
            if(entrance==null)BattleMusicMod.LOG.warn("Missing entrance music {}; retaining base",desired);
            else{
                outgoing.remove(entrance);resume(entrance);entrancePokemon=next.pokemon();
                if(opening)entrance.gain.snap(1);else entrance.gain.to(1,next.transitionTicks());
            }
        }
        if(entrance==null){resume(base);if(opening)base.gain.snap(1);else base.gain.to(1,next.transitionTicks());}
        else if(opening)base.gain.snap(0);else base.gain.to(0,next.transitionTicks());
        // Apply a leading theme's full gain before the sound executor starts its channel.
        if(opening){base.advance();if(entrance!=null)entrance.advance();}
    }
    private void pauseBackground(){
        backgroundFading=false;backgroundGain.snap(1);
        backgroundPaused=true;
        for(var category:FILTERED)SoundExtensionsKt.pauseSounds(manager(),null,category);
        applyBackgroundVolume();
    }
    public void refreshSettings(){if(state!=null&&base!=null&&!ending)chooseEntrance(state);}
    private void restoreBackground(boolean fade){
        if(!backgroundPaused&&!backgroundFading)return;
        boolean resume=backgroundPaused;backgroundPaused=false;backgroundFading=fade;
        backgroundGain.snap(fade?0:1);if(fade)backgroundGain.to(1,60);
        // Queue attenuation before resuming so retained BGM cannot jump to full gain.
        applyBackgroundVolume();
        if(resume)for(var category:FILTERED)SoundExtensionsKt.resumeSounds(manager(),null,category);
    }
    private void applyBackgroundVolume(){
        var engine=(SoundSystemAccessor)((SoundManagerAccessor)manager()).battlemusic$engine();
        engine.battlemusic$channels().forEach((sound,channel)->{
            if(backgroundSound(sound)){
                float gain=engine.battlemusic$volume(sound);
                channel.method_19735(source->source.method_19647(gain));
            }
        });
    }
    private class_4235.class_4236 channel(ManagedSound sound){
        return ((SoundSystemAccessor)((SoundManagerAccessor)manager()).battlemusic$engine()).battlemusic$channels().get(sound);
    }
    private void pause(ManagedSound sound){if(sound==null)return;var channel=channel(sound);if(channel!=null){channel.method_19735(class_4224::method_19653);sound.paused=true;}}
    private void resume(ManagedSound sound){if(sound==null||!sound.paused)return;var channel=channel(sound);if(channel!=null)channel.method_19735(class_4224::method_19654);sound.paused=false;}
    public void tick(){
        if(backgroundFading){backgroundGain.tick();applyBackgroundVolume();if(backgroundGain.settled())backgroundFading=false;}
        if(state==null)return;age++;
        // A muted category or audio-device reset can destroy channels. Allow asynchronous
        // streaming startup time, then recover from the last authoritative selection.
        if(!ending&&age>40&&class_310.method_1551().field_1690.method_1630(class_3419.field_15253)>0
            &&(base!=null&&channel(base)==null||entrance!=null&&channel(entrance)==null)){
            MusicState latest=state;reset(false);accept(latest);return;
        }
        if(base!=null){base.advance();if(!ending&&base.gain.settled()&&base.gain.value()==0){pause(base);base.pausedTicks++;}else if(!base.paused)resumeChannel(base);}
        if(entrance!=null){entrance.advance();resumeChannel(entrance);}
        for(var it=outgoing.iterator();it.hasNext();){
            var sound=it.next();sound.advance();
            if(sound.gain.settled()){
                if(!ending&&retainedEntrances.containsValue(sound))pause(sound);else stop(sound);
                it.remove();
            }
        }
        if(ending&&(base==null||base.gain.settled())&&(entrance==null||entrance.gain.settled())&&outgoing.isEmpty()){
            // A short quiet gap separates the final battle tail from environment BGM.
            if(++outroSilence>=10){UUID done=state.battle();boolean fade=trainerSession;reset(false);restoreBackground(fade);retire(done);}
        }
    }
    private void resumeChannel(ManagedSound sound){var channel=channel(sound);if(channel!=null)channel.method_19735(class_4224::method_19654);}
    public void end(){
        if(state==null||ending)return;ending=true;outroSilence=0;retire(state.battle());
        if(base!=null){if(base.paused){stop(base);base.gain.snap(0);}else base.gain.to(0,60);}
        if(entrance!=null)entrance.gain.to(0,60);
        outgoing.forEach(s->s.gain.to(0,60));
        for(var voice:retainedEntrances.values())if(voice!=entrance&&!outgoing.contains(voice))stop(voice);
    }
    private void retire(UUID id){retired.add(id);if(retired.size()>128)retired.remove(retired.iterator().next());}
    private void stop(ManagedSound sound){if(sound!=null){sound.finish();manager().method_4870(sound);}}
    public void reset(){reset(true);}
    private void reset(boolean releaseBackground){
        stop(base);stop(entrance);outgoing.forEach(this::stop);retainedEntrances.values().forEach(this::stop);
        outgoing.clear();retainedEntrances.clear();base=entrance=null;entrancePokemon=null;state=null;ending=false;outroSilence=0;
        if(releaseBackground)restoreBackground(false);
    }
    public void disconnect(){reset();retired.clear();}
    public void resourcesReloaded(){
        resourceRevision++;MusicState previous=state;
        if(previous!=null&&!ending){reset(false);accept(previous);}
    }
    public String diagnostics(){return "battle="+(state==null?null:state.battle())+" base="+(base==null?null:base.method_4775())+" basePaused="+(base!=null&&base.paused)+" entrance="+(entrance==null?null:entrance.method_4775())+" voices="+outgoing.size()+" age="+age;}
}
