package dev.battlemusic.fixture;
import com.google.gson.*;
import java.util.*;
public final class Shared {
    public static volatile String expect="",ack="",error;
    public static volatile boolean championArceusActive;
    public static volatile long recallStartedNanos,entrySentNanos;
    public static final List<String> checks=Collections.synchronizedList(new ArrayList<>());
    public static final JsonObject measurements=new JsonObject();
    public static void request(String expectation){ack="";expect=expectation;}
}
