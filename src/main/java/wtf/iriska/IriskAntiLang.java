package wtf.iriska;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Плагин для обнаружения и блокировки слов со смешанными алфавитами
 * Автор: Iriska
 * Версия: 1.2
 */
public class IriskAntiLang extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private String warningMessage;
    private boolean cancelMessage;
    private Set<String> bypassPermissions;

    @Override
    public void onEnable() {
        // Загрузка конфигурации
        saveDefaultConfig();
        reloadConfiguration();

        // Регистрация событий
        getServer().getPluginManager().registerEvents(this, this);

        // Регистрация команды для перезагрузки конфига
        getCommand("iriskantilang").setExecutor((sender, command, label, args) -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("iriskantilang.reload")) {
                    reloadConfiguration();
                    sender.sendMessage(ChatColor.GREEN + "Конфигурация IriskAntiLang перезагружена!");
                    return true;
                }
            }
            return false;
        });

        getLogger().log(Level.INFO, "Плагин IriskAntiLang успешно включен (v{0})", getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Плагин IriskAntiLang выключен");
    }

    private void reloadConfiguration() {
        reloadConfig();
        config = getConfig();
        
        // Установка значений по умолчанию, если их нет в конфиге
        config.addDefault("warning-message", "&cВаше сообщение кажется подозрительным!");
        config.addDefault("cancel-message", true);
        config.addDefault("bypass-permissions", new String[]{"iriskantilang.bypass"});
        
        warningMessage = ChatColor.translateAlternateColorCodes('&', config.getString("warning-message"));
        cancelMessage = config.getBoolean("cancel-message");
        
        bypassPermissions = new HashSet<>();
        for (String perm : config.getStringList("bypass-permissions")) {
            bypassPermissions.add(perm.toLowerCase());
        }
        
        saveConfig();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Проверка прав на обход фильтра
        if (canBypass(player)) {
            return;
        }

        // Разбиваем сообщение на слова и проверяем каждое
        for (String word : message.split("\\s+")) {
            if (hasMixedAlphabets(word)) {
                player.sendMessage(warningMessage);
                
                if (cancelMessage) {
                    event.setCancelled(true);
                    logViolation(player, word);
                }
                break; // Прекращаем проверку после первого нарушения
            }
        }
    }

    private boolean canBypass(Player player) {
        for (String perm : bypassPermissions) {
            if (player.hasPermission(perm)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMixedAlphabets(String word) {
        boolean hasCyrillic = false;
        boolean hasLatin = false;

        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            
            // Пропускаем не-буквенные символы
            if (!Character.isLetter(c)) {
                continue;
            }

            if (isCyrillic(c)) {
                hasCyrillic = true;
            } else if (isLatin(c)) {
                hasLatin = true;
            }

            // Если найдены оба алфавита в одном слове
            if (hasCyrillic && hasLatin) {
                return true;
            }
        }

        return false;
    }

    private boolean isCyrillic(char c) {
        // Основной диапазон кириллицы (включая Ёё)
        return (c >= 'А' && c <= 'я') || c == 'Ё' || c == 'ё';
    }

    private boolean isLatin(char c) {
        // Латинские буквы (A-Z, a-z)
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private void logViolation(Player player, String word) {
        getLogger().log(Level.WARNING, "Игрок {0} использовал смешение алфавитов в слове: {1}",
                new Object[]{player.getName(), word});
    }
}
