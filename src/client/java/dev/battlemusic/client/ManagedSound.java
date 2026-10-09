package dev.battlemusic.client;

import net.minecraft.*;

public final class ManagedSound extends class_1101 {
    public final Envelope gain;public final float nominalVolume;public boolean paused;public int pausedTicks;
    private class_1146 resolved;
    public ManagedSound(String event,float volume,float pitch,float initial){
        super(class_3414.method_47908(class_2960.method_60654(event)),class_3419.field_15253,class_1113.method_43221());
        nominalVolume=volume;gain=new Envelope(initial);field_5442=volume*initial;field_5441=pitch;
        field_5446=true;field_5451=0;field_18936=true;field_5440=class_1113.class_1114.field_5478;
    }
    // Gain is driven by the controller, once per client tick. The sound engine must retain a zero-gain paused channel.
    @Override public void method_16896(){}
    @Override public boolean method_4785(){return true;}
    @Override public class_1146 method_4783(class_1144 manager){if(resolved==null)resolved=super.method_4783(manager);return resolved;}
    public void advance(){field_5442=nominalVolume*gain.tick();}
    public void finish(){method_24876();}
}
