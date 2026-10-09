package dev.battlemusic.rules;

import java.util.*;

/** Plain immutable values: no game classes, safe to validate and test offline. */
public record Context(String battleType, String species, String form, Set<String> aspects,
                      Set<String> labels, String trainer, UUID pokemon, String encounter,
                      String dimension, String biome, Set<String> biomeTags, double x, double y, double z) {
    public Context { aspects=Set.copyOf(aspects); labels=Set.copyOf(labels); biomeTags=Set.copyOf(biomeTags); }
    public static Set<String> labels(Collection<String> species, Collection<String> form) {
        var result=new HashSet<String>(species); result.addAll(form);
        result.removeIf(s -> s.matches("gen[1-9][a-z]?"));
        var generations=form.stream().filter(s -> s.matches("gen[1-9][a-z]?")).toList();
        if(generations.isEmpty()) generations=species.stream().filter(s -> s.matches("gen[1-9][a-z]?")).toList();
        generations.forEach(s -> result.add(s.substring(0,4)));
        return Set.copyOf(result);
    }
}
