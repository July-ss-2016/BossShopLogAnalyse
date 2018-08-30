package vip.ourcraft.programs.bossshoploganalyse;

import com.google.gson.annotations.SerializedName;

public class BossShopLog {
    private String reward;
    @SerializedName("buy_type")
    private String buyType;
    private String price;
    @SerializedName("price_type")
    private String priceType;
    @SerializedName("item_name")
    private String itemName;
    private String time;
    @SerializedName("player_name")
    private String playerName;
    @SerializedName("shop_name")
    private String shopName;
    @SerializedName("is_purchased")
    private boolean isPurchased;

    public String getReward() {
        return reward;
    }

    public void setReward(String reward) {
        this.reward = reward;
    }

    public String getBuyType() {
        return buyType;
    }

    public void setBuyType(String buyType) {
        this.buyType = buyType;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getPriceType() {
        return priceType;
    }

    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public boolean isPurchased() {
        return isPurchased;
    }

    public void setPurchased(boolean purchased) {
        isPurchased = purchased;
    }
}
