package com.nebulaclient.mod.client.server;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.minecraft.client.network.ServerInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Decides what order servers appear in: favourites first, then the
 * player's own dragged order, then anything new at the end.
 *
 * Everything is keyed by server address rather than list index, so the
 * ordering survives the player adding, editing or deleting entries in
 * vanilla's own server list.
 */
public final class ServerOrdering {
    private ServerOrdering() {}

    /** Sorted view of the given servers: favourites on top, then saved drag order. */
    public static List<ServerInfo> sorted(List<ServerInfo> servers) {
        NebulaConfig cfg = NebulaConfig.get();
        List<ServerInfo> out = new ArrayList<>(servers);
        out.sort(Comparator
                // favourites first (false sorts before true, so negate)
                .comparing((ServerInfo s) -> !cfg.isFavoriteServer(s.address))
                // then the player's hand-dragged order; unknown entries go last
                .thenComparingInt(s -> {
                    int i = cfg.serverOrder.indexOf(s.address);
                    return i < 0 ? Integer.MAX_VALUE : i;
                }));
        return out;
    }

    /** Sorted view, optionally filtered to favourites only. */
    public static List<ServerInfo> visible(List<ServerInfo> servers, boolean favoritesOnly) {
        List<ServerInfo> ordered = sorted(servers);
        if (!favoritesOnly) return ordered;
        NebulaConfig cfg = NebulaConfig.get();
        List<ServerInfo> filtered = new ArrayList<>();
        for (ServerInfo s : ordered) if (cfg.isFavoriteServer(s.address)) filtered.add(s);
        return filtered;
    }

    /**
     * Records a drag: moves {@code moved} to sit at {@code newIndex} within
     * the full ordered list, then saves the resulting address order.
     */
    public static void applyDrag(List<ServerInfo> allServers, ServerInfo moved, int newIndex) {
        List<ServerInfo> ordered = sorted(allServers);
        ordered.remove(moved);
        newIndex = Math.max(0, Math.min(newIndex, ordered.size()));
        ordered.add(newIndex, moved);

        NebulaConfig cfg = NebulaConfig.get();
        cfg.serverOrder.clear();
        for (ServerInfo s : ordered) cfg.serverOrder.add(s.address);
        cfg.save();
    }
}
