package com.votre_nom.ManagerCMD;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import spark.Spark;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Main extends JavaPlugin {

    private final List<Player> blueTeam = new ArrayList<>();
    private final List<Player> redTeam = new ArrayList<>();
    private final Random random = new Random();

    @Override
    public void onEnable() {
        getLogger().info("ManagerCMD est activé !");
        startApiServer();

        // Listener pour les événements
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerChat(AsyncPlayerChatEvent event) {
                Player player = event.getPlayer();
                String teamPrefix = "";

                if (blueTeam.contains(player)) {
                    teamPrefix = "§9[BLEU] ";
                } else if (redTeam.contains(player)) {
                    teamPrefix = "§c[ROUGE] ";
                }

                // Format stylé du message
                event.setFormat(teamPrefix + "§f" + player.getName() + " : " + event.getMessage());
            }

            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                Player player = event.getPlayer();
                if (player.getName().equalsIgnoreCase("magyke")) {
                    Bukkit.getScheduler().runTaskTimer(Main.this, () -> {
                        String[] colors = {"§c", "§6", "§e", "§a", "§b", "§d"};
                        String randomColor = colors[random.nextInt(colors.length)];
                        player.setDisplayName(randomColor + "LeProf §l✨🔥✨");
                        player.setPlayerListName(randomColor + "LeProf §l✨🔥✨");
                    }, 0L, 20L); // Change toutes les secondes
                }
            }
        }, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("ManagerCMD est désactivé !");
        try {
            Spark.stop();
            Thread.sleep(3000); // Laisser le temps à Jetty de s'arrêter correctement
        } catch (Exception e) {
            getLogger().warning("Erreur lors de l'arrêt de Spark : " + e.getMessage());
        }
    }

    private void startApiServer() {
        Spark.port(4567); // Démarre le serveur HTTP sur le port 4567

        // Configuration des CORS
        Spark.options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        Spark.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });

        Spark.post("/api/give", (req, res) -> {
            res.type("application/json");

            // Récupérer les paramètres
            String playerName = req.queryParams("player");
            String itemName = req.queryParams("item");

            if (playerName == null || itemName == null) {
                res.status(400);
                return "{ \"error\": \"Missing 'player' or 'item' parameter\" }";
            }

            // Trouver le joueur
            Player player = Bukkit.getPlayer(playerName);
            if (player == null) {
                res.status(404);
                return "{ \"error\": \"Player not found\" }";
            }

            // Créer l'item
            Material material;
            try {
                material = Material.valueOf(itemName.toUpperCase());
            } catch (IllegalArgumentException e) {
                res.status(400);
                return "{ \"error\": \"Invalid item name\" }";
            }

            player.getInventory().addItem(new ItemStack(material, 1));
            return "{ \"success\": \"" + itemName + " given to " + playerName + "\" }";
        });

        Spark.get("/api/teams/players", (req, res) -> {
            res.type("application/json");
            return "{ \"blue\": " + blueTeam.stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()) +
                    ", \"red\": " + redTeam.stream()
                            .map(Player::getName)
                            .collect(Collectors.toList())
                    + " }";
        });

        // Endpoint pour les informations des équipes
        Spark.get("/api/teams", (req, res) -> {
            res.type("application/json");

            // Créer l'objet JSON pour l'équipe bleue
            JsonObject blueTeamJson = new JsonObject();
            blueTeamJson.addProperty("count", blueTeam.size());
            JsonArray bluePlayersArray = new JsonArray();
            blueTeam.forEach(player -> bluePlayersArray.add(player.getName()));
            blueTeamJson.add("players", bluePlayersArray);

            // Créer l'objet JSON pour l'équipe rouge
            JsonObject redTeamJson = new JsonObject();
            redTeamJson.addProperty("count", redTeam.size());
            JsonArray redPlayersArray = new JsonArray();
            redTeam.forEach(player -> redPlayersArray.add(player.getName()));
            redTeamJson.add("players", redPlayersArray);

            // Créer l'objet JSON principal
            JsonObject teamsJson = new JsonObject();
            teamsJson.add("blue", blueTeamJson);
            teamsJson.add("red", redTeamJson);

            return teamsJson.toString(); // Retourne l'objet JSON sous forme de chaîne
        });

        getLogger().info("API HTTP démarrée sur le port 4567");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Seuls les joueurs peuvent exécuter cette commande.");
            return true;
        }

        Player player = (Player) sender;

        switch (label.toLowerCase()) {
            case "joinblue":
                if (redTeam.contains(player) || blueTeam.contains(player)) {
                    player.sendMessage("Vous êtes déjà dans une équipe et ne pouvez pas changer !");
                    return true;
                }
                blueTeam.add(player);
                player.sendMessage("§9Vous avez rejoint l'équipe bleue !");
                break;

            case "joinred":
                if (redTeam.contains(player) || blueTeam.contains(player)) {
                    player.sendMessage("Vous êtes déjà dans une équipe et ne pouvez pas changer !");
                    return true;
                }
                redTeam.add(player);
                player.sendMessage("§cVous avez rejoint l'équipe rouge !");
                break;

            case "teamcount":
                player.sendMessage("§9Équipe bleue : " + blueTeam.size() + " joueurs.");
                player.sendMessage("§cÉquipe rouge : " + redTeam.size() + " joueurs.");
                break;

            default:
                return false;
        }

        return true;
    }
}
