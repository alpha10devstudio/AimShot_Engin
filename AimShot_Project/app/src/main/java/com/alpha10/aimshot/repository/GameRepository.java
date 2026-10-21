package com.alpha10.aimshot.repository;

import com.alpha10.aimshot.R;
import com.alpha10.aimshot.model.GameItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Centralized repository containing verified game configurations and optimizer profiles.
 */
public class GameRepository {

    private static GameRepository instance;
    private final List<GameItem> supportedGames;

    private GameRepository() {
        supportedGames = new ArrayList<>();
        initializeGames();
    }

    public static synchronized GameRepository getInstance() {
        if (instance == null) {
            instance = new GameRepository();
        }
        return instance;
    }

    private void initializeGames() {
        supportedGames.add(new GameItem("pubg", "PUBG Mobile", "Aim Assist • Crosshair • FPS Boost", R.drawable.ic_aimshot_logo, true, "com.tencent.ig"));
        supportedGames.add(new GameItem("freefire", "Free Fire", "Smooth Aim • No Recoil • Custom HUD", R.drawable.ic_aimshot_logo, true, "com.dts.freefireth"));
        supportedGames.add(new GameItem("bgmi", "BGMI", "Performance • Aim Tools • Anti-Lag", R.drawable.ic_aimshot_logo, true, "com.pubg.imobile"));
        supportedGames.add(new GameItem("codm", "Call of Duty Mobile", "Precision • Sensitivity • No Recoil", R.drawable.ic_aimshot_logo, true, "com.activision.callofduty.shooter"));
        supportedGames.add(new GameItem("valorant", "Valorant", "Crosshair • FPS • Game Sense", R.drawable.ic_aimshot_logo, true, "com.riotgames.valorant"));
        supportedGames.add(new GameItem("gtav", "GTA V", "Aim Bot • Visuals • Performance", R.drawable.ic_aimshot_logo, true, "com.rockstargames.gtav"));
        supportedGames.add(new GameItem("efootball", "eFootball", "Better Control • Aim Assist • Smooth", R.drawable.ic_aimshot_logo, true, "jp.konami.pesam"));
        supportedGames.add(new GameItem("minecraft", "Minecraft", "FPS Boost • Performance • Stability", R.drawable.ic_aimshot_logo, true, "com.mojang.minecraftpe"));
    }

    public List<GameItem> getAllGames() {
        return Collections.unmodifiableList(supportedGames);
    }

    public List<GameItem> searchGames(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllGames();
        }
        String lower = query.trim().toLowerCase();
        List<GameItem> results = new ArrayList<>();
        for (GameItem item : supportedGames) {
            if (item.getName().toLowerCase().contains(lower) || item.getFeatureSummary().toLowerCase().contains(lower)) {
                results.add(item);
            }
        }
        return results;
    }
}
