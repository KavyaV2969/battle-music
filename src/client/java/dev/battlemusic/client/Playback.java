package dev.battlemusic.client;

import com.cobblemon.mod.common.util.SoundExtensionsKt;
import dev.battlemusic.BattleMusicMod;
import dev.battlemusic.mixin.*;
import dev.battlemusic.net.MusicState;
import net.minecraft.*;
import java.util.*;

/** One retained base and transient entrance voices. No stopping/recreating the base to resume it. */
public final class Playback {
    public static final Playback INSTANCE=new Playback();
    private static final Set<class_3419> FILTERED=Set.of(class_3419.field_15253,class_3419.field_15247,class_3419.field_15256);
    private final LinkedHashSet<UUID> retired=new LinkedHashSet<>();
    private final List<ManagedSound> outgoing=new ArrayList<>();
    private MusicState state;private ManagedSound base,entrance;private boolean ending,backgroundPaused;
    private int age;private UUID entrancePokemon;private long resourceRevision;
    private class_1144 manager(){return class_310.method_1551().method_1483();}
    public boolean owns(){return state!=null;}
    public boolean suppressBackground(){return owns()&&backgroundPaused;}
    public boolean filtered(class_1113 sound){return suppressBackground()&&!(sound instanceof ManagedSound)&&FILTERED.contains(sound.method_4774());}
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
        if(fresh){if(state!=null)retire(state.battle());reset();state=next;pauseBackground();age=0;}
        else state=next;
        if(base==null||!base.method_4775().toString().equals(next.base())&&!base.method_4775().toString().equals(next.fallback())){
            ManagedSound replacement=start(next.base(),next.volume(),next.pitch(),1);
            if(replacement==null){BattleMusicMod.LOG.warn("Missing base music {}; falling back to {}",next.base(),next.fallback());replacement=start(next.fallback(),next.volume(),next.pitch(),1);}
            if(replacement==null){BattleMusicMod.LOG.warn("No playable base music for battle {}",next.battle());reset();return;}
            if(base!=null){resume(base);base.gain.to(0,next.transitionTicks());outgoing.add(base);}
            base=replacement;
        }
        chooseEntrance(next);
    }
    private void chooseEntrance(MusicState next){
        String desired=next.entrance();
        if(desired!=null&&desired.equals(base.method_4775().toString()))desired=null;
        boolean same=entrance!=null&&Objects.equals(desired,entrance.method_4775().toString())&&Objects.equals(next.pokemon(),entrancePokemon);
        if(same)return;
        if(entrance!=null){entrance.gain.to(0,next.transitionTicks());outgoing.add(entrance);entrance=null;}
        entrancePokemon=null;
        if(desired!=null){
            entrance=start(desired,1,1,0);
            if(entrance==null)BattleMusicMod.LOG.warn("Missing entrance music {}; retaining base",desired);
            else{entrancePokemon=next.pokemon();entrance.gain.to(1,next.transitionTicks());}
        }
        if(entrance==null){resume(base);base.gain.to(1,next.transitionTicks());}
        else base.gain.to(0,next.transitionTicks());
    }
    private void pauseBackground(){
        backgroundPaused=true;
        for(var category:FILTERED)SoundExtensionsKt.pauseSounds(manager(),null,category);
    }
    private void restoreBackground(){
        if(!backgroundPaused)return;backgroundPaused=false;
        for(var category:FILTERED)SoundExtensionsKt.resumeSounds(manager(),null,category);
    }
    private class_4235.class_4236 channel(ManagedSound sound){
        return ((SoundSystemAccessor)((SoundManagerAccessor)manager()).battlemusic$engine()).battlemusic$channels().get(sound);
    }
    private void pause(ManagedSound sound){if(sound==null)return;var channel=channel(sound);if(channel!=null){channel.method_19735(class_4224::method_19653);sound.paused=true;}}
    private void resume(ManagedSound sound){if(sound==null||!sound.paused)return;var channel=channel(sound);if(channel!=null)channel.method_19735(class_4224::method_19654);sound.paused=false;}
    public void tick(){
        if(state==null)return;age++;
        // A muted category or audio-device reset can destroy channels. Allow asynchronous
        // streaming startup time, then recover from the last authoritative selection.
        if(!ending&&age>40&&class_310.method_1551().field_1690.method_1630(class_3419.field_15253)>0
            &&(base!=null&&channel(base)==null||entrance!=null&&channel(entrance)==null)){
            MusicState latest=state;reset();accept(latest);return;
        }
        if(base!=null){base.advance();if(!ending&&base.gain.settled()&&base.gain.value()==0){pause(base);base.pausedTicks++;}else if(!base.paused)resumeChannel(base);}
        if(entrance!=null){entrance.advance();resumeChannel(entrance);}
        for(var it=outgoing.iterator();it.hasNext();){var sound=it.next();sound.advance();if(sound.gain.settled()){stop(sound);it.remove();}}
        if(ending&&(base==null||base.gain.settled())&&(entrance==null||entrance.gain.settled())){UUID done=state.battle();reset();retire(done);}
    }
    private void resumeChannel(ManagedSound sound){var channel=channel(sound);if(channel!=null)channel.method_19735(class_4224::method_19654);}
    public void end(){
        if(state==null||ending)return;ending=true;retire(state.battle());
        if(base!=null){if(base.paused)stop(base);base.gain.to(0,60);}
        if(entrance!=null)entrance.gain.to(0,60);
        outgoing.forEach(s->s.gain.to(0,60));
    }
    private void retire(UUID id){retired.add(id);if(retired.size()>128)retired.remove(retired.iterator().next());}
    private void stop(ManagedSound sound){if(sound!=null){sound.finish();manager().method_4870(sound);}}
    public void reset(){
        stop(base);stop(entrance);outgoing.forEach(this::stop);outgoing.clear();base=entrance=null;entrancePokemon=null;state=null;ending=false;restoreBackground();
    }
    public void disconnect(){reset();retired.clear();}
    public void resourcesReloaded(){
        resourceRevision++;MusicState previous=state;
        if(previous!=null&&!ending){reset();accept(previous);}
    }
    public String diagnostics(){return "battle="+(state==null?null:state.battle())+" base="+(base==null?null:base.method_4775())+" basePaused="+(base!=null&&base.paused)+" entrance="+(entrance==null?null:entrance.method_4775())+" voices="+outgoing.size()+" age="+age;}
}
