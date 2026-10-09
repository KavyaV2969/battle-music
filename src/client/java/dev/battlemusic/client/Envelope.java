package dev.battlemusic.client;

/** Retargetable smoothstep envelope. A rapid change begins at the current gain. */
public final class Envelope {
    private float value,start,target;private int duration,elapsed;
    public Envelope(float initial){value=start=target=initial;}
    public void to(float gain,int ticks){if(target==gain)return;start=value;target=gain;duration=Math.max(1,ticks);elapsed=0;}
    public float tick(){if(elapsed<duration){float t=(float)++elapsed/duration;t=t*t*(3-2*t);value=start+(target-start)*t;}return value;}
    public float value(){return value;}
    public float target(){return target;}
    public boolean settled(){return elapsed>=duration;}
}
