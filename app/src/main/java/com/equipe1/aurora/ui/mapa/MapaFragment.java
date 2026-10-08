package com.equipe1.aurora.ui.mapa;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

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

import com.equipe1.aurora.domain.model.Circulo;
import com.equipe1.aurora.domain.model.Membro;
import com.equipe1.aurora.ui.circulos.MembroAdapter;
import com.equipe1.aurora.R;
import com.equipe1.aurora.databinding.FragmentMapaBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import org.json.JSONArray;
import org.json.JSONObject;

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

/**
 * Fragment responsável pela exibição e interação com o mapa OSMDroid,
 * cálculo de rotas via OSRM e visualização de membros do círculo.
 */
public class MapaFragment extends Fragment {

    // --- ATRIBUTOS E GERENCIAMENTO DE ESTADO ---
    private FragmentMapaBinding vinculacaoLayout;
    private MapaViewModel viewModel;
    private MapView mapa;

    private MyLocationNewOverlay marcadorMinhaLocalizacao;
    private Marker marcadorDestinoSelecionado;

    private final List<Polyline> linhasDesenhadasDaRota = new ArrayList<>();
    private final List<JSONObject> listaDadosRotasJson = new ArrayList<>();
    private boolean estaNavegandoAtivamente = false;
    private BottomSheetBehavior<View> comportamentoPainelInferior;

    // Gerenciador de threads secundárias para não travar a interface gráfica (UI Thread)
    private ExecutorService executor;
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    // Controle de concorrência em buscas de endereço por geocodificação
    private long idRequisicaoEndereco = 0;

    // Callback para interceptar o botão de voltar do dispositivo
    private OnBackPressedCallback voltarParaCirculosCallback;

    // Adaptador e listas do sistema de círculos
    private MembroAdapter adaptadorMembros;
    private final List<Circulo> listaCirculos = new ArrayList<>();
    private final List<Marker> marcadoresMembros = new ArrayList<>();

    // Gerenciador de solicitação de permissões de localização em tempo de execução
    private final ActivityResultLauncher<String[]> lancadorDePermissaoLocalizacao =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
                Boolean permissaoPrecisaAceita = resultado.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean permissaoAproximadaAceita = resultado.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if ((permissaoPrecisaAceita != null && permissaoPrecisaAceita) ||
                        (permissaoAproximadaAceita != null && permissaoAproximadaAceita)) {
                    ativarLocalizacaoUsuario();
                    Toast.makeText(getContext(), "Permissão de localização concedida.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Permissão de localização negada.", Toast.LENGTH_SHORT).show();
                }
            });

    // --- CICLO DE VIDA ---

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Carrega as configurações de cache do OSMDroid com o contexto da aplicação
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        // Inicializa o ViewModel e o Executor
        viewModel = new ViewModelProvider(this).get(MapaViewModel.class);
        executor = Executors.newSingleThreadExecutor();

        vinculacaoLayout = FragmentMapaBinding.inflate(inflater, container, false);
        View raiz = vinculacaoLayout.getRoot();
        mapa = vinculacaoLayout.map;

        // Configura o painel inferior (BottomSheet)
        comportamentoPainelInferior = BottomSheetBehavior.from(vinculacaoLayout.bottomSheet);
        comportamentoPainelInferior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Trata o botão "Voltar" do Android para alternar entre visualizações do painel
        voltarParaCirculosCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                voltarParaCirculos();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), voltarParaCirculosCallback);

        // Configuração inicial do Mapa
        mapa.setTileSource(TileSourceFactory.MAPNIK);
        mapa.setMultiTouchControls(true);
        mapa.getController().setZoom(17.0);

        // Inicialização de ouvintes e funcionalidades
        configurarCliqueNoMapa();
        configurarOuvinteBarraPesquisa();
        configurarOuvintesBotoes();
        configurarCirculos();
        configurarAcessibilidade();
        processarBuscaRecebida();
        checarPermissoesELocalizar();

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

        // Encerra adequadamente as threads para evitar memory leaks
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        // Desativa a localização e limpa a view do mapa
        if (marcadorMinhaLocalizacao != null) {
            marcadorMinhaLocalizacao.disableMyLocation();
        }

        if (mapa != null) {
            mapa.onDetach();
        }

        vinculacaoLayout = null;
    }

    // --- CONFIGURAÇÃO DE EVENTOS E LISTENERS ---

    private void configurarOuvintesBotoes() {
        vinculacaoLayout.btnMinhaLocalizacao.setOnClickListener(v -> centralizarNaLocalizacaoAtual());
        vinculacaoLayout.btnNavigate.setOnClickListener(v -> executarCalculoDeRota());
        vinculacaoLayout.btnVoltarCirculos.setOnClickListener(v -> voltarParaCirculos());
    }

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

    /**
     * Define o destino selecionado no mapa, limpa rotas anteriores e busca o endereço.
     */
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

    private void configurarOuvinteBarraPesquisa() {
        vinculacaoLayout.etPesquisa.setOnEditorActionListener((view, acaoId, evento) -> {
            if (acaoId == EditorInfo.IME_ACTION_SEARCH || (evento != null && evento.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                buscarLocalizacaoPeloTexto(vinculacaoLayout.etPesquisa.getText().toString());
                return true;
            }
            return false;
        });
    }

    // --- NAVEGAÇÃO INTERNA DO BOTTOM SHEET ---

    private void mostrarModoLocal() {
        vinculacaoLayout.layoutCirculos.setVisibility(View.GONE);
        vinculacaoLayout.layoutLocal.setVisibility(View.VISIBLE);
        vinculacaoLayout.tvDistanciaTempo.setText("Calculando rota...");
        voltarParaCirculosCallback.setEnabled(true);
    }

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

    // --- CÍRCULOS DE CONTATOS ---

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

        vinculacaoLayout.rvMembros.announceForAccessibility(
                "Círculo " + c.nome + ", " + c.membros.size() + " membros");
    }

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

    // --- ACESSIBILIDADE ---

    private void configurarAcessibilidade() {
        ViewCompat.addAccessibilityAction(mapa, "Selecionar o ponto no centro do mapa", (v, args) -> {
            IGeoPoint centro = mapa.getMapCenter();
            selecionarLocal(new GeoPoint(centro.getLatitude(), centro.getLongitude()));
            return true;
        });
    }

    private void atualizarDescricaoAcessivel() {
        if (vinculacaoLayout == null || marcadorDestinoSelecionado == null) return;
        GeoPoint p = marcadorDestinoSelecionado.getPosition();
        String desc = vinculacaoLayout.tvNomeDoLocal.getText() + ". "
                + vinculacaoLayout.tvLocalizacaoEstado.getText() + ". "
                + vinculacaoLayout.tvDistanciaTempo.getText().toString().replace("(", ", ").replace(")", "") + ". "
                + String.format(new Locale("pt", "BR"), "Latitude %.5f, longitude %.5f", p.getLatitude(), p.getLongitude());
        vinculacaoLayout.blocoInfoLocal.setContentDescription(desc);
    }

    // --- SERVIÇOS DE LOCALIZAÇÃO (GPS) ---

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
                    if (getArguments() == null || !getArguments().containsKey("search_query")) {
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

    private void processarBuscaRecebida() {
        if (getArguments() != null && getArguments().containsKey("search_query")) {
            String textoRecebido = getArguments().getString("search_query");
            if (textoRecebido != null && !textoRecebido.isEmpty()) {
                vinculacaoLayout.etPesquisa.setText(textoRecebido);
                buscarLocalizacaoPeloTexto(textoRecebido);
            }
        }
    }

    // --- CÁLCULO DE ROTA (API OSRM) ---

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

    private void baixarETracarRotaInternet(GeoPoint origem, GeoPoint destino) {
        executor.execute(() -> {
            final List<JSONObject> rotasRecebidas = new ArrayList<>();

            try {
                String enderecoRequisicao = String.format(Locale.US,
                        "https://router.project-osrm.org/route/v1/%s/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&alternatives=true&steps=true",
                        "driving",
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
                e.printStackTrace();
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

    private void limparLinhasDesenhadasNoMapa() {
        for (Polyline linha : linhasDesenhadasDaRota) {
            mapa.getOverlays().remove(linha);
        }
        linhasDesenhadasDaRota.clear();
        mapa.invalidate();
    }

    /**
     * Desenha as linhas das rotas no mapa OSMDroid sem quebrar a ordem dos overlays do mapa.
     */
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
                e.printStackTrace();
            }
        }
        mapa.invalidate();
    }

    private void atualizarInformacoesDeTrajetoEmTempoReal(GeoPoint coordenadaAtual, GeoPoint coordenadaDestino) {
        double distanciaQueFalta = coordenadaAtual.distanceToAsDouble(coordenadaDestino);
        double tempoEstimadoRestante = distanciaQueFalta / 8.3; // Aproximadamente 30 km/h

        vinculacaoLayout.tvDistanciaTempo.setText(formatarTextoTempoEDistancia(tempoEstimadoRestante, distanciaQueFalta));

        if (distanciaQueFalta < 15.0) {
            estaNavegandoAtivamente = false;
            viewModel.setEstaNavegando(false);
            vinculacaoLayout.tvDistanciaTempo.setText("Você chegou ao seu destino! 🎉");
            Toast.makeText(getContext(), "Você chegou ao seu destino!", Toast.LENGTH_LONG).show();
        }
        atualizarDescricaoAcessivel();
    }

    // --- FORMATAÇÃO E AUXILIARES ---

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

    private void escreverPassosDaNavegacaoNaTela(JSONObject jsonCompletoRota) {
        try {
            SpannableStringBuilder textoComOsPassos = new SpannableStringBuilder();
            JSONArray blocosDaRota = jsonCompletoRota.getJSONArray("legs");

            if (blocosDaRota.length() > 0) {
                JSONArray passos = blocosDaRota.getJSONObject(0).getJSONArray("steps");
                textoComOsPassos.append("🟢 Ponto de Partida\n\n");

                int tamanhoIconePx = (int) (20 * getResources().getDisplayMetrics().density);

                for (int i = 0; i < passos.length(); i++) {
                    JSONObject passo = passos.getJSONObject(i);
                    String instrucao = passo.has("name") && !passo.getString("name").isEmpty()
                            ? "Siga por " + passo.getString("name")
                            : "Continue no percurso";

                    double metros = passo.getDouble("distance");
                    String distStr = metros < 1000 ? String.format(Locale.getDefault(), "%.0f m", metros) : String.format(Locale.getDefault(), "%.1f km", metros / 1000.0);

                    textoComOsPassos.append("• ").append(instrucao).append(" (").append(distStr).append(")\n");
                }
                textoComOsPassos.append("\n🔴 Chegada ao Destino");
                vinculacaoLayout.tvEtapasDetalhes.setText(textoComOsPassos);
            }
        } catch (Exception e) {
            e.printStackTrace();
            vinculacaoLayout.tvEtapasDetalhes.setText("• Toque no botão de rota para ver as instruções detalhadas.");
        }
    }
}