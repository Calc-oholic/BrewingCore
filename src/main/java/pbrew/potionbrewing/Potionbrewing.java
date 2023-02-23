package pbrew.potionbrewing;

import java.util.*;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Potionbrewing extends JavaPlugin implements Listener {
    private Map<List<Material>, List<ItemStack>> recipes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        recipes = loadRecipesFromConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        recipes.clear();
    }

    private Map<List<Material>, List<ItemStack>> loadRecipesFromConfig() {
        Map<List<Material>, List<ItemStack>> recipeMap = new HashMap<>();

        for (String key : getConfig().getKeys(false)) {
            List<Material> inputMaterials = new ArrayList<>();
            System.out.println("Key: " + key);

            for (String inputKey : getConfig().getStringList(key + ".input")) {
                inputMaterials.add(Material.getMaterial(inputKey));
                System.out.println("  Input: " + inputKey);
            }

            List<ItemStack> outputItems = new ArrayList<>();
            ConfigurationSection outputSection = getConfig().getConfigurationSection(key + ".output");

            if (outputSection != null) {
                for (String outputKey : outputSection.getKeys(false)) {
                    String[] parts = outputKey.split(":");
                    Material material = Material.getMaterial(parts[0]);
                    int amount = Integer.parseInt(parts[1]);
                    ItemStack outputItem = new ItemStack(material, amount);

                    if (parts.length > 2) {
                        ItemMeta meta = outputItem.getItemMeta();
                        meta.setLore(Collections.singletonList(parts[2]));
                        outputItem.setItemMeta(meta);
                    }

                    outputItems.add(outputItem);
                    System.out.println("  Output: " + outputItems);
                }
            }

            recipeMap.put(inputMaterials, outputItems);
        }

        return recipeMap;
    }

    public void reloadConfigFile() {
        reloadConfig();
        recipes = loadRecipesFromConfig();
    }

    long cooldown = 60;
    Map<String, Long> lastUsedTimes = new HashMap<>();

    @EventHandler
    public void onBrewingStandInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.BREWING_STAND && event.getPlayer().isSneaking() && event.getPlayer().hasPermission("pbrew.potions")) {
            String playerName = event.getPlayer().getName();
            if (lastUsedTimes.containsKey(playerName)) {
                long lastUsedTime = lastUsedTimes.get(playerName);
                long currentTime = System.currentTimeMillis() / 1000;
                if (lastUsedTime + cooldown > currentTime) {
                    return;
                }
            }
            lastUsedTimes.put(playerName, System.currentTimeMillis() / 1000);
            event.setCancelled(true);
            Inventory brewingStandInventory = Bukkit.createInventory(null, InventoryType.BREWING);
            ItemStack potion1 = new ItemStack(Material.LINGERING_POTION);
            PotionMeta meta = (PotionMeta) potion1.getItemMeta();
            meta.setDisplayName(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_PURPLE + "Chemistry" + ChatColor.DARK_GRAY + "] " + ChatColor.DARK_PURPLE + "Hydrochloric Acid");
            meta.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 160, 1), true);
            meta.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 160, 1), true);
            meta.addCustomEffect(new PotionEffect(PotionEffectType.WEAKNESS, 160, 0), true);
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            meta.setColor(Color.fromRGB(126, 178, 202));
            potion1.setItemMeta(meta);

            ItemStack potion2 = new ItemStack(Material.SPLASH_POTION);
            meta = (PotionMeta) potion2.getItemMeta();
            meta.setDisplayName(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_PURPLE + "Chemistry" + ChatColor.DARK_GRAY + "] " + ChatColor.DARK_PURPLE + "Blue Powder");
            meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 400, 0), true);
            meta.addCustomEffect(new PotionEffect(PotionEffectType.JUMP, 400, 0), true);
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            meta.setColor(Color.fromRGB(32, 32, 164));
            potion2.setItemMeta(meta);

            ItemStack potion3 = new ItemStack(Material.LINGERING_POTION);
            meta = (PotionMeta) potion3.getItemMeta();
            meta.setDisplayName(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_PURPLE + "Chemistry" + ChatColor.DARK_GRAY + "] " + ChatColor.DARK_PURPLE + "Helium");
            meta.addCustomEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 0), true);
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            meta.setColor(Color.fromRGB(255, 0, 0));
            potion3.setItemMeta(meta);

            brewingStandInventory.setItem(0, potion1);
            brewingStandInventory.setItem(1, potion2);
            brewingStandInventory.setItem(2, potion3);

            event.getPlayer().openInventory(brewingStandInventory);
        }
    }



    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("pbreload")) {
            if (sender.hasPermission("pbrew.reload")) {
                reloadConfigFile();
                sender.sendMessage("Config file reloaded.");
                return true;
            } else {
                sender.sendMessage("You do not have permission to use this command.");
                return false;
            }
        }
        return false;
    }


    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }
        if (clickedInventory.getType() == InventoryType.BREWING) {
            if (event.getClick() == ClickType.LEFT) {
                Player player = (Player) event.getWhoClicked();
                BrewingStand brewingStand = (BrewingStand) clickedInventory.getHolder();
                int slot = event.getSlot();
                ItemStack cursorItem = event.getCursor();
                ItemStack targetItem = brewingStand.getInventory().getItem(slot);
                if (slot == 3) {
                    event.setCursor(targetItem);
                    brewingStand.getInventory().setItem(slot, cursorItem);
                    player.updateInventory();
                    System.out.println("onBrewStandUpdate triggered...");
                    BrewerInventory inv = brewingStand.getInventory();
                    ItemStack ingredient = inv.getItem(3);
                    ItemStack fuel = inv.getItem(4);

                    if (ingredient != null && fuel != null) {
                        if (ingredient.getType() != Material.AIR && fuel.getType() != Material.AIR) {
                            Material input1 = ingredient.getType();
                            Material input2 = fuel.getType();
                            List<ItemStack> outputs = recipes.get(Arrays.asList(input1, input2));
                            if (outputs == null) {
                                outputs = recipes.get(Arrays.asList(input2, input1));
                            }

                            if (outputs != null) {
                                if (outputs.size() == 1) {
                                    inv.setItem(0, outputs.get(0));
                                } else if (outputs.size() == 2) {
                                    inv.setItem(0, outputs.get(0));
                                    inv.setItem(1, outputs.get(1));
                                } else if (outputs.size() == 3) {
                                    inv.setItem(0, outputs.get(0));
                                    inv.setItem(1, outputs.get(1));
                                    inv.setItem(2, outputs.get(2));
                                }
                                ingredient.setAmount(ingredient.getAmount() - 1);
                                fuel.setAmount(fuel.getAmount() - 1);
                                System.out.println("Items dispensed: " + input1 + ", " + input2);
                            } else {
                                System.out.println("No recipe found for ingredients: " + input1 + ", " + input2);
                            }
                        }
                    }
                }
                else if (slot == 0 || slot == 1 || slot == 2 || slot == 4) {
                    event.setCursor(targetItem);
                    brewingStand.getInventory().setItem(slot, cursorItem);
                    player.updateInventory();
                    System.out.println("onBrewStandUpdate triggered...");
                    BrewerInventory inv = brewingStand.getInventory();
                    ItemStack ingredient = inv.getItem(3);
                    ItemStack fuel = inv.getItem(4);

                    if (ingredient != null && fuel != null) {
                        if (ingredient.getType() != Material.AIR && fuel.getType() != Material.AIR) {
                            Material input1 = ingredient.getType();
                            Material input2 = fuel.getType();
                            List<ItemStack> outputs = recipes.get(Arrays.asList(input1, input2));

                            if (outputs != null) {
                                if (outputs.size() == 1) {
                                    inv.setItem(0, outputs.get(0));
                                    player.playSound(player.getLocation(), "entity.player.burp", 1, 1);
                                } else if (outputs.size() == 2) {
                                    inv.setItem(0, outputs.get(0));
                                    inv.setItem(1, outputs.get(1));
                                    player.playSound(player.getLocation(), "entity.player.burp", 1, 1);
                                } else if (outputs.size() == 3) {
                                    inv.setItem(0, outputs.get(0));
                                    inv.setItem(1, outputs.get(1));
                                    inv.setItem(2, outputs.get(2));
                                    player.playSound(player.getLocation(), "entity.player.burp", 1, 1);
                                }
                                ingredient.setAmount(ingredient.getAmount() - 1);
                                fuel.setAmount(fuel.getAmount() - 1);
                                System.out.println("Items dispensed: " + input1 + ", " + input2);
                            } else {
                                System.out.println("No recipe found for ingredients: " + input1 + ", " + input2);
                            }
                        }
                    }
                }
                event.setCancelled(true);
            }
        }
    }
}