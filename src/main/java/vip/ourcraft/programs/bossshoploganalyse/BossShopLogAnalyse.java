package vip.ourcraft.programs.bossshoploganalyse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BossShopLogAnalyse {
    private static int globalTotalTradedPrice;
    private static List<String> globalTotalTradedPlayers;
    private static int globalTotalTradedCount;
    private String itemIndex;
    private String itemInfo;
    private Double totalTradedPrice = 0D;
    private int totalTradedCount;
    private List<String> tradedPlayers = new ArrayList<>();
    private File sourceFile;

    static {
        globalTotalTradedPrice = 0;
        globalTotalTradedPlayers = new ArrayList<>();
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getItemIndex() {
        return itemIndex;
    }

    public void setItemIndex(String itemIndex) {
        this.itemIndex = itemIndex;
    }

    public String getItemInfo() {
        return itemInfo;
    }

    public void setItemInfo(String itemInfo) {
        this.itemInfo = itemInfo;
    }

    public Double getTotalTradedPrice() {
        return totalTradedPrice;
    }

    public void addTotalTradedPrice(double amount) {
        this.totalTradedPrice += amount;
        globalTotalTradedPrice += amount;
    }

    public List<String> getTradedPlayers() {
        return tradedPlayers;
    }

    private boolean isTradedPlayer(String player) {
        return tradedPlayers.contains(player);
    }

    public void putTradedPlayer(String player) {
        if (!isTradedPlayer(player)) {
            tradedPlayers.add(player);
        }

        if (!globalTotalTradedPlayers.contains(player)) {
            globalTotalTradedPlayers.add(player);
        }
    }

    public int getTotalTradedCount() {
        return totalTradedCount;
    }

    public void addTotalTradedCount(int totalTradedCount) {
        this.totalTradedCount += totalTradedCount;
        globalTotalTradedCount += 1;
    }

    public double getPercentOfGlobalTradedPrice() {
        return totalTradedPrice / globalTotalTradedPrice;
    }

    public double getPercentOfGlobalTradedPlayerCount() {
        if (tradedPlayers.size() == 0 || globalTotalTradedPlayers.size() == 0) {
            return 0;
        }

        return (double) tradedPlayers.size() / globalTotalTradedPlayers.size();
    }

    public double getPercentOfGlobalTradedCount() {
        return (double) totalTradedCount / globalTotalTradedCount;
    }

    public int getTradedPlayerCount() {
        return tradedPlayers.size();
    }

    public static void resetGlobalData() {
        globalTotalTradedCount = 0;
        globalTotalTradedPlayers = new ArrayList<>();
        globalTotalTradedPrice = 0;

    }
}
