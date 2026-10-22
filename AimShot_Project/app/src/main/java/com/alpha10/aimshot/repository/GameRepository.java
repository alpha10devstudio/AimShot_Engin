package com.alpha10.aimshot.repository;

import com.alpha10.aimshot.R;
import com.alpha10.aimshot.model.GameItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Centralized repository containing verified game configurations and optimizer profiles.
 * Carrom Disc Pool is positioned 1st (Active & Unlocked).
 * 8 Ball Pool is positioned 2nd (Locked, alpha 0.45).
 * Followed by additional supported battle royale and competitive titles (Locked, alpha 0.45).
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
        // 1. Carrom Disc Pool - Active & Unlocked
        supportedGames.add(new GameItem(
                "carrom",
                "Carrom Disc Pool",
                "Auto Aim • Guideline • Smooth Control",
                R.drawable.game_carrom,
                true,
                "com.miniclip.carrom",
                true,
                "game_carrom"
        ));

        // 2. 8 Ball Pool - Locked
        supportedGames.add(new GameItem(
                "8ball",
                "8 Ball Pool",
                "Long Line • Accurate Angle • Safe",
                R.drawable.game_8ball,
                true,
                "com.miniclip.eightballpool",
                false,
                "game_8ball"
        ));

        // 3. PUBG Mobile - Locked
        supportedGames.add(new GameItem(
                "pubg",
                "PUBG Mobile",
                "Aim Assist • Crosshair • FPS Boost",
                R.drawable.game_pubg,
                false,
                "com.tencent.ig",
                false,
                "game_pubg"
        ));

        // 4. Free Fire - Locked
        supportedGames.add(new GameItem(
                "freefire",
                "Free Fire",
                "Smooth Aim • No Recoil • Custom HUD",
                R.drawable.game_freefire,
                false,
                "com.dts.freefireth",
                false,
                "game_freefire"
        ));

        // 5. BGMI - Locked
        supportedGames.add(new GameItem(
                "bgmi",
                "BGMI",
                "Performance • Aim Tools • Anti-Lag",
                R.drawable.game_bgmi,
                false,
                "com.pubg.imobile",
                false,
                "game_bgmi"
        ));

        // 6. Call of Duty Mobile - Locked
        supportedGames.add(new GameItem(
                "codm",
                "Call of Duty Mobile",
                "Precision • Sensitivity • No Recoil",
                R.drawable.game_codm,
                false,
                "com.activision.callofduty.shooter",
                false,
                "game_codm"
        ));

        // 7. Valorant - Locked
        supportedGames.add(new GameItem(
                "valorant",
                "Valorant",
                "Crosshair • FPS • Game Sense",
                R.drawable.game_valorant,
                false,
                "com.riotgames.valorant",
                false,
                "game_valorant"
        ));

        // 8. GTA V - Locked
        supportedGames.add(new GameItem(
                "gtav",
                "GTA V",
                "Aim Bot • Visuals • Performance",
                R.drawable.game_gtav,
                false,
                "com.rockstargames.gtav",
                false,
                "game_gtav"
        ));

        // 9. eFootball - Locked
        supportedGames.add(new GameItem(
                "efootball",
                "eFootball",
                "Better Control • Aim Assist • Smooth",
                R.drawable.game_efootball,
                false,
                "jp.konami.pesam",
                false,
                "game_efootball"
        ));

        // 10. Minecraft - Locked
        supportedGames.add(new GameItem(
                "minecraft",
                "Minecraft",
                "FPS Boost • Performance • Stability",
                R.drawable.game_minecraft,
                false,
                "com.mojang.minecraftpe",
                false,
                "game_minecraft"
        ));
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
