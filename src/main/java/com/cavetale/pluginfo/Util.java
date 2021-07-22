package com.cavetale.pluginfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class Util {
    private Util() { }

    public static Map<String, Set<String>> buildDependedGraph() {
        Map<String, Set<String>> dependedGraph = new HashMap<>();
        for (Plugin it : Bukkit.getPluginManager().getPlugins()) {
            for (String depend : it.getDescription().getDepend()) {
                dependedGraph.computeIfAbsent(depend, d -> new HashSet<>()).add(it.getName());
            }
            for (String depend : it.getDescription().getSoftDepend()) {
                dependedGraph.computeIfAbsent(depend, d -> new HashSet<>()).add(it.getName());
            }
        }
        return dependedGraph;
    }

    /**
     * Find all plugins depending on name.
     * @param name the name of the depended on plugin
     * @param dependedGraph the graph returned by
     * buildDependedGraph(). It will be modified!
     * @return a list of names starting with the plugin with the least
     * dependencies (the first to reload), ending with the given
     * plugin.
     */
    public static List<String> findPluginDependencies(String name, Map<String, Set<String>> dependedGraph) {
        Set<String> todo = new HashSet<>();
        Set<String> done = new HashSet<>();
        List<String> order = new ArrayList<>();
        todo.add(name);
        done.add(name);
        while (!todo.isEmpty()) {
            Set<String> next = new HashSet<>();
            List<String> doRemove = new ArrayList<>();
            List<String> doAdd = new ArrayList<>();
            for (String it : todo) {
                Set<String> dependedSet = dependedGraph.get(it);
                if (dependedSet == null || dependedSet.isEmpty()) {
                    order.add(it);
                    doRemove.add(it);
                } else {
                    for (String depended : dependedSet) {
                        if (!done.contains(depended)) {
                            done.add(depended);
                            doAdd.add(depended);
                        }
                    }
                }
            }
            for (Set<String> depended : dependedGraph.values()) {
                depended.removeAll(doRemove);
            }
            todo.removeAll(doRemove);
            todo.addAll(doAdd);
        }
        return order;
    }
}
