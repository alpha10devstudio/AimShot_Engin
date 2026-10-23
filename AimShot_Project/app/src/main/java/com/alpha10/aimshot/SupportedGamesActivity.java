package com.alpha10.aimshot;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alpha10.aimshot.adapter.GameListAdapter;
import com.alpha10.aimshot.model.GameItem;
import com.alpha10.aimshot.repository.GameRepository;

import java.util.List;

/**
 * Screen presenting all supported games, their optimization profiles, and search capability.
 */
public class SupportedGamesActivity extends AppCompatActivity {

    private RecyclerView rvGames;
    private GameListAdapter adapter;
    private EditText editSearch;
    private View btnSearch;
    private boolean isSearchVisible = false;

    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supported_games);

        initViews();
        setupGameList();
        setupSearch();
    }

    private void initViews() {
        rvGames = findViewById(R.id.rv_supported_games_list);
        editSearch = findViewById(R.id.edit_games_search);
        btnSearch = findViewById(R.id.btn_games_search);
        tvEmptyState = findViewById(R.id.tv_games_empty_state);

        View btnBack = findViewById(R.id.btn_games_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupGameList() {
        rvGames.setLayoutManager(new LinearLayoutManager(this));
        List<GameItem> allGames = GameRepository.getInstance().getAllGames();
        adapter = new GameListAdapter(allGames, new GameListAdapter.OnGameActionListener() {
            @Override
            public void onGameSelected(GameItem item) {
                if (item.isActive()) {
                    Toast.makeText(SupportedGamesActivity.this,
                            item.getName() + " tools active: " + item.getFeatureSummary(),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SupportedGamesActivity.this,
                            item.getName() + " is currently locked / Pro Key required",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvGames.setAdapter(adapter);
    }

    private void setupSearch() {
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSearchVisible = !isSearchVisible;
                editSearch.setVisibility(isSearchVisible ? View.VISIBLE : View.GONE);
                if (isSearchVisible) {
                    editSearch.requestFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(editSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                } else {
                    editSearch.setText("");
                    List<GameItem> all = GameRepository.getInstance().getAllGames();
                    adapter.updateData(all);
                    if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
                }
            }
        });

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<GameItem> filtered = GameRepository.getInstance().searchGames(s.toString());
                adapter.updateData(filtered);
                if (tvEmptyState != null) {
                    tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}
