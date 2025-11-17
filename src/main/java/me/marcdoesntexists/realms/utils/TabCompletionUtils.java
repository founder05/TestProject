package me.marcdoesntexists.realms.utils;

import me.marcdoesntexists.realms.managers.SocietiesManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class TabCompletionUtils {

    private TabCompletionUtils() {
    }

    public static List<String> match(Collection<String> candidates, String partial) {
        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();
        String p = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public static List<String> matchDistinct(Collection<String> candidates, String partial) {
        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();
        LinkedHashSet<String> set = new LinkedHashSet<>(candidates);
        return match(set, partial);
    }

    public static List<String> onlinePlayers(String partial) {
        String p = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(Objects::nonNull)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(p))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public static List<String> onlinePlayersFilter(String partial, java.util.function.Predicate<String> include) {
        String p = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(Objects::nonNull)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(p))
                .filter(n -> include == null || include.test(n))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public static List<String> towns(SocietiesManager sm, String partial) {
        if (sm == null) return Collections.emptyList();
        return matchDistinct(sm.getAllTowns().stream().map(t -> t.getName()).collect(Collectors.toList()), partial);
    }

    public static List<String> kingdoms(SocietiesManager sm, String partial) {
        if (sm == null) return Collections.emptyList();
        return matchDistinct(sm.getAllKingdoms().stream().map(k -> k.getName()).collect(Collectors.toList()), partial);
    }

    public static List<String> empires(SocietiesManager sm, String partial) {
        if (sm == null) return Collections.emptyList();
        return matchDistinct(sm.getAllEmpires().stream().map(e -> e.getName()).collect(Collectors.toList()), partial);
    }

}
