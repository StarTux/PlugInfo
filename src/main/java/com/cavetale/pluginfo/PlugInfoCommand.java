package com.cavetale.pluginfo;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredListener;

@RequiredArgsConstructor
public final class PlugInfoCommand implements TabExecutor {
    private final PlugInfoPlugin pluginfo;
    private Map<String, Subcommand> commandMap = new LinkedHashMap<>();

    public void enable() {
        registerCommand("list", this::list);
        registerCommand("dump", this::dump, this::completePlugins);
        registerCommand("depend", this::depend, this::completePlugins);
        registerCommand("author", this::author);
        registerCommand("nauthor", this::nauthor);
        registerCommand("listen", this::listen);
        registerCommand("permission", this::permission);
        registerCommand("command", this::command);
        registerCommand("reload", this::reload, this::completePlugins);
        registerCommand("graph", this::graph);
        registerCommand("synccommands", this::syncCommands);
        registerCommand("api", this::api);
        pluginfo.getCommand("pluginfo").setExecutor(this);
    }

    @RequiredArgsConstructor
    private static final class Subcommand {
        protected final BiFunction<CommandSender, String[], Boolean> call;
        protected final Function<String, List<String>>[] completers;
    }

    private void registerCommand(final String name,
                                 final BiFunction<CommandSender, String[], Boolean> call,
                                 final Function<String, List<String>>... completers) {
        commandMap.put(name, new Subcommand(call, completers));
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 0) return false;
        String cmd = args[0].toLowerCase();
        Subcommand subcommand = commandMap.get(cmd);
        if (subcommand == null) return false;
        return subcommand.call.apply(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            String arg = args[0];
            return commandMap.keySet().stream()
                .filter(s -> s.contains(arg))
                .collect(Collectors.toList());
        } else if (args.length > 1) {
            Subcommand subcommand = commandMap.get(args[0]);
            if (subcommand == null) return Collections.emptyList();
            int argIndex = args.length - 2;
            if (argIndex >= subcommand.completers.length) return Collections.emptyList();
            return subcommand.completers[argIndex].apply(args[args.length - 1]);
        }
        return null;
    }

    private List<String> completePlugins(String arg) {
        return Stream.of(Bukkit.getServer().getPluginManager().getPlugins())
            .map(Plugin::getName)
            .filter(name -> name.contains(arg))
            .collect(Collectors.toList());
    }

    protected boolean list(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage("Plugins loaded:");
        int count = 0;
        for (Plugin plugin: Bukkit.getServer().getPluginManager().getPlugins()) {
            sender.sendMessage("- " + plugin.getName());
            count += 1;
        }
        sender.sendMessage("Total " + count);
        return true;
    }

    protected boolean dump(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String arg = args[0];
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(arg);
        if (plugin == null) {
            sender.sendMessage("Plugin not found: " + arg);
            return true;
        }
        PluginDescriptionFile desc = plugin.getDescription();
        sender.sendMessage("Name: " + desc.getName());
        sender.sendMessage("Version: " + desc.getVersion());
        sender.sendMessage("Main: " + desc.getMain());
        sender.sendMessage("Authors: " + desc.getAuthors());
        sender.sendMessage("Description: " + desc.getDescription());
        sender.sendMessage("Website: " + desc.getWebsite());
        sender.sendMessage("Prefix: " + desc.getPrefix());
        sender.sendMessage("Load: " + desc.getLoad());
        sender.sendMessage("Depend: " + desc.getDepend());
        sender.sendMessage("SoftDepend: " + desc.getSoftDepend());
        sender.sendMessage("LoadBefore: " + desc.getLoadBefore());
        Map commands = desc.getCommands();
        sender.sendMessage("Commands: " + (commands == null ? "[]" : commands.keySet()));
        Collection<Permission> permissions = desc.getPermissions();
        sender.sendMessage("Permissions: " + (permissions == null ? "[]" : permissions.stream().map(a -> a.getName()).collect(Collectors.toList())));
        sender.sendMessage("PermissionDefault: " + desc.getPermissionDefault());
        sender.sendMessage("Awareness: " + desc.getAwareness());
        return true;
    }

    protected boolean depend(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String arg = args[0];
        Plugin plugin = Bukkit.getPluginManager().getPlugin(arg);
        if (plugin == null) {
            sender.sendMessage("Plugin not found: " + arg);
            return true;
        }
        Map<String, Set<String>> dependedGraph = Util.buildDependedGraph(true);
        Set<String> directlyDepending = new HashSet<>(dependedGraph.computeIfAbsent(arg, d -> new HashSet<>()));
        List<String> dependencies = Util.findPluginDependencies(plugin.getName(), dependedGraph);
        List<String> all = dependencies.subList(0, dependencies.size() - 1);
        Collections.reverse(all);
        sender.sendMessage(directlyDepending.size() + " directly depending: " + directlyDepending);
        sender.sendMessage((dependencies.size() - 1) + " depending: " + all);
        return true;
    }

    protected boolean author(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String arg = args[0];
        sender.sendMessage("Plugins being (co)authored by " + arg + ":");
        int count = 0;
        for (Plugin plugin: Bukkit.getServer().getPluginManager().getPlugins()) {
            if (plugin.getDescription().getAuthors().contains(arg)) {
                sender.sendMessage("- " + plugin.getName());
                count += 1;
            }
        }
        sender.sendMessage("Total " + count);
        return true;
    }

    protected boolean nauthor(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String arg = args[0];
        sender.sendMessage("Plugins not being (co)authored by " + arg + ":");
        int count = 0;
        for (Plugin plugin: Bukkit.getServer().getPluginManager().getPlugins()) {
            if (!plugin.getDescription().getAuthors().contains(arg)) {
                sender.sendMessage("- " + plugin.getName());
                count += 1;
            }
        }
        sender.sendMessage("Total " + count);
        return true;
    }

    protected boolean listen(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String arg = args[0];
        HandlerList handlers;
        try {
            Class clazz = Class.forName(arg);
            @SuppressWarnings("unchecked")
                HandlerList tmp = (HandlerList) clazz.getMethod("getHandlerList").invoke(null);
            handlers = tmp;
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage("Event class not found: " + arg + ". See console.");
            return true;
        }
        sender.sendMessage("Plugins listening to " + arg + ":");
        int count = 0;
        for (RegisteredListener listener: handlers.getRegisteredListeners()) {
            sender.sendMessage("- " + listener.getPlugin().getName() + " (" + listener.getPriority() + ")");
            count += 1;
        }
        sender.sendMessage("Total " + count);
        return true;
    }

    protected boolean permission(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String arg = args[0];
        Permission perm = Bukkit.getServer().getPluginManager().getPermission(arg);
        if (perm == null) {
            sender.sendMessage("Permission not found: " + arg);
            return true;
        }
        sender.sendMessage("Name: " + perm.getName());
        sender.sendMessage("Default: " + perm.getDefault());
        sender.sendMessage("Description: " + perm.getDescription());
        sender.sendMessage("Children:");
        for (Map.Entry<String, Boolean> child: perm.getChildren().entrySet()) {
            sender.sendMessage("- " + child.getKey() + ": " + child.getValue());
        }
        Set ps = perm.getPermissibles();
        sender.sendMessage("Permissibles: " + (ps == null ? 0 : ps.size()));
        return true;
    }

    protected boolean command(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String arg = args[0];
        PluginCommand pcmd = Bukkit.getServer().getPluginCommand(arg);
        if (pcmd == null) {
            sender.sendMessage("Plugin command not found: " + arg);
            return true;
        }
        sender.sendMessage("Name: " + pcmd.getName());
        sender.sendMessage("Plugin: " + pcmd.getPlugin().getName());
        sender.sendMessage("Aliases: " + pcmd.getAliases());
        sender.sendMessage("Description: " + pcmd.getDescription());
        sender.sendMessage("Label: " + pcmd.getLabel());
        sender.sendMessage("Permission: " + pcmd.getPermission());
        sender.sendMessage("Usage: " + pcmd.getUsage());
        sender.sendMessage("Registered: " + pcmd.isRegistered());
        return true;
    }

    protected boolean reload(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String arg = args[0];
        Plugin plugin = Bukkit.getPluginManager().getPlugin(arg);
        if (plugin == null) {
            sender.sendMessage("Plugin not found: " + arg);
            return true;
        }
        List<String> order = Util.findPluginDependencies(plugin.getName(), Util.buildDependedGraph(true));
        sender.sendMessage("Plugin disable order: " + order);
        for (String name : order) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "plugman unload " + name);
        }
        for (int i = order.size() - 1; i >= 0; i -= 1) {
            String name = order.get(i);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "plugman load " + name);
        }
        syncCommands();
        return true;
    }

    protected boolean graph(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        Map<String, Set<String>> dependedGraph = Util.buildDependedGraph(false);
        List<String> lines = new ArrayList<>();
        lines.add("digraph {");
        List<String> names = new ArrayList<>(dependedGraph.keySet());
        Collections.sort(names, (a, b) -> Integer.compare(dependedGraph.get(b).size(),
                                                          dependedGraph.get(a).size()));
        for (String name : names) {
            for (String dependedBy : dependedGraph.get(name)) {
                lines.add(dependedBy + " -> " + name);
            }
        }
        lines.add("}");
        String fn = "PlugInfoGraph.txt";
        try (PrintWriter out = new PrintWriter(fn)) {
            out.print(String.join("\n", lines));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            sender.sendMessage("IOException! See console");
            return true;
        }
        sender.sendMessage("Graph written to " + fn);
        return true;
    }

    protected boolean syncCommands(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        syncCommands();
        sender.sendMessage("Commands synchronized");
        return true;
    }

    private static void syncCommands() {
        try {
            Bukkit.getServer().getClass().getMethod("syncCommands").invoke(Bukkit.getServer());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean api(CommandSender sender, String[] args) {
        int count = 0;
        for (Plugin plugin: Bukkit.getServer().getPluginManager().getPlugins()) {
            String apiVersion = plugin.getDescription().getAPIVersion();
            if (!"1.17".equals(apiVersion)) {
                sender.sendMessage("- " + plugin.getName() + ": " + apiVersion);
                count += 1;
            }
        }
        sender.sendMessage("Total " + count);
        return true;
    }
}
