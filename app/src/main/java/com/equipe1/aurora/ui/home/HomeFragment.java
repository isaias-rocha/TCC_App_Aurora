package com.equipe1.aurora.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.equipe1.aurora.R;
import com.equipe1.aurora.databinding.FragmentHomeBinding;
import com.equipe1.aurora.ui.auth.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;

public class HomeFragment extends Fragment {

    // Instâncias do ViewModel e ViewBinding
    private HomeViewModel viewModel;
    private FragmentHomeBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuração de carregamento das preferências e User-Agent do mapa
        Context contexto = requireActivity().getApplicationContext();
        Configuration.getInstance().load(contexto, PreferenceManager.getDefaultSharedPreferences(contexto));
        Configuration.getInstance().setUserAgentValue(contexto.getPackageName());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflação do layout utilizando ViewBinding
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicialização do ViewModel da arquitetura MVVM
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Chamada dos métodos de inicialização e tratamento da UI
        configurarMapaPreview();
        configurarOuvintes();
        configurarObservadores();
        configurarGestoVoltar();
    }

    // Configuração inicial da pré-visualização do mapa interativo
    private void configurarMapaPreview() {
        if (binding.mapaPreview == null) return;

        binding.mapaPreview.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapaPreview.setMultiTouchControls(false);
        binding.mapaPreview.setClickable(false);
        binding.mapaPreview.setFocusable(false);

        IMapController controladorMapa = binding.mapaPreview.getController();
        controladorMapa.setZoom(15.0);
        GeoPoint pontoInicial = new GeoPoint(-23.5505, -46.6333);
        controladorMapa.setCenter(pontoInicial);
    }

    // Configuração de eventos de clique e ações do usuário
    private void configurarOuvintes() {

        // A. Clique no Cabeçalho de Perfil -> Redireciona para a Tela de Perfil
        binding.llHeaderProfile.setOnClickListener(v -> {
            anunciarTalkBack("Navegando para o perfil do usuário");
            try {
                Navigation.findNavController(v).navigate(R.id.nav_perfil);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Não foi possível abrir o perfil.", Toast.LENGTH_SHORT).show();
            }
        });

        // B. Clique na sobreposição do Mapa
        binding.viewMapaCliqueSobreposicao.setOnClickListener(v -> abrirTelaMapa());

        // C. Pesquisa de locais
        binding.etPesquisa.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                String textoBusca = binding.etPesquisa.getText().toString().trim();
                if (!textoBusca.isEmpty()) {
                    anunciarTalkBack("Pesquisando por " + textoBusca);
                    Bundle argumentos = new Bundle();
                    argumentos.putString("search_query", textoBusca);
                    try {
                        Navigation.findNavController(v).navigate(R.id.nav_mapa, argumentos);
                    } catch (Exception e) {
                        abrirTelaMapa();
                    }
                }
                return true;
            }
            return false;
        });

        // D. Botões do menu rápido
        binding.btnRotasSalvas.setOnClickListener(v -> {
            anunciarTalkBack("Abrindo Rotas Salvas");
            Toast.makeText(getContext(), "Abrindo Rotas Salvas...", Toast.LENGTH_SHORT).show();
        });

        binding.btnSecaoCirculos.setOnClickListener(v -> {
            anunciarTalkBack("Abrindo Círculos de Segurança");
            Toast.makeText(getContext(), "Abrindo Círculos...", Toast.LENGTH_SHORT).show();
        });

        binding.btnHistorico.setOnClickListener(v -> {
            anunciarTalkBack("Abrindo Histórico de Alertas");
            Toast.makeText(getContext(), "Abrindo Histórico", Toast.LENGTH_SHORT).show();
        });

        // E. Gerenciamento e adição de Círculos
        binding.tvGerenciarCirculos.setOnClickListener(v -> {
            anunciarTalkBack("Abrindo gerenciamento de círculos");
            Toast.makeText(getContext(), "Gerenciando círculos", Toast.LENGTH_SHORT).show();
        });

        binding.cardAddCirculo.setOnClickListener(v -> {
            anunciarTalkBack("Abrindo tela para criar novo círculo");
            Toast.makeText(getContext(), "Criar novo círculo", Toast.LENGTH_SHORT).show();
        });

        // F. Ver contatos próximos
        binding.tvVerTodos.setOnClickListener(v -> {
            anunciarTalkBack("Abrindo lista completa de contatos próximos");
            Toast.makeText(getContext(), "Exibindo lista de contatos", Toast.LENGTH_SHORT).show();
        });
    }

    // Observadores LiveData para atualização reativa
    private void configurarObservadores() {
        viewModel.getObterNomeUsuario().observe(getViewLifecycleOwner(), nome -> {
            if (nome != null) {
                binding.tvNomeUsuario.setText(nome);
                binding.llHeaderProfile.setContentDescription(
                        "Perfil do usuário. Olá, " + nome + ". Toque para abrir as configurações do seu perfil."
                );
            }
        });
    }

    // Interceptação do gesto / botão de voltar quando estiver na Home
    private void configurarGestoVoltar() {
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        exibirDialogoConfirmacaoSair();
                    }
                }
        );
    }

    // Exibição da caixa de diálogo pedindo confirmação para encerrar a sessão/sair
    private void exibirDialogoConfirmacaoSair() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Encerrar Sessão")
                .setMessage("Deseja realmente sair da sua conta?")
                .setPositiveButton("Sair", (dialog, which) -> encerrarSessao())
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Método responsável por deslogar o usuário e retornar à tela de Login
    private void encerrarSessao() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    // Utilitário de acessibilidade
    private void anunciarTalkBack(String mensagem) {
        if (getView() != null) {
            getView().announceForAccessibility(mensagem);
        }
    }

    // Navegação para a aba do mapa
    private void abrirTelaMapa() {
        anunciarTalkBack("Abrindo mapa interativo em tela cheia");
        BottomNavigationView navegacaoInferior = requireActivity().findViewById(R.id.bottom_navigation);
        if (navegacaoInferior != null) {
            navegacaoInferior.setSelectedItemId(R.id.nav_mapa);
        }
    }

    // Gerenciamento do ciclo de vida do componente MapView
    @Override
    public void onResume() {
        super.onResume();
        if (binding.mapaPreview != null) binding.mapaPreview.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding.mapaPreview != null) binding.mapaPreview.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding.mapaPreview != null) {
            binding.mapaPreview.onDetach();
        }
        binding = null;
    }
}