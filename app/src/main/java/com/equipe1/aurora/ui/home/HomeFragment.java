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
import androidx.lifecycle.ViewModelProvider;
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

    private HomeViewModel viewModel;

    // Perfil e Barra de Pesquisa
    private LinearLayout llHeaderProfile;
    private ImageView imgPerfil;
    private TextView tvNomeUsuario;
    private EditText etPesquisa;

    // Mapa
    private MapView mapaPreview;
    private View mapaOverlay;

    // Menu Horizontal (Navegação Rápida)
    private MaterialCardView btnRotasSalvas;
    private MaterialCardView btnSecaoCirculos;
    private MaterialCardView btnHistorico;

    // Secção "Meus Círculos"
    private TextView tvGerenciarCirculos;
    private MaterialCardView cardCirculoFamilia;
    private MaterialCardView cardCirculoFaculdade;
    private MaterialCardView cardAddCirculo;

    // Secção "Amigos Próximos"
    private TextView tvFriendsSeeAll;

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

        // Inicialização do ViewModel
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Mapeamento dos componentes de interface
        llHeaderProfile = view.findViewById(R.id.ll_header_profile);
        imgPerfil = view.findViewById(R.id.img_foto_perfil);
        tvNomeUsuario = view.findViewById(R.id.tv_nome_usuario);
        etPesquisa = view.findViewById(R.id.et_pesquisa);

        mapaPreview = view.findViewById(R.id.mapa_preview);
        mapaOverlay = view.findViewById(R.id.view_mapa_clique_sobreposicao);

        btnRotasSalvas = view.findViewById(R.id.btn_rotas_salvas);
        btnSecaoCirculos = view.findViewById(R.id.btn_secao_circulos);
        btnHistorico = view.findViewById(R.id.btn_historico);

        tvGerenciarCirculos = view.findViewById(R.id.tv_gerenciar_circulos);
        cardCirculoFamilia = view.findViewById(R.id.card_circulo_familia);
        cardCirculoFaculdade = view.findViewById(R.id.card_circulo_faculdade);
        cardAddCirculo = view.findViewById(R.id.card_add_circulo);

        tvFriendsSeeAll = view.findViewById(R.id.tv_ver_todos);

        setupMapPreview();
        setupListeners();
        setupObservers();
    }

    private void setupMapPreview() {
        if (mapaPreview == null) return;

        mapaPreview.setTileSource(TileSourceFactory.MAPNIK);
        mapaPreview.setMultiTouchControls(false);
        mapaPreview.setClickable(false);
        mapaPreview.setFocusable(false);

        IMapController mapController = mapaPreview.getController();
        mapController.setZoom(15.0);
        GeoPoint startPoint = new GeoPoint(-23.5505, -46.6333);
        mapController.setCenter(startPoint);
    }

    private void setupListeners() {
        // A. Perfil Agrupado (Compatível com TalkBack)
        if (llHeaderProfile != null) {
            llHeaderProfile.setOnClickListener(v -> {
                anunciarTalkBack("Abrindo perfil do usuário");
                Toast.makeText(getContext(), "Abrindo Perfil...", Toast.LENGTH_SHORT).show();
            });
        }

        // B. Clique no Mapa Preview
        if (mapaOverlay != null) {
            mapaOverlay.setOnClickListener(v -> abrirTelaMapa());
        }

        // C. Pesquisa
        if (etPesquisa != null) {
            etPesquisa.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                    String query = etPesquisa.getText().toString().trim();
                    if (!query.isEmpty()) {
                        anunciarTalkBack("Pesquisando por " + query);
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

        // D. Menu Horizontal de Seções
        if (btnRotasSalvas != null) {
            btnRotasSalvas.setOnClickListener(v -> {
                anunciarTalkBack("Abrindo Rotas Salvas");
                Toast.makeText(getContext(), "Abrindo Rotas Salvas...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnSecaoCirculos != null) {
            btnSecaoCirculos.setOnClickListener(v -> {
                anunciarTalkBack("Abrindo Círculos de Segurança");
                Toast.makeText(getContext(), "Abrindo Círculos...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnHistorico != null) {
            btnHistorico.setOnClickListener(v -> {
                anunciarTalkBack("Abrindo Histórico de Alertas");
                Toast.makeText(getContext(), "Abrindo Histórico...", Toast.LENGTH_SHORT).show();
            });
        }

        // E. Círculos de Segurança do Usuário
        if (tvGerenciarCirculos != null) {
            tvGerenciarCirculos.setOnClickListener(v -> {
                anunciarTalkBack("Abrindo gerenciamento de círculos");
                Toast.makeText(getContext(), "Gerenciando círculos...", Toast.LENGTH_SHORT).show();
            });
        }

        if (cardCirculoFamilia != null) {
            cardCirculoFamilia.setOnClickListener(v -> {
                anunciarTalkBack("Abrindo Círculo Família");
                Toast.makeText(getContext(), "Abrindo Círculo Família...", Toast.LENGTH_SHORT).show();
            });
        }

        if (cardCirculoFaculdade != null) {
            cardCirculoFaculdade.setOnClickListener(v -> {
                anunciarTalkBack("Abrindo Círculo Faculdade");
                Toast.makeText(getContext(), "Abrindo Círculo Faculdade...", Toast.LENGTH_SHORT).show();
            });
        }

        if (cardAddCirculo != null) {
            cardAddCirculo.setOnClickListener(v -> {
                anunciarTalkBack("Abrindo tela para criar novo círculo");
                Toast.makeText(getContext(), "Criar novo círculo...", Toast.LENGTH_SHORT).show();
            });
        }

        // F. Ver Todos os Amigos
        if (tvFriendsSeeAll != null) {
            tvFriendsSeeAll.setOnClickListener(v -> {
                anunciarTalkBack("Abrindo lista completa de amigos próximos");
                Toast.makeText(getContext(), "Exibindo lista de amigos...", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupObservers() {
        // Observa alterações no nome do usuário e atualiza a acessibilidade dinamicamente
        viewModel.getUserName().observe(getViewLifecycleOwner(), name -> {
            if (tvNomeUsuario != null && name != null) {
                tvNomeUsuario.setText(name);
                if (llHeaderProfile != null) {
                    llHeaderProfile.setContentDescription("Perfil do usuário. Olá, " + name + ". Que bom te ver por aqui. Toque duas vezes para abrir o perfil.");
                }
            }
        });
    }

    private void anunciarTalkBack(String mensagem) {
        if (getView() != null) {
            getView().announceForAccessibility(mensagem);
        }
    }

    private void abrirTelaMapa() {
        anunciarTalkBack("Abrindo mapa interativo em tela cheia");
        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_mapa);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapaPreview != null) mapaPreview.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapaPreview != null) mapaPreview.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Liberação de recursos do mapa para prevenir vazamento de memória
        if (mapaPreview != null) {
            mapaPreview.onDetach();
        }
    }
}