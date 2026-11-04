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
