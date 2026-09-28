package com.equipe1.aurora.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.equipe1.aurora.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class HomeFragment extends Fragment {
    private MapView mapPreview;
    private View mapOverlay;
    private EditText etSearch;
    private ImageView imgProfile;
    private TextView tvFriendsSeeAll;
    private LinearLayout llShortcuts;
    private MaterialCardView btnShortcutCasa;
    private MaterialCardView btnShortcutTrabalho;
    private MaterialCardView btnShortcutAdicionar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapPreview = view.findViewById(R.id.mapa_preview);
        mapOverlay = view.findViewById(R.id.view_mapa_clique_sobreposicao);
        etSearch = view.findViewById(R.id.et_pesquisa);
        imgProfile = view.findViewById(R.id.img_foto_perfil);
        tvFriendsSeeAll = view.findViewById(R.id.tv_ver_todos);
        llShortcuts = view.findViewById(R.id.ll_atalhos);

        btnShortcutCasa = view.findViewById(R.id.btn_acesso_casa);
        btnShortcutTrabalho = view.findViewById(R.id.btn_acesso_trabalho);
        btnShortcutAdicionar = view.findViewById(R.id.btn_acesso_adicionar);

        setupMapPreview();
        setupListeners(view);
    }

    private void setupMapPreview() {
        if (mapPreview == null) return;

        mapPreview.setTileSource(TileSourceFactory.MAPNIK);
        mapPreview.setMultiTouchControls(false);
        mapPreview.setClickable(false);
        mapPreview.setFocusable(false);

        IMapController mapController = mapPreview.getController();
        mapController.setZoom(15.0);
        GeoPoint startPoint = new GeoPoint(-23.5505, -46.6333);
        mapController.setCenter(startPoint);
    }

    private void setupListeners(View view) {
        // A. Clique no Mapa
        if (mapOverlay != null) {
            mapOverlay.setOnClickListener(v -> abrirTelaMapa());
        }

        // B. Perfil
        if (imgProfile != null) {
            imgProfile.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Abrindo Perfil...", Toast.LENGTH_SHORT).show()
            );
        }

        // C. Pesquisa
        if (etSearch != null) {
            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                    String query = etSearch.getText().toString().trim();
                    if (!query.isEmpty()) {
                        Bundle args = new Bundle();
                        args.putString("search_query", query);
                        try {
                            Navigation.findNavController(v).navigate(R.id.nav_mapa, args);
                        } catch (Exception e) {
                            abrirTelaMapa();
                        }
                    }
                    return true;
                }
                return false;
            });
        }

        // D. Ver Todos Amigos
        if (tvFriendsSeeAll != null) {
            tvFriendsSeeAll.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Exibindo lista de amigos...", Toast.LENGTH_SHORT).show()
            );
        }

        // E. Atalho Casa
        if (btnShortcutCasa != null) {
            btnShortcutCasa.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Iniciando rota para Casa...", Toast.LENGTH_SHORT).show()
            );
        }

        // F. Atalho Trabalho
        if (btnShortcutTrabalho != null) {
            btnShortcutTrabalho.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Iniciando rota para Trabalho...", Toast.LENGTH_SHORT).show()
            );
        }

        // G. Atalho Adicionar
        if (btnShortcutAdicionar != null) {
            btnShortcutAdicionar.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Adicionar novo atalho...", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void abrirTelaMapa() {
        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_mapa);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapPreview != null) mapPreview.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapPreview != null) mapPreview.onPause();
    }
}