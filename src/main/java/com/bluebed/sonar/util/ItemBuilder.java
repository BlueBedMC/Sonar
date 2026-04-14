package com.bluebed.sonar.util;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.item.ItemProvider;

import java.util.Arrays;
import java.util.List;

public class ItemBuilder extends ItemStack {
    public ItemBuilder() {
        super(Material.AIR);
    }

    private ItemBuilder(ItemStack item) {
        super(item);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder(ItemStack itemStack) {
        return new Builder(new ItemBuilder(itemStack));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements ItemProvider {
        private final ItemBuilder itemBuilder;

        private Builder(ItemBuilder itemBuilder) {
            this.itemBuilder = itemBuilder;
        }

        private Builder() {
            this.itemBuilder = new ItemBuilder();
        }

        public Builder material(Material material) {
            this.itemBuilder.setType(material);
            return this;
        }

        public Builder material(Material material, int data) {
            this.itemBuilder.setType(material);
            this.itemBuilder.setDurability((short)data);
            return this;
        }

        public Builder meta(int data) {
            this.itemBuilder.setDurability((short)data);
            return this;
        }

        public Builder amount(int amount) {
            this.itemBuilder.setAmount(amount);
            return this;
        }

        public Builder addAmount(int amount) {
            this.itemBuilder.setAmount(this.itemBuilder.getAmount() + amount);
            return this;
        }

        public Builder removeAmount(int amount) {
            this.itemBuilder.setAmount(this.itemBuilder.getAmount() - amount);
            return this;
        }

        public Builder enchant(Enchantment enchant, Integer level) {
            this.itemBuilder.addUnsafeEnchantment(enchant, level);
            return this;
        }

        public Builder hiddenEnchant(Enchantment enchant, Integer level) {
            ItemMeta itemMeta = this.itemBuilder.getItemMeta();
            itemMeta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            this.itemBuilder.setItemMeta(itemMeta);
            return this.enchant(enchant, level);
        }

        public Builder name(String name) {
            ItemMeta itemMeta = this.itemBuilder.getItemMeta();
            itemMeta.setDisplayName(name);
            this.itemBuilder.setItemMeta(itemMeta);
            return this;
        }

        public Builder lore(String... lore) {
            return this.lore(Arrays.asList(lore));
        }

        public Builder lore(List<String> lore) {
            ItemMeta itemMeta = this.itemBuilder.getItemMeta();
            itemMeta.setLore(lore);
            this.itemBuilder.setItemMeta(itemMeta);
            return this;
        }

        public Builder unbreakable() {
            ItemMeta itemMeta = this.itemBuilder.getItemMeta();
            itemMeta.setUnbreakable(true);
            itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            this.itemBuilder.setItemMeta(itemMeta);
            return this;
        }

        public Builder skullOwner(String owner) {
            this.material(Material.PLAYER_HEAD);
            ItemMeta itemMeta = this.itemBuilder.getItemMeta();
            SkullMeta skullMeta = (SkullMeta)itemMeta;
            skullMeta.setOwner(owner);
            this.itemBuilder.setItemMeta(skullMeta);
            return this;
        }

//        public Item.Builder skullValue(String value) {
//            this.material(Material.PLAYER_HEAD);
//            this.meta(SkullType.PLAYER.ordinal());
//            ItemMeta itemMeta = this.item.getItemMeta();
//            SkullMeta skullMeta = (SkullMeta)itemMeta;
//            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), (String)null);
//            gameProfile.getProperties().put("textures", new Property("textures", value));
//            Reflections.setField(skullMeta, "profile", gameProfile);
//            this.item.setItemMeta(skullMeta);
//            return this;
//        }

        public Builder color(Color color) {
            ItemMeta itemMeta = this.itemBuilder.getItemMeta();
            LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta)itemMeta;
            leatherArmorMeta.setColor(color);
            this.itemBuilder.setItemMeta(leatherArmorMeta);
            return this;
        }

        public Builder hideItemFlags() {
            ItemMeta itemMeta = this.itemBuilder.getItemMeta();

            for(ItemFlag itemFlag : ItemFlag.values()) {
                itemMeta.addItemFlags(new ItemFlag[]{itemFlag});
            }

            this.itemBuilder.setItemMeta(itemMeta);
            return this;
        }

        public ItemBuilder build() {
            return this.itemBuilder;
        }

        @Override
        public @NotNull ItemStack get(@Nullable String s) {
            return build();
        }
    }
}
