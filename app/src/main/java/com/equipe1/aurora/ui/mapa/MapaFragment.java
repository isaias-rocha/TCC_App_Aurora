package com.equipe1.aurora.ui.mapa;

// --- IMPORTS DO SISTEMA E ANDROID ---
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

// --- IMPORTS DE JETPACK, ARQUITETURA E RECYCLERVIEW ---
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

// --- IMPORTS DAS CLASSES DO PROJETO AURORA ---
import com.equipe1.aurora.R;
import com.equipe1.aurora.databinding.FragmentMapaBinding;
import com.equipe1.aurora.domain.model.AreaRisco;
import com.equipe1.aurora.domain.model.BairroPoligono;
import com.equipe1.aurora.domain.model.Circulo;
import com.equipe1.aurora.domain.model.Membro;
import com.equipe1.aurora.domain.model.OcorrenciaCrime;
import com.equipe1.aurora.ui.circulos.MembroAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;

// --- IMPORTS DO MAPA (OSMDROID) E JSON ---
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapaFragment extends Fragment {

    // =========================================================================
    // 1. GERENCIAMENTO DE BINDING, VIEWMODEL E ESTRUTURAS DO MAPA
    // =========================================================================
    private FragmentMapaBinding vinculacaoLayout;
    private MapaViewModel viewModel;
    private MapView mapa;

    // Marcador de posição atual (GPS) e marcador de destino selecionado pelo usuário
    private MyLocationNewOverlay marcadorMinhaLocalizacao;
    private Marker marcadorDestinoSelecionado;

    // Coleções para armazenamento das camadas (overlays) desenhadas no mapa
    private final List<Polyline> linhasDesenhadasDaRota = new ArrayList<>();
    private final List<JSONObject> listaDadosRotasJson = new ArrayList<>();
    private final List<Overlay> camadasRiscosList = new ArrayList<>();
    private final List<Marker> marcadoresMembros = new ArrayList<>();

    // Estado da navegação em tempo real
    private boolean estaNavegandoAtivamente = false;
    private BottomSheetBehavior<View> comportamentoPainelInferior;

    // Executores assíncronos e manipuladores da thread principal
    private ExecutorService executor;
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());
    private long idRequisicaoEndereco = 0;

    // Callback para o botão voltar do dispositivo
    private OnBackPressedCallback voltarParaCirculosCallback;

    // Adaptador e coleção para lista de círculos e contatos
    private MembroAdapter adaptadorMembros;
    private final List<Circulo> listaCirculos = new ArrayList<>();

    // Gerenciador do contrato de permissões em tempo de execução
    private final ActivityResultLauncher<String[]> lancadorDePermissaoLocalizacao =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
                Boolean permissaoPrecisa = resultado.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean permissaoAproximada = resultado.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if ((permissaoPrecisa != null && permissaoPrecisa) || (permissaoAproximada != null && permissaoAproximada)) {
                    ativarLocalizacaoUsuario();
                    Toast.makeText(getContext(), "Permissão de localização concedida.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Permissão de localização negada.", Toast.LENGTH_SHORT).show();
                }
            });

    // =========================================================================
    // 2. CICLO DE VIDA DO FRAGMENT
    // =========================================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inicializa configurações do OSMDroid
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        // Vincula o ViewModel com escopo na Activity para compartilhamento de dados
        viewModel = new ViewModelProvider(requireActivity()).get(MapaViewModel.class);
        executor = Executors.newSingleThreadExecutor();

        // Configuração do ViewBinding e layout raiz
        vinculacaoLayout = FragmentMapaBinding.inflate(inflater, container, false);
        View raiz = vinculacaoLayout.getRoot();
        mapa = vinculacaoLayout.map;

        // Configuração inicial do BottomSheetBehavior (Painel inferior)
        comportamentoPainelInferior = BottomSheetBehavior.from(vinculacaoLayout.bottomSheet);
        comportamentoPainelInferior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Registro do callback do botão Voltar
        voltarParaCirculosCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                voltarParaCirculos();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), voltarParaCirculosCallback);

        // Parametrização básica do motor do mapa
        mapa.setTileSource(TileSourceFactory.MAPNIK);
        mapa.setMultiTouchControls(true);
        mapa.getController().setZoom(17.0);

        // Inicialização de componentes, listeners e observadores
        configurarCliqueNoMapa();
        configurarOuvinteBarraPesquisa();
        configurarOuvintesBotoes();
        configurarCirculos();
        configurarAcessibilidade();

        configurarObservadoresViewModel();
        processarArgumentosEsp32();
        processarBuscaRecebida();
        checarPermissoesELocalizar();

        // Solicita o carregamento inicial das camadas ao ViewModel
        viewModel.carregarTodasAsCamadasDeRisco();

        return raiz;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapa != null) mapa.onResume();
        if (marcadorMinhaLocalizacao != null) marcadorMinhaLocalizacao.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapa != null) mapa.onPause();
        if (marcadorMinhaLocalizacao != null) marcadorMinhaLocalizacao.disableMyLocation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Libera recursos e encerra threads para evitar memory leaks
        if (executor != null && !executor.isShutdown()) executor.shutdownNow();
        if (marcadorMinhaLocalizacao != null) marcadorMinhaLocalizacao.disableMyLocation();
        if (mapa != null) mapa.onDetach();
        vinculacaoLayout = null;
    }

    // =========================================================================
    // 3. CONFIGURAÇÃO DOS OBSERVADORES DO VIEWMODEL (MVVM)
    // =========================================================================

    private void configurarObservadoresViewModel() {
        // Observador da localização do rastreador ESP32
        viewModel.getPontoEsp32().observe(getViewLifecycleOwner(), pontoEsp32 -> {
            if (pontoEsp32 != null && mapa != null) {
                if (marcadorMinhaLocalizacao != null) {
                    marcadorMinhaLocalizacao.disableFollowLocation();
                }
                selecionarLocalEsp32(pontoEsp32);
            }
        });

        // Observadores das listas de camadas de risco fornecidas pela API
        viewModel.getListaRiscos().observe(getViewLifecycleOwner(), this::desenharAreasDeRisco);
        viewModel.getListaBairros().observe(getViewLifecycleOwner(), this::desenharPoligonosBairros);
        viewModel.getListaCrimes().observe(getViewLifecycleOwner(), this::desenharOcorrenciasCrimes);

        // Observador para alternar a exibição/ocultação das camadas
        viewModel.getExibirAreasRisco().observe(getViewLifecycleOwner(), visivel -> {
            if (visivel) {
                vinculacaoLayout.btnToggleRiscos.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_dark));
                viewModel.carregarTodasAsCamadasDeRisco();
            } else {
                vinculacaoLayout.btnToggleRiscos.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
                limparCamadasDeRiscoDoMapa();
            }
        });
    }

    // =========================================================================
    // 4. INTEGRAÇÃO E NAVEGAÇÃO VIA DISPOSITIVO ESP32
    // =========================================================================

    // Processa coordenadas recebidas via argumentos do Fragment
    private void processarArgumentosEsp32() {
        if (getArguments() != null && getArguments().containsKey("LATITUDE_ESP32")) {
            double lat = getArguments().getDouble("LATITUDE_ESP32", 0.0);
            double lng = getArguments().getDouble("LONGITUDE_ESP32", 0.0);

            if (lat != 0.0 && lng != 0.0) {
                GeoPoint pontoEsp32 = new GeoPoint(lat, lng);
                viewModel.setPontoEsp32(pontoEsp32);
            }
        }
    }

    // Move o foco do mapa e abre a rota para a localização do ESP32
    private void selecionarLocalEsp32(GeoPoint pontoEsp32) {
        estaNavegandoAtivamente = false;
        viewModel.setEstaNavegando(false);

        limparLinhasDesenhadasNoMapa();
        adicionarOuMoverMarcadorDeDestino(pontoEsp32);

        if (marcadorDestinoSelecionado != null) {
            marcadorDestinoSelecionado.setTitle("Localizador Aurora ESP32");
        }

        mostrarModoLocal();
        obterEnderecoGeocodificado(pontoEsp32);
        executarCalculoDeRota();

        vinculacaoLayout.tvNomeDoLocal.setText("Dispositivo Aurora (ESP32)");
        comportamentoPainelInferior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    // =========================================================================
    // 5. OUVINTES DE EVENTOS E BOTÕES DA INTERFACE
    // =========================================================================

    private void configurarOuvintesBotoes() {
        vinculacaoLayout.btnMinhaLocalizacao.setOnClickListener(v -> centralizarNaLocalizacaoAtual());
        vinculacaoLayout.btnNavigate.setOnClickListener(v -> executarCalculoDeRota());
        vinculacaoLayout.btnVoltarCirculos.setOnClickListener(v -> voltarParaCirculos());
        vinculacaoLayout.btnToggleRiscos.setOnClickListener(v -> viewModel.alternarExibicaoRiscos());
    }

    // Intercepta toques simples diretamente na área do mapa
    private void configurarCliqueNoMapa() {
        MapEventsReceiver receptorEventosMapa = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint pontoTocado) {
                selecionarLocal(pontoTocado);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint pontoTocado) {
                return false;
            }
        };
        mapa.getOverlays().add(new MapEventsOverlay(receptorEventosMapa));
    }

    // Ação ao tocar em qualquer coordenada do mapa
    private void selecionarLocal(GeoPoint ponto) {
        estaNavegandoAtivamente = false;
        viewModel.setEstaNavegando(false);
        viewModel.setPontoSelecionado(ponto);

        limparLinhasDesenhadasNoMapa();

        if (marcadorMinhaLocalizacao != null) {
            marcadorMinhaLocalizacao.disableFollowLocation();
        }

        adicionarOuMoverMarcadorDeDestino(ponto);
        mostrarModoLocal();
        obterEnderecoGeocodificado(ponto);
        executarCalculoDeRota();

        comportamentoPainelInferior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    // Configura a pesquisa por texto na barra de busca
    private void configurarOuvinteBarraPesquisa() {
        vinculacaoLayout.etPesquisa.setOnEditorActionListener((view, acaoId, evento) -> {
            if (acaoId == EditorInfo.IME_ACTION_SEARCH || (evento != null && evento.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                buscarLocalizacaoPeloTexto(vinculacaoLayout.etPesquisa.getText().toString());
                return true;
            }
            return false;
        });
    }

    // =========================================================================
    // 6. CONTROLE E NAVEGAÇÃO DO PAINEL INFERIOR (BOTTOM SHEET)
    // =========================================================================

    // Exibe as informações do local selecionado e esconde os círculos
    private void mostrarModoLocal() {
        vinculacaoLayout.layoutCirculos.setVisibility(View.GONE);
        vinculacaoLayout.layoutLocal.setVisibility(View.VISIBLE);
        vinculacaoLayout.tvDistanciaTempo.setText("Calculando rota...");
        voltarParaCirculosCallback.setEnabled(true);
    }

    // Restaura a visualização padrão dos Círculos de Segurança
    private void voltarParaCirculos() {
        estaNavegandoAtivamente = false;
        viewModel.setEstaNavegando(false);
        idRequisicaoEndereco++;
        voltarParaCirculosCallback.setEnabled(false);
        limparLinhasDesenhadasNoMapa();

        if (marcadorDestinoSelecionado != null) {
            mapa.getOverlays().remove(marcadorDestinoSelecionado);
            marcadorDestinoSelecionado = null;
            mapa.invalidate();
        }

        vinculacaoLayout.layoutLocal.setVisibility(View.GONE);
        vinculacaoLayout.layoutCirculos.setVisibility(View.VISIBLE);
        comportamentoPainelInferior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    // =========================================================================
    // 7. CÍRCULOS DE CONTATOS E MEMBROS DA FAMÍLIA
    // =========================================================================

    private void configurarCirculos() {
        adaptadorMembros = new MembroAdapter(membro -> {
            if (marcadorMinhaLocalizacao != null) marcadorMinhaLocalizacao.disableFollowLocation();
            mapa.getController().animateTo(new GeoPoint(membro.lat, membro.lon));
            comportamentoPainelInferior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });
        vinculacaoLayout.rvMembros.setLayoutManager(new LinearLayoutManager(requireContext()));
        vinculacaoLayout.rvMembros.setAdapter(adaptadorMembros);

        carregarCirculosDeExemplo();

        float densidade = getResources().getDisplayMetrics().density;
        for (Circulo c : listaCirculos) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setText(c.nome);
            chip.setCheckable(true);
            chip.setTag(c);
            chip.setMinHeight((int) (48 * densidade));

            vinculacaoLayout.chipGroupCirculos.addView(chip);
        }

        vinculacaoLayout.chipGroupCirculos.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (ids.isEmpty()) return;
            Chip chip = grupo.findViewById(ids.get(0));
            selecionarCirculo((Circulo) chip.getTag());
        });

        if (vinculacaoLayout.chipGroupCirculos.getChildCount() > 0) {
            ((Chip) vinculacaoLayout.chipGroupCirculos.getChildAt(0)).setChecked(true);
        }
    }

    // Renderiza os membros do círculo selecionado no mapa e na lista
    private void selecionarCirculo(Circulo c) {
        adaptadorMembros.submit(c.membros);
        vinculacaoLayout.tvCirculoVazio.setVisibility(c.membros.isEmpty() ? View.VISIBLE : View.GONE);

        for (Marker m : marcadoresMembros) mapa.getOverlays().remove(m);
        marcadoresMembros.clear();

        for (Membro mb : c.membros) {
            Marker m = new Marker(mapa);
            m.setPosition(new GeoPoint(mb.lat, mb.lon));
            m.setTitle(mb.nome);
            m.setSubDescription(mb.status);
            marcadoresMembros.add(m);
            mapa.getOverlays().add(m);
        }
        mapa.invalidate();
    }

    // Carga inicial de círculos para demonstração
    private void carregarCirculosDeExemplo() {
        Circulo familia = new Circulo("1", "Família");
        familia.membros.add(new Membro("Maria Souza", "Em casa · há 2 min", 85, -23.5489, -46.6388));
        familia.membros.add(new Membro("João Souza", "A caminho · há 1 min", 52, -23.5614, -46.6559));
        Circulo amigos = new Circulo("2", "Amigos da escola");
        amigos.membros.add(new Membro("Ana Lima", "Na Etec · há 5 min", 40, -23.5329, -46.6395));
        listaCirculos.add(familia);
        listaCirculos.add(amigos);
        viewModel.setListaCirculos(listaCirculos);
    }

    // =========================================================================
    // 8. CONFIGURAÇÕES DE ACESSIBILIDADE
    // =========================================================================

    private void configurarAcessibilidade() {
        ViewCompat.addAccessibilityAction(mapa, "Selecionar o ponto no centro do mapa", (v, args) -> {
            IGeoPoint centro = mapa.getMapCenter();
            selecionarLocal(new GeoPoint(centro.getLatitude(), centro.getLongitude()));
            return true;
        });
    }

    // Atualiza a descrição audível para os leitores de tela
    private void atualizarDescricaoAcessivel() {
        if (vinculacaoLayout == null || marcadorDestinoSelecionado == null) return;
        GeoPoint p = marcadorDestinoSelecionado.getPosition();
        String desc = vinculacaoLayout.tvNomeDoLocal.getText() + ". "
                + vinculacaoLayout.tvLocalizacaoEstado.getText() + ". "
                + vinculacaoLayout.tvDistanciaTempo.getText().toString().replace("(", ", ").replace(")", "") + ". "
                + String.format(new Locale("pt", "BR"), "Latitude %.5f, longitude %.5f", p.getLatitude(), p.getLongitude());
        vinculacaoLayout.blocoInfoLocal.setContentDescription(desc);
    }

    // =========================================================================
    // 9. SERVIÇOS DE LOCALIZAÇÃO E GPS DO DISPOSITIVO
    // =========================================================================

    private void checarPermissoesELocalizar() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            ativarLocalizacaoUsuario();
        } else {
            lancadorDePermissaoLocalizacao.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    // Ativa a camada de geolocalização do usuário no OSMDroid
    private void ativarLocalizacaoUsuario() {
        if (getContext() == null || mapa == null) return;

        GpsMyLocationProvider provedorGps = new GpsMyLocationProvider(requireContext());
        marcadorMinhaLocalizacao = new MyLocationNewOverlay(provedorGps, mapa) {
            @Override
            public void onLocationChanged(android.location.Location novaLocalizacao, org.osmdroid.views.overlay.mylocation.IMyLocationProvider fonte) {
                super.onLocationChanged(novaLocalizacao, fonte);

                if (novaLocalizacao != null && estaNavegandoAtivamente && marcadorDestinoSelecionado != null) {
                    GeoPoint coordenadaAtual = new GeoPoint(novaLocalizacao.getLatitude(), novaLocalizacao.getLongitude());
                    GeoPoint coordenadaDestino = marcadorDestinoSelecionado.getPosition();

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (vinculacaoLayout == null || marcadorDestinoSelecionado == null) return;
                            mapa.getController().animateTo(coordenadaAtual);
                            atualizarInformacoesDeTrajetoEmTempoReal(coordenadaAtual, coordenadaDestino);
                        });
                    }
                }
            }
        };

        marcadorMinhaLocalizacao.enableMyLocation();
        marcadorMinhaLocalizacao.enableFollowLocation();

        marcadorMinhaLocalizacao.runOnFirstFix(() -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (mapa == null) return;
                    if ((getArguments() == null || !getArguments().containsKey("search_query"))
                            && viewModel.getPontoEsp32().getValue() == null) {
                        GeoPoint posicaoCapturada = marcadorMinhaLocalizacao.getMyLocation();
                        if (posicaoCapturada != null) {
                            mapa.getController().animateTo(posicaoCapturada);
                        }
                    }
                });
            }
        });

        mapa.getOverlays().add(marcadorMinhaLocalizacao);
        mapa.invalidate();
    }

    // Centraliza a câmera nas coordenadas atuais do GPS
    private void centralizarNaLocalizacaoAtual() {
        if (marcadorMinhaLocalizacao != null && marcadorMinhaLocalizacao.getMyLocation() != null) {
            GeoPoint posicaoUsuario = marcadorMinhaLocalizacao.getMyLocation();
            mapa.getController().animateTo(posicaoUsuario);
            marcadorMinhaLocalizacao.enableFollowLocation();
            voltarParaCirculos();
        } else {
            Toast.makeText(getContext(), "Buscando sinal de GPS...", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // 10. PESQUISA DE ENDEREÇOS E GEOCODIFICAÇÃO
    // =========================================================================

    private void buscarLocalizacaoPeloTexto(String textoPesquisado) {
        if (textoPesquisado.isEmpty()) return;

        final Context ctx = requireContext();
        executor.execute(() -> {
            Geocoder conversorTextoParaMapa = new Geocoder(ctx, Locale.getDefault());
            try {
                List<Address> enderecosEncontrados = conversorTextoParaMapa.getFromLocationName(textoPesquisado, 1);

                handlerPrincipal.post(() -> {
                    if (!isAdded() || vinculacaoLayout == null) return;

                    if (enderecosEncontrados != null && !enderecosEncontrados.isEmpty()) {
                        Address melhorEndereco = enderecosEncontrados.get(0);
                        GeoPoint coordenada = new GeoPoint(melhorEndereco.getLatitude(), melhorEndereco.getLongitude());

                        if (marcadorMinhaLocalizacao != null) {
                            marcadorMinhaLocalizacao.disableFollowLocation();
                        }

                        adicionarOuMoverMarcadorDeDestino(coordenada);
                        mostrarModoLocal();
                        idRequisicaoEndereco++;
                        aplicarEnderecoNaTela(extrairEndereco(melhorEndereco), coordenada);

                        executarCalculoDeRota();
                        comportamentoPainelInferior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    } else {
                        Toast.makeText(getContext(), "Local não encontrado", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                handlerPrincipal.post(() ->
                        Toast.makeText(getContext(), "Erro ao buscar local", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Processa parâmetros de busca repassados de outras Fragments
    private void processarBuscaRecebida() {
        if (getArguments() != null && getArguments().containsKey("search_query")) {
            String textoRecebido = getArguments().getString("search_query");
            if (textoRecebido != null && !textoRecebido.isEmpty()) {
                vinculacaoLayout.etPesquisa.setText(textoRecebido);
                buscarLocalizacaoPeloTexto(textoRecebido);
            }
        }
    }

    // =========================================================================
    // 11. CÁLCULO E DESENHO DE ROTAS (API OSRM)
    // =========================================================================

    private void executarCalculoDeRota() {
        if (marcadorDestinoSelecionado == null) {
            Toast.makeText(getContext(), "Selecione um ponto no mapa primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }

        GeoPoint coordenadaDestino = marcadorDestinoSelecionado.getPosition();
        GeoPoint coordenadaOrigem = (marcadorMinhaLocalizacao != null) ? marcadorMinhaLocalizacao.getMyLocation() : null;

        if (coordenadaOrigem == null) {
            vinculacaoLayout.tvDistanciaTempo.setText("Aguardando sinal do GPS...");
            Toast.makeText(getContext(), "Aguardando sinal do GPS...", Toast.LENGTH_SHORT).show();
            return;
        }

        vinculacaoLayout.tvDistanciaTempo.setText("Calculando rota...");
        baixarETracarRotaInternet(coordenadaOrigem, coordenadaDestino);
    }

    // Requisição assíncrona HTTP ao serviço de roteamento OSRM
    private void baixarETracarRotaInternet(GeoPoint origem, GeoPoint destino) {
        executor.execute(() -> {
            final List<JSONObject> rotasRecebidas = new ArrayList<>();

            try {
                String enderecoRequisicao = String.format(Locale.US,
                        "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&alternatives=true&steps=true",
                        origem.getLongitude(), origem.getLatitude(),
                        destino.getLongitude(), destino.getLatitude());

                URL linkUrl = new URL(enderecoRequisicao);
                HttpURLConnection conexao = (HttpURLConnection) linkUrl.openConnection();
                conexao.setRequestMethod("GET");
                conexao.setRequestProperty("User-Agent", Configuration.getInstance().getUserAgentValue());
                conexao.setConnectTimeout(8000);

                if (conexao.getResponseCode() == 200) {
                    BufferedReader leitorTextos = new BufferedReader(new InputStreamReader(conexao.getInputStream()));
                    StringBuilder respostaServidor = new StringBuilder();
                    String linhaTexto;
                    while ((linhaTexto = leitorTextos.readLine()) != null) {
                        respostaServidor.append(linhaTexto);
                    }
                    leitorTextos.close();

                    JSONObject objetoJsonCompleto = new JSONObject(respostaServidor.toString());
                    JSONArray conjuntoRotas = objetoJsonCompleto.getJSONArray("routes");
                    for (int i = 0; i < conjuntoRotas.length(); i++) {
                        rotasRecebidas.add(conjuntoRotas.getJSONObject(i));
                    }
                }
            } catch (Exception e) {
                Log.e("MAPA_ERRO", "Erro no cálculo da rota", e);
            }

            handlerPrincipal.post(() -> {
                if (!isAdded() || vinculacaoLayout == null) return;

                listaDadosRotasJson.clear();
                listaDadosRotasJson.addAll(rotasRecebidas);

                if (!listaDadosRotasJson.isEmpty()) {
                    desenharRotasRecebidasNoMapa(0);
                    estaNavegandoAtivamente = true;
                    viewModel.setEstaNavegando(true);

                    if (marcadorMinhaLocalizacao != null) {
                        marcadorMinhaLocalizacao.enableFollowLocation();
                    }
                } else {
                    vinculacaoLayout.tvDistanciaTempo.setText("Rota indisponível");
                    Toast.makeText(getContext(), "Não foi possível encontrar rotas.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Limpa do mapa as linhas desenhadas referente às rotas ativas
    private void limparLinhasDesenhadasNoMapa() {
        for (Polyline linha : linhasDesenhadasDaRota) {
            mapa.getOverlays().remove(linha);
        }
        linhasDesenhadasDaRota.clear();
        mapa.invalidate();
    }

    // Plota as polilinhas no mapa e destaca a rota selecionada
    private void desenharRotasRecebidasNoMapa(int indiceRotaSelecionada) {
        limparLinhasDesenhadasNoMapa();

        for (int i = 0; i < listaDadosRotasJson.size(); i++) {
            try {
                JSONObject informacaoRota = listaDadosRotasJson.get(i);
                JSONArray coordenadasRota = informacaoRota.getJSONObject("geometry").getJSONArray("coordinates");

                List<GeoPoint> pontosDaLinha = new ArrayList<>();
                for (int j = 0; j < coordenadasRota.length(); j++) {
                    JSONArray parDePontos = coordenadasRota.getJSONArray(j);
                    pontosDaLinha.add(new GeoPoint(parDePontos.getDouble(1), parDePontos.getDouble(0)));
                }

                Polyline linhaColorida = new Polyline(mapa);
                linhaColorida.setPoints(pontosDaLinha);

                final int indiceDestaRota = i;
                boolean ehARotaPrincipal = (i == indiceRotaSelecionada);

                if (ehARotaPrincipal) {
                    linhaColorida.getOutlinePaint().setColor(Color.parseColor("#6C5CE7"));
                    linhaColorida.getOutlinePaint().setStrokeWidth(16f);

                    double distanciaEmMetros = informacaoRota.getDouble("distance");
                    double duracaoEmSegundos = informacaoRota.getDouble("duration");

                    vinculacaoLayout.tvDistanciaTempo.setText(formatarTextoTempoEDistancia(duracaoEmSegundos, distanciaEmMetros));
                    atualizarDescricaoAcessivel();

                    escreverPassosDaNavegacaoNaTela(informacaoRota);
                } else {
                    linhaColorida.getOutlinePaint().setColor(Color.parseColor("#64748B"));
                    linhaColorida.getOutlinePaint().setStrokeWidth(10f);
                }

                linhaColorida.setOnClickListener((linhaTrocada, vistaDoMapa, eventoPosicao) -> {
                    desenharRotasRecebidasNoMapa(indiceDestaRota);
                    Toast.makeText(getContext(), "Rota " + (indiceDestaRota + 1) + " selecionada", Toast.LENGTH_SHORT).show();
                    return true;
                });

                linhasDesenhadasDaRota.add(linhaColorida);
                mapa.getOverlays().add(linhaColorida);

            } catch (Exception e) {
                Log.e("MAPA_ERRO", "Erro ao desenhar rota", e);
            }
        }
        mapa.invalidate();
    }

    // Recalcula o progresso da viagem durante o deslocamento
    private void atualizarInformacoesDeTrajetoEmTempoReal(GeoPoint coordenadaAtual, GeoPoint coordenadaDestino) {
        double distanciaQueFalta = coordenadaAtual.distanceToAsDouble(coordenadaDestino);
        double tempoEstimadoRestante = distanciaQueFalta / 8.3;

        vinculacaoLayout.tvDistanciaTempo.setText(formatarTextoTempoEDistancia(tempoEstimadoRestante, distanciaQueFalta));

        if (distanciaQueFalta < 15.0) {
            estaNavegandoAtivamente = false;
            viewModel.setEstaNavegando(false);
            vinculacaoLayout.tvDistanciaTempo.setText("Você chegou ao seu destino! 🎉");
            Toast.makeText(getContext(), "Você chegou ao seu destino!", Toast.LENGTH_LONG).show();
        }
        atualizarDescricaoAcessivel();
    }

    // =========================================================================
    // 12. RENDERIZAÇÃO DAS CAMADAS DE RISCO, BAIRROS E CRIMES
    // =========================================================================

    // Remove todas as camadas de segurança do mapa
    private void limparCamadasDeRiscoDoMapa() {
        for (Overlay overlay : camadasRiscosList) {
            mapa.getOverlays().remove(overlay);
        }
        camadasRiscosList.clear();
        mapa.invalidate();
    }

    // Plota círculos concêntricos e pontos de risco no mapa
    private void desenharAreasDeRisco(List<AreaRisco> listaRiscos) {
        if (listaRiscos == null || !Boolean.TRUE.equals(viewModel.getExibirAreasRisco().getValue())) return;

        for (AreaRisco risco : listaRiscos) {
            try {
                if (risco.getLatitude() == 0.0 || risco.getLongitude() == 0.0) continue;

                GeoPoint ponto = new GeoPoint(risco.getLatitude(), risco.getLongitude());

                Marker marker = new Marker(mapa);
                marker.setPosition(ponto);
                marker.setTitle(risco.getNomeArea() + (risco.getTipoCrime() != null ? " (" + risco.getTipoCrime() + ")" : ""));
                marker.setSnippet(risco.getDescricaoArea());
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                configurarIconePorCrime(marker, risco.getTipoCrime());

                mapa.getOverlays().add(marker);
                camadasRiscosList.add(marker);

                Polygon circle = new Polygon(mapa);
                double raio = risco.getRaioMetros() > 0 ? risco.getRaioMetros() : 100.0;
                circle.setPoints(Polygon.pointsAsCircle(ponto, raio));

                String nivel = risco.getNivelRisco() != null ? risco.getNivelRisco().toUpperCase() : "MEDIO";
                if (nivel.contains("ALTO") || nivel.contains("CRITICO")) {
                    circle.setFillColor(Color.argb(70, 255, 0, 0));
                    circle.setStrokeColor(Color.RED);
                } else {
                    circle.setFillColor(Color.argb(70, 255, 235, 59));
                    circle.setStrokeColor(Color.parseColor("#FBC02D"));
                }
                circle.setStrokeWidth(2f);

                mapa.getOverlays().add(circle);
                camadasRiscosList.add(circle);

            } catch (Exception e) {
                Log.e("MAPA_ERRO", "Erro ao desenhar área de risco", e);
            }
        }
        mapa.invalidate();
    }

    // Desenha as fronteiras poligonais dos bairros com cores indicativas de perigo
    private void desenharPoligonosBairros(List<BairroPoligono> listaBairros) {
        if (listaBairros == null || !Boolean.TRUE.equals(viewModel.getExibirAreasRisco().getValue())) return;

        for (BairroPoligono bairro : listaBairros) {
            List<GeoPoint> pontosGeo = bairro.getCoordenadasVertices();
            if (pontosGeo == null || pontosGeo.isEmpty()) continue;

            Polygon poligono = new Polygon(mapa);
            poligono.setPoints(pontosGeo);
            poligono.setTitle("Bairro: " + bairro.getNomeBairro() + " (Risco: " + bairro.getNivelRisco() + ")");

            String nivelRisco = bairro.getNivelRisco() != null ? bairro.getNivelRisco().toUpperCase() : "MEDIO";
            switch (nivelRisco) {
                case "ALTO":
                    poligono.setFillColor(Color.parseColor("#4DFF0000"));
                    poligono.setStrokeColor(Color.parseColor("#FF0000"));
                    break;
                case "MEDIO":
                    poligono.setFillColor(Color.parseColor("#4DFFFF00"));
                    poligono.setStrokeColor(Color.parseColor("#D4AC0D"));
                    break;
                default:
                    poligono.setFillColor(Color.parseColor("#4D00FF00"));
                    poligono.setStrokeColor(Color.parseColor("#00FF00"));
                    break;
            }

            poligono.setStrokeWidth(3f);
            mapa.getOverlays().add(0, poligono);
            camadasRiscosList.add(poligono);
        }
        mapa.invalidate();
    }

    // Adiciona marcadores específicos para locais de crimes reportados
    private void desenharOcorrenciasCrimes(List<OcorrenciaCrime> listaCrimes) {
        if (listaCrimes == null || !Boolean.TRUE.equals(viewModel.getExibirAreasRisco().getValue())) return;

        for (OcorrenciaCrime crime : listaCrimes) {
            GeoPoint ponto = new GeoPoint(crime.getLatitude(), crime.getLongitude());

            Marker marker = new Marker(mapa);
            marker.setPosition(ponto);
            marker.setTitle(crime.getTipoCrime());
            marker.setSnippet(crime.getDescricao());
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            configurarIconePorCrime(marker, crime.getTipoCrime());

            mapa.getOverlays().add(marker);
            camadasRiscosList.add(marker);
        }
        mapa.invalidate();
    }

    // Configura o ícone gráfico do marcador dependendo da categoria de crime
    private void configurarIconePorCrime(Marker marker, String tipoCrime) {
        if (tipoCrime == null || !isAdded() || getContext() == null) return;
        String tipo = tipoCrime.toLowerCase();

        if (tipo.contains("roubo") || tipo.contains("assalto")) {
            marker.setIcon(ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_myplaces));
        } else if (tipo.contains("furto")) {
            marker.setIcon(ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_info_details));
        } else {
            marker.setIcon(ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_dialog_alert));
        }
    }

    // =========================================================================
    // 13. PASSO A PASSO DA NAVEGAÇÃO E ÍCONES DE MANOBRA
    // =========================================================================

    // Monta o texto legível de instruções de navegação
    private void escreverPassosDaNavegacaoNaTela(JSONObject jsonCompletoRota) {
        try {
            SpannableStringBuilder textoComOsPassos = new SpannableStringBuilder();
            JSONArray blocosDaRota = jsonCompletoRota.getJSONArray("legs");

            if (blocosDaRota.length() > 0) {
                JSONArray passos = blocosDaRota.getJSONObject(0).getJSONArray("steps");
                textoComOsPassos.append("🟢 Ponto de Partida\n\n");

                for (int i = 0; i < passos.length(); i++) {
                    JSONObject passo = passos.getJSONObject(i);
                    String nomeRua = passo.optString("name", "");
                    double metros = passo.optDouble("distance", 0);

                    JSONObject maneuver = passo.optJSONObject("maneuver");
                    String modifier = (maneuver != null) ? maneuver.optString("modifier", "") : "";
                    String type = (maneuver != null) ? maneuver.optString("type", "") : "";

                    String iconeManobra = obterIconeManobra(type, modifier);
                    String distStr = metros < 1000 ? String.format(Locale.getDefault(), "%.0f m", metros) : String.format(Locale.getDefault(), "%.1f km", metros / 1000.0);

                    textoComOsPassos.append(iconeManobra).append(" ");
                    if (!nomeRua.isEmpty()) {
                        textoComOsPassos.append("Siga por ").append(nomeRua);
                    } else {
                        textoComOsPassos.append("Continue no trajeto");
                    }
                    textoComOsPassos.append(" (").append(distStr).append(")\n\n");
                }
                textoComOsPassos.append("🔴 Chegada ao Destino");
                vinculacaoLayout.tvEtapasDetalhes.setText(textoComOsPassos);
            }
        } catch (Exception e) {
            Log.e("MAPA_ERRO", "Erro ao formatar etapas", e);
            vinculacaoLayout.tvEtapasDetalhes.setText("• Toque no botão de rota para ver as instruções detalhadas.");
        }
    }

    // Associa direções do OSRM aos seus respectivos ícones
    private String obterIconeManobra(String type, String modifier) {
        if (modifier.contains("right")) return "↗️ Virar à direita";
        if (modifier.contains("left")) return "↖️ Virar à esquerda";
        if (modifier.contains("slight right")) return "↗️ Mantenha-se à direita";
        if (modifier.contains("slight left")) return "↖️ Mantenha-se à esquerda";
        if (modifier.contains("sharp right")) return "↪ Curva acentuada à direita";
        if (modifier.contains("sharp left")) return "↩️ Curva acentuada à esquerda";
        if (modifier.contains("uturn")) return "🔄 Faça o retorno";
        return "⬆️ Siga em frente";
    }

    // =========================================================================
    // 14. MÉTODOS AUXILIARES DE SUPORTE E FORMATAÇÕES
    // =========================================================================

    private void adicionarOuMoverMarcadorDeDestino(GeoPoint coordenada) {
        if (marcadorDestinoSelecionado == null) {
            marcadorDestinoSelecionado = new Marker(mapa);
            marcadorDestinoSelecionado.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marcadorDestinoSelecionado.setInfoWindow(null);
            mapa.getOverlays().add(marcadorDestinoSelecionado);
        }
        marcadorDestinoSelecionado.setPosition(coordenada);
        mapa.getController().animateTo(coordenada);
        mapa.invalidate();
    }

    private void obterEnderecoGeocodificado(GeoPoint coordenada) {
        vinculacaoLayout.tvCoordenadas.setText(formatarCoordenadas(coordenada));
        vinculacaoLayout.tvNomeDoLocal.setText("Buscando endereço...");
        vinculacaoLayout.tvLocalizacaoEstado.setText("");
        atualizarDescricaoAcessivel();

        final long id = ++idRequisicaoEndereco;
        final Context ctx = requireContext();

        executor.execute(() -> {
            String[] endereco = {"Local selecionado", "Endereço indisponível"};
            try {
                Geocoder descodificador = new Geocoder(ctx, new Locale("pt", "BR"));
                List<Address> locais = descodificador.getFromLocation(coordenada.getLatitude(), coordenada.getLongitude(), 1);
                if (locais != null && !locais.isEmpty()) {
                    endereco = extrairEndereco(locais.get(0));
                }
            } catch (IOException ignorado) {
                endereco = new String[]{"Local selecionado", "Sem conexão para buscar o endereço"};
            }

            final String[] resultado = endereco;
            handlerPrincipal.post(() -> {
                if (!isAdded() || vinculacaoLayout == null || id != idRequisicaoEndereco) return;
                aplicarEnderecoNaTela(resultado, coordenada);
            });
        });
    }

    private void aplicarEnderecoNaTela(String[] endereco, GeoPoint coordenada) {
        vinculacaoLayout.tvNomeDoLocal.setText(endereco[0]);
        vinculacaoLayout.tvLocalizacaoEstado.setText(endereco[1]);
        vinculacaoLayout.tvCoordenadas.setText(formatarCoordenadas(coordenada));
        atualizarDescricaoAcessivel();
    }

    private String[] extrairEndereco(Address a) {
        String rua = a.getThoroughfare();
        if (rua != null) {
            if (a.getSubThoroughfare() != null) rua += ", " + a.getSubThoroughfare();
        } else {
            rua = a.getFeatureName() != null ? a.getFeatureName() : "Local sem nome";
        }

        StringBuilder sb = new StringBuilder();
        if (a.getSubLocality() != null) sb.append(a.getSubLocality()).append(", ");
        if (a.getLocality() != null) sb.append(a.getLocality());
        String uf = ufDe(a.getAdminArea());
        if (!uf.isEmpty()) sb.append(" - ").append(uf);

        return new String[]{rua, sb.length() > 0 ? sb.toString() : "Localização aproximada"};
    }

    private static final String[][] UFS = {
            {"Acre","AC"},{"Alagoas","AL"},{"Amapá","AP"},{"Amazonas","AM"},{"Bahia","BA"},{"Ceará","CE"},
            {"Distrito Federal","DF"},{"Espírito Santo","ES"},{"Goiás","GO"},{"Maranhão","MA"},
            {"Mato Grosso","MT"},{"Mato Grosso do Sul","MS"},{"Minas Gerais","MG"},{"Pará","PA"},
            {"Paraíba","PB"},{"Paraná","PR"},{"Pernambuco","PE"},{"Piauí","PI"},{"Rio de Janeiro","RJ"},
            {"Rio Grande do Norte","RN"},{"Rio Grande do Sul","RS"},{"Rondônia","RO"},{"Roraima","RR"},
            {"Santa Catarina","SC"},{"São Paulo","SP"},{"Sergipe","SE"},{"Tocantins","TO"}};

    private static String ufDe(String estado) {
        if (estado == null) return "";
        if (estado.length() == 2) return estado.toUpperCase();
        for (String[] par : UFS) if (par[0].equalsIgnoreCase(estado)) return par[1];
        return estado;
    }

    private String formatarCoordenadas(GeoPoint p) {
        return String.format(Locale.US, "Lat: %.5f | Lon: %.5f", p.getLatitude(), p.getLongitude());
    }

    private String formatarTextoTempoEDistancia(double segundos, double metros) {
        int minutos = (int) Math.ceil(segundos / 60.0);
        String textoTempo = minutos < 60 ? minutos + " min" : (minutos / 60) + " h " + (minutos % 60) + " min";
        String textoDistancia = metros < 1000 ? String.format(Locale.getDefault(), "%.0f m", metros) : String.format(Locale.getDefault(), "%.1f km", metros / 1000.0);
        return textoTempo + " (" + textoDistancia + ")";
    }
}