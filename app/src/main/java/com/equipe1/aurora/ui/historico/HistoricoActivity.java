package com.equipe1.aurora.ui.historico;

// =========================================================================
// BLOCO 1: IMPORTAÇÕES DE DEPENDÊNCIAS
// =========================================================================
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.equipe1.aurora.R;
import com.equipe1.aurora.databinding.ActivityHistoricoBinding;
import com.equipe1.aurora.domain.model.HistoricoItem;
import com.equipe1.aurora.domain.usecase.ObterHistoricoUseCase;
import com.equipe1.aurora.domain.usecase.ObterResumoEstatisticoUseCase;
import com.equipe1.aurora.infra.local.AppDataBase;
import com.equipe1.aurora.infra.network.api.ApiClient;
import com.equipe1.aurora.infra.repository.HistoricoRepositoryImpl;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

// =========================================================================
// BLOCO 2: CLASSE PRINCIPAL DA ACTIVITY
// =========================================================================
public class HistoricoActivity extends AppCompatActivity {

    private ActivityHistoricoBinding binding;
    private HistoricoViewModel viewModel;
    private HistoricoAdapter adapter;

    // =========================================================================
    // BLOCO 3: CICLO DE VIDA (ONCREATE)
    // =========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializa as configurações do mapa OSM
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        binding = ActivityHistoricoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        configurarBotoesTopo();
        inicializarViewModel();
        configurarMapaOSM();
        configurarRecyclerView();
        configurarFiltros();
        observarViewModel();

        // Dispara o carregamento dos dados do repositório
        viewModel.carregarHistorico(1L);
    }

    // =========================================================================
    // BLOCO 4: CONFIGURAÇÃO DE NAVEGAÇÃO E AÇÕES DO TOPO
    // =========================================================================
    private void configurarBotoesTopo() {
        binding.btnVoltar.setOnClickListener(v -> finish());
        binding.btnCompartilhar.setOnClickListener(v -> compartilharHistorico());
    }

    // =========================================================================
    // BLOCO 5: INICIALIZAÇÃO DA VIEWMODEL E CASOS DE USO
    // =========================================================================
    private void inicializarViewModel() {
        AppDataBase db = AppDataBase.getInstance(this);
        HistoricoRepositoryImpl repository = new HistoricoRepositoryImpl(db.alertaSosDao(), ApiClient.getApiService());

        ObterHistoricoUseCase obterHistoricoUseCase = new ObterHistoricoUseCase(repository);
        ObterResumoEstatisticoUseCase obterResumoUseCase = new ObterResumoEstatisticoUseCase(repository);

        viewModel = new HistoricoViewModel(obterHistoricoUseCase, obterResumoUseCase);
    }

    // =========================================================================
    // BLOCO 6: CONFIGURAÇÃO DO MAPA INTERATIVO (OPENSTREETMAP)
    // =========================================================================
    private void configurarMapaOSM() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.setMultiTouchControls(true);

        GeoPoint pontoSP = new GeoPoint(-23.550520, -46.633308);
        binding.mapView.getController().setZoom(15.0);
        binding.mapView.getController().setCenter(pontoSP);
    }

    // =========================================================================
    // BLOCO 7: CONFIGURAÇÃO DA RECYCLERVIEW E ADAPTER
    // =========================================================================
    private void configurarRecyclerView() {
        adapter = new HistoricoAdapter(this::focarEAnotarNoMapa);
        binding.rvHistorico.setAdapter(adapter);
    }

    // =========================================================================
    // BLOCO 8: LÓGICA DE FILTROS E SUBFILTROS DE PERÍODO
    // =========================================================================
    private void configurarFiltros() {
        // Escuta os Filtros Principais
        binding.chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                viewModel.filtrarPorTipo(null);
                binding.scrollPeriodo.setVisibility(View.GONE);
                binding.btnLimparFiltro.setVisibility(View.GONE);
                return;
            }

            int checkedId = checkedIds.get(0);
            if (checkedId == binding.chipTodos.getId()) {
                viewModel.filtrarPorTipo(null);
                binding.scrollPeriodo.setVisibility(View.GONE);
                binding.btnLimparFiltro.setVisibility(View.GONE);
            } else {
                binding.scrollPeriodo.setVisibility(View.VISIBLE);
                binding.btnLimparFiltro.setVisibility(View.VISIBLE);

                if (checkedId == binding.chipSos.getId()) {
                    viewModel.filtrarPorTipo(HistoricoItem.TipoHistorico.ALERTA_SOS);
                } else if (checkedId == binding.chipOcorrencias.getId()) {
                    viewModel.filtrarPorTipo(HistoricoItem.TipoHistorico.OCORRENCIA_DENUNCIA);
                } else if (checkedId == binding.chipRotas.getId()) {
                    viewModel.filtrarPorTipo(HistoricoItem.TipoHistorico.ROTA);
                }
            }
        });

        // Escuta os Subfiltros de Período
        binding.chipGroupPeriodo.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            aplicarFiltroPeriodo(checkedId);
        });

        // Botão para resetar filtros
        binding.btnLimparFiltro.setOnClickListener(v -> {
            binding.chipTodos.setChecked(true);
            binding.chipPeriodoTodos.setChecked(true);
            binding.scrollPeriodo.setVisibility(View.GONE);
            binding.btnLimparFiltro.setVisibility(View.GONE);
            viewModel.filtrarPorTipo(null);
        });
    }

    // =========================================================================
    // BLOCO 9: APLICAÇÃO DO FILTRO DE PERÍODO EM MEMÓRIA
    // =========================================================================
    private void aplicarFiltroPeriodo(int chipId) {
        List<HistoricoItem> listaOriginal = viewModel.historicoItems.getValue();
        if (listaOriginal == null) return;

        List<HistoricoItem> filtrados = new ArrayList<>();
        long agora = System.currentTimeMillis();

        for (HistoricoItem item : listaOriginal) {
            long tempoItem = item.getDataHora().getTime();

            if (chipId == binding.chipHoje.getId()) {
                if (DateUtils.isToday(tempoItem)) filtrados.add(item);
            } else if (chipId == binding.chipOntem.getId()) {
                if (DateUtils.isToday(tempoItem + DateUtils.DAY_IN_MILLIS)) filtrados.add(item);
            } else if (chipId == binding.chipEstaSemana.getId()) {
                if ((agora - tempoItem) <= (7 * DateUtils.DAY_IN_MILLIS)) filtrados.add(item);
            } else {
                filtrados.add(item);
            }
        }
        adapter.setItens(filtrados);
    }

    // =========================================================================
    // BLOCO 10: OBSERVAÇÃO DO ESTADO DA VIEWMODEL
    // =========================================================================
    private void observarViewModel() {
        viewModel.historicoItems.observe(this, itens -> {
            adapter.setItens(itens);
            boolean listaVazia = itens == null || itens.isEmpty();
            binding.tvStateVazio.setVisibility(listaVazia ? View.VISIBLE : View.GONE);
        });

        viewModel.carregando.observe(this, estaCarregando ->
                binding.progressBar.setVisibility(estaCarregando ? View.VISIBLE : View.GONE));

        viewModel.mensagemErro.observe(this, mensagem -> {
            if (mensagem != null && !mensagem.isEmpty()) {
                Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================================
    // BLOCO 11: FUNCIONALIDADE DE COMPARTILHAMENTO E EXPORTAÇÃO
    // =========================================================================
    private void compartilharHistorico() {
        List<HistoricoItem> listaAtual = viewModel.historicoItems.getValue();
        if (listaAtual == null || listaAtual.isEmpty()) {
            Toast.makeText(this, "Nenhum histórico disponível para exportar.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("--- RELATÓRIO DE HISTÓRICO DE SEGURANÇA - AURORA ---\n\n");

        for (HistoricoItem item : listaAtual) {
            builder.append("• ").append(item.getTitulo()).append("\n");
            builder.append("  Detalhes: ").append(item.getDescricao()).append("\n");
            builder.append("  Data: ").append(item.getDataHora().toString()).append("\n\n");
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, builder.toString());
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "Compartilhar Histórico via:");
        startActivity(shareIntent);
    }

    // =========================================================================
    // BLOCO 12: INTERAÇÃO DE SELEÇÃO E FOCO NO MAPA
    // =========================================================================
    private void focarEAnotarNoMapa(HistoricoItem item) {
        if (item.getLatitude() == 0.0 && item.getLongitude() == 0.0) return;

        GeoPoint ponto = new GeoPoint(item.getLatitude(), item.getLongitude());
        binding.mapView.getController().animateTo(ponto);
        binding.mapView.getController().setZoom(17.0);

        binding.mapView.getOverlays().clear();
        Marker marker = new Marker(binding.mapView);
        marker.setPosition(ponto);
        marker.setTitle(item.getTitulo());
        marker.setSnippet(item.getDescricao());
        binding.mapView.getOverlays().add(marker);
        binding.mapView.invalidate();
    }

    // =========================================================================
    // BLOCO 13: CICLOS DE VIDA DO MAPA OSM
    // =========================================================================
    @Override
    protected void onResume() {
        super.onResume();
        binding.mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.mapView.onPause();
    }
}