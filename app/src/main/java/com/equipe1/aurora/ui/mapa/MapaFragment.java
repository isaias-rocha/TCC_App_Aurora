package com.equipe1.aurora.ui.mapa;

// --- IMPORTS DO ANDROID E MATERIAL DESIGN ---
import android.Manifest;
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
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.equipe1.aurora.R;
// Importação da classe gerada automaticamente a partir do layout XML
import com.equipe1.aurora.databinding.FragmentMapaBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

// --- IMPORTS DO MAPA (OSMDROID) ---
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

// --- IMPORTS DE REDE E MANIPULAÇÃO DE JSON ---
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
import java.util.concurrent.Executors;

public class MapaFragment extends Fragment {
    // =========================================================================
    // 1. ATRIBUTOS E VARIÁVEIS DE ESTADO
    // =========================================================================

    // Objeto que conecta o código Java diretamente aos elementos da tela (XML)
    private FragmentMapaBinding vinculacaoLayout;

    // Componentes principais do mapa
    private MapView mapa;
    private MyLocationNewOverlay marcadorMinhaLocalizacao; // Ponto indicando onde o usuário está
    private Marker marcadorDestinoSelecionado; // Alfinete indicando o destino

    // Listas para armazenar os dados do trajeto (OSRM)
    private final List<Polyline> linhasDesenhadasDaRota = new ArrayList<>(); // Desenhos das linhas no mapa
    private final List<JSONObject> listaDadosRotasJson = new ArrayList<>(); // Dados brutos recebidos da internet

    // Controles de estado da navegação
    private boolean estaNavegandoAtivamente = false; // Verifica se o usuário já apertou para iniciar a rota
    private String perfilTransporteAtual = "walking"; // Pode ser "driving" (carro), "walking" (pé) ou "bike" (bicicleta)

    // Controlador do painel inferior (BottomSheet) que sobe na tela
    private BottomSheetBehavior<View> comportamentoPainelInferior;

    // Gerenciador responsável por pedir permissão de GPS para o usuário
    private final ActivityResultLauncher<String[]> lancadorDePermissaoLocalizacao =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
                // Verifica se a permissão precisa ou aproximada foi aceita
                Boolean permissaoPrecisaAceita = resultado.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean permissaoAproximadaAceita = resultado.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if ((permissaoPrecisaAceita != null && permissaoPrecisaAceita) ||
                        (permissaoAproximadaAceita != null && permissaoAproximadaAceita)) {
                    ativarLocalizacaoUsuario();
                    Toast.makeText(getContext(), "Permissão de localização aceita.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Permissão de localização negada.", Toast.LENGTH_SHORT).show();
                }
            });

    // =========================================================================
    // 2. CICLO DE VIDA DO FRAGMENT (MÉTODOS PADRÃO DO ANDROID)
    // =========================================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Prepara as configurações de armazenamento (cache) do mapa no dispositivo
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        // Vincula a interface gráfica (XML) com o Java
        vinculacaoLayout = FragmentMapaBinding.inflate(inflater, container, false);
        View raiz = vinculacaoLayout.getRoot();

        // Associa a variável 'mapa' ao mapa do XML
        mapa = vinculacaoLayout.map;

        // Configura a lógica de esconder ou mostrar o painel inferior (BottomSheet)
        comportamentoPainelInferior = BottomSheetBehavior.from(vinculacaoLayout.bottomSheet);
        comportamentoPainelInferior.setState(BottomSheetBehavior.STATE_HIDDEN); // Oculto inicialmente

        // Configurações básicas de navegação no mapa (zoom, servidor de imagens)
        mapa.setTileSource(TileSourceFactory.MAPNIK);
        mapa.setMultiTouchControls(true); // Permite usar dois dedos para dar zoom
        mapa.getController().setZoom(17.0);

        // Chama as funções que preparam os "ouvintes" (cliques na tela)
        configurarCliqueNoMapa();
        configurarOuvinteBarraPesquisa();
        configurarOuvintesBotoes();

        // Verifica se a tela foi aberta a partir de uma pesquisa de outro lugar
        processarBuscaRecebida();

        // Confere se o app tem permissão para usar o GPS e inicia
        checarPermissoesELocalizar();

        return raiz;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Volta a exibir o mapa e o GPS quando a tela volta para o primeiro plano
        if (mapa != null) mapa.onResume();
        if (marcadorMinhaLocalizacao != null) marcadorMinhaLocalizacao.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Pausa os processos visuais e o GPS quando o app vai para o fundo (economiza bateria)
        if (mapa != null) mapa.onPause();
        if (marcadorMinhaLocalizacao != null) marcadorMinhaLocalizacao.disableMyLocation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpa a memória para evitar travamentos (Memory Leak)
        if (mapa != null) mapa.onDetach();
        vinculacaoLayout = null;
    }

    // =========================================================================
    // 3. LISTENERS (AÇÕES DE INTERFACE)
    // =========================================================================

    /**
     * Prepara os cliques dos botões flutuantes.
     */
    private void configurarOuvintesBotoes() {
        // Centraliza a câmera no usuário ao clicar no botão de mira
        vinculacaoLayout.btnMinhaLocalizacao.setOnClickListener(v -> centralizarNaLocalizacaoAtual());

        // Inicia o cálculo do trajeto ao clicar em "Navegar"
        vinculacaoLayout.btnNavigate.setOnClickListener(v -> executarCalculoDeRota());
    }


    /**
     * Intercepta quando o usuário toca na tela do mapa
     */
    private void configurarCliqueNoMapa() {
        MapEventsReceiver receptorEventosMapa = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint pontoTocado) {
                // 1. Desliga navegações anteriores
                estaNavegandoAtivamente = false;
                limparLinhasDesenhadasNoMapa();

                if (marcadorMinhaLocalizacao != null) {
                    marcadorMinhaLocalizacao.disableFollowLocation();
                }

                // 2. Coloca o alfinete onde o usuário tocou
                adicionarOuMoverMarcadorDeDestino(pontoTocado);

                // 3. Busca qual é o nome da rua/local
                obterEnderecoGeocodificado(pontoTocado);

                // 4. Já começa a calcular o trajeto automaticamente
                executarCalculoDeRota();

                // 5. Mostra as informações subindo a aba inferior levemente
                if (comportamentoPainelInferior != null) {
                    comportamentoPainelInferior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint pontoTocado) {
                return false; // Ignora o toque longo
            }
        };

        // Adiciona a camada invisível que escuta toques no mapa
        mapa.getOverlays().add(new MapEventsOverlay(receptorEventosMapa));
    }

    /**
     * Ação disparada quando o usuário aperta o botão de 'lupa/enter' no teclado virtual
     */
    private void configurarOuvinteBarraPesquisa() {
        vinculacaoLayout.etSearch.setOnEditorActionListener((view, acaoId, evento) -> {
            // Verifica se foi apertado o botão de pesquisar
            if (acaoId == EditorInfo.IME_ACTION_SEARCH || (evento != null && evento.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                buscarLocalizacaoPeloTexto(vinculacaoLayout.etSearch.getText().toString());
                return true;
            }
            return false;
        });
    }

    // =========================================================================
    // 4. GPS E LOCALIZAÇÃO DO USUÁRIO
    // =========================================================================

    /**
     * Inicia o fluxo de verificar se temos permissão, e liga o GPS se tiver.
     */
    private void checarPermissoesELocalizar() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            ativarLocalizacaoUsuario();
        } else {
            // Mostra o pop-up pedindo acesso à localização
            lancadorDePermissaoLocalizacao.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    /**
     * Liga o "ponto azul" que segue o usuário no mapa
     */
    private void ativarLocalizacaoUsuario() {
        if (getContext() == null || mapa == null) return;

        GpsMyLocationProvider provedorGps = new GpsMyLocationProvider(requireContext());
        marcadorMinhaLocalizacao = new MyLocationNewOverlay(provedorGps, mapa) {
            @Override
            public void onLocationChanged(android.location.Location novaLocalizacao, org.osmdroid.views.overlay.mylocation.IMyLocationProvider fonte) {
                super.onLocationChanged(novaLocalizacao, fonte);

                // Quando estiver no meio da viagem guiada, vai atualizando a distância na tela
                if (novaLocalizacao != null && estaNavegandoAtivamente && marcadorDestinoSelecionado != null) {
                    GeoPoint coordenadaAtual = new GeoPoint(novaLocalizacao.getLatitude(), novaLocalizacao.getLongitude());
                    GeoPoint coordenadaDestino = marcadorDestinoSelecionado.getPosition();

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            mapa.getController().animateTo(coordenadaAtual);
                            atualizarInformacoesDeTrajetoEmTempoReal(coordenadaAtual, coordenadaDestino);
                        });
                    }
                }
            }
        };

        marcadorMinhaLocalizacao.enableMyLocation();
        marcadorMinhaLocalizacao.enableFollowLocation(); // O mapa segue o movimento do celular

        // Assim que achar o satélite pela primeira vez, dá um zoom onde o usuário está
        marcadorMinhaLocalizacao.runOnFirstFix(() -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (getArguments() == null || !getArguments().containsKey("search_query")) {
                        GeoPoint posicaoCapturada = marcadorMinhaLocalizacao.getMyLocation();
                        if (posicaoCapturada != null) {
                            mapa.getController().animateTo(posicaoCapturada);
                            obterEnderecoGeocodificado(posicaoCapturada);
                        }
                    }
                });
            }
        });

        mapa.getOverlays().add(marcadorMinhaLocalizacao);
        mapa.invalidate();
    }

    /**
     * Puxa a câmera manualmente de volta para o ponto onde o usuário está
     */
    private void centralizarNaLocalizacaoAtual() {
        if (marcadorMinhaLocalizacao != null && marcadorMinhaLocalizacao.getMyLocation() != null) {
            GeoPoint posicaoUsuario = marcadorMinhaLocalizacao.getMyLocation();
            mapa.getController().animateTo(posicaoUsuario);
            marcadorMinhaLocalizacao.enableFollowLocation();
            obterEnderecoGeocodificado(posicaoUsuario);

            if (comportamentoPainelInferior != null) {
                comportamentoPainelInferior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        } else {
            Toast.makeText(getContext(), "Buscando sinal de GPS...", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Pega o texto digitado (Ex: "Avenida Brasil") e converte em coordenadas (Lat, Long)
     */
    private void buscarLocalizacaoPeloTexto(String textoPesquisado) {
        if (textoPesquisado.isEmpty()) return;

        // Roda em segundo plano para não travar a tela
        Executors.newSingleThreadExecutor().execute(() -> {
            Geocoder conversorTextoParaMapa = new Geocoder(requireContext(), Locale.getDefault());
            try {
                // Tenta achar apenas 1 resultado relevante
                List<Address> enderecosEncontrados = conversorTextoParaMapa.getFromLocationName(textoPesquisado, 1);

                // Retorna os dados para a "Thread Principal" (A que desenha as coisas na tela)
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return;

                    if (enderecosEncontrados != null && !enderecosEncontrados.isEmpty()) {
                        Address melhorEndereco = enderecosEncontrados.get(0);
                        GeoPoint coordenada = new GeoPoint(melhorEndereco.getLatitude(), melhorEndereco.getLongitude());

                        if (marcadorMinhaLocalizacao != null) {
                            marcadorMinhaLocalizacao.disableFollowLocation();
                        }

                        adicionarOuMoverMarcadorDeDestino(coordenada);
                        vinculacaoLayout.tvNomeDoLocal.setText(melhorEndereco.getFeatureName());
                        vinculacaoLayout.tvLocalizacaoEstado.setText(melhorEndereco.getLocality() + ", " + melhorEndereco.getAdminArea());

                        executarCalculoDeRota(); // Calcula como chegar lá

                        if (comportamentoPainelInferior != null) {
                            comportamentoPainelInferior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        }
                    } else {
                        Toast.makeText(getContext(), "Local não encontrado", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "Erro ao buscar local", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    /**
     * Recebe informações de texto que vieram de uma tela anterior (se houver)
     */
    private void processarBuscaRecebida() {
        if (getArguments() != null && getArguments().containsKey("search_query")) {
            String textoRecebido = getArguments().getString("search_query");
            if (textoRecebido != null && !textoRecebido.isEmpty()) {
                vinculacaoLayout.etSearch.setText(textoRecebido);
                buscarLocalizacaoPeloTexto(textoRecebido);
            }
        }
    }

    // =========================================================================
    // 5. CONEXÃO COM OSRM E CÁLCULO DE ROTAS
    // =========================================================================

    /**
     * Prepara os dados do ponto A (GPS) e Ponto B (Marcador) para traçar o caminho
     */
    private void executarCalculoDeRota() {
        if (marcadorDestinoSelecionado == null) {
            Toast.makeText(getContext(), "Selecione um ponto no mapa primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }

        GeoPoint coordenadaDestino = marcadorDestinoSelecionado.getPosition();
        GeoPoint coordenadaOrigem = (marcadorMinhaLocalizacao != null) ? marcadorMinhaLocalizacao.getMyLocation() : null;

        if (coordenadaOrigem == null) {
            Toast.makeText(getContext(), "Aguardando sinal do GPS...", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Calculando rota...", Toast.LENGTH_SHORT).show();
        baixarETracarRotaInternet(coordenadaOrigem, coordenadaDestino, perfilTransporteAtual);
    }

    /**
     * Faz a requisição na internet (API OSRM) para pedir as ruas do trajeto
     */
    private void baixarETracarRotaInternet(GeoPoint origem, GeoPoint destino, String perfil) {
        Executors.newSingleThreadExecutor().execute(() -> {
            listaDadosRotasJson.clear(); // Limpa as rotas antigas

            try {
                // Criação do Link da API
                String enderecoRequisicao = String.format(Locale.US,
                        "https://router.project-osrm.org/route/v1/%s/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&alternatives=true&steps=true",
                        perfil,
                        origem.getLongitude(), origem.getLatitude(),
                        destino.getLongitude(), destino.getLatitude());

                // Abre a conexão
                URL linkUrl = new URL(enderecoRequisicao);
                HttpURLConnection conexao = (HttpURLConnection) linkUrl.openConnection();
                conexao.setRequestMethod("GET");
                conexao.setRequestProperty("User-Agent", Configuration.getInstance().getUserAgentValue());
                conexao.setConnectTimeout(8000);

                // Se o servidor respondeu 'OK' (Código 200)
                if (conexao.getResponseCode() == 200) {
                    BufferedReader leitorTextos = new BufferedReader(new InputStreamReader(conexao.getInputStream()));
                    StringBuilder respostaServidor = new StringBuilder();
                    String linhaTexto;

                    // Lê o JSON linha por linha
                    while ((linhaTexto = leitorTextos.readLine()) != null) {
                        respostaServidor.append(linhaTexto);
                    }
                    leitorTextos.close();

                    // Converte o texto recebido para um Objeto Manipulável (JSON)
                    JSONObject objetoJsonCompleto = new JSONObject(respostaServidor.toString());
                    JSONArray conjuntoRotas = objetoJsonCompleto.getJSONArray("routes");

                    for (int i = 0; i < conjuntoRotas.length(); i++) {
                        listaDadosRotasJson.add(conjuntoRotas.getJSONObject(i));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(); // Registra o erro no Log do Android Studio
            }

            // Volta para a tela para desenhar
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) return;

                if (!listaDadosRotasJson.isEmpty()) {
                    desenharRotasRecebidasNoMapa(0); // A rota 0 é a mais rápida (oficial)
                    estaNavegandoAtivamente = true;

                    if (marcadorMinhaLocalizacao != null) {
                        marcadorMinhaLocalizacao.enableFollowLocation();
                    }
                } else {
                    Toast.makeText(getContext(), "Não foi possível encontrar rotas.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Pega os dados recebidos da internet, transforma em linhas, pinta, e coloca no mapa
     */
    private void desenharRotasRecebidasNoMapa(int indiceRotaSelecionada) {
        limparLinhasDesenhadasNoMapa();

        for (int i = 0; i < listaDadosRotasJson.size(); i++) {
            try {
                JSONObject informacaoRota = listaDadosRotasJson.get(i);
                JSONArray coordenadasRota = informacaoRota.getJSONObject("geometry").getJSONArray("coordinates");

                // Converte as coordenadas do formato do JSON [Long, Lat] para o formato do Mapa [Lat, Long]
                List<GeoPoint> pontosDaLinha = new ArrayList<>();
                for (int j = 0; j < coordenadasRota.length(); j++) {
                    JSONArray parDePontos = coordenadasRota.getJSONArray(j);
                    pontosDaLinha.add(new GeoPoint(parDePontos.getDouble(1), parDePontos.getDouble(0)));
                }

                // Cria o objeto "Caminho" visual (Polyline)
                Polyline linhaColorida = new Polyline(mapa);
                linhaColorida.setPoints(pontosDaLinha);

                final int indiceDestaRota = i;
                boolean ehARotaPrincipal = (i == indiceRotaSelecionada);

                if (ehARotaPrincipal) {
                    // Pinta de Roxo a rota que o usuário escolheu
                    linhaColorida.getOutlinePaint().setColor(Color.parseColor("#6C5CE7"));
                    linhaColorida.getOutlinePaint().setStrokeWidth(16f);

                    double distanciaEmMetros = informacaoRota.getDouble("distance");
                    double duracaoEmSegundos = informacaoRota.getDouble("duration");

                    // Atualiza a telinha debaixo com as estimativas
                    vinculacaoLayout.tvNomeDoLocal.setText(formatarTextoTempoEDistancia(duracaoEmSegundos, distanciaEmMetros));
                    vinculacaoLayout.tvLocalizacaoEstado.setText(listaDadosRotasJson.size() > 1 ?
                            "Rota " + (indiceRotaSelecionada + 1) + " ativa (Navegando...)" : "Em navegação...");

                    escreverPassosDaNavegacaoNaTela(informacaoRota);

                } else {
                    // Pinta as rotas "opcionais" de cinza
                    linhaColorida.getOutlinePaint().setColor(Color.parseColor("#64748B"));
                    linhaColorida.getOutlinePaint().setStrokeWidth(10f);
                }

                // Permite o usuário trocar para uma rota cinza apenas clicando nela
                linhaColorida.setOnClickListener((linhaTrocada, vistaDoMapa, eventoPosicao) -> {
                    desenharRotasRecebidasNoMapa(indiceDestaRota);
                    Toast.makeText(getContext(), "Rota " + (indiceDestaRota + 1) + " selecionada", Toast.LENGTH_SHORT).show();
                    return true;
                });

                linhasDesenhadasDaRota.add(linhaColorida);

                // Coloca as rotas cinzas para baixo e a Rota roxa por cima
                if (ehARotaPrincipal) {
                    mapa.getOverlays().add(linhaColorida);
                } else {
                    mapa.getOverlays().add(0, linhaColorida);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mapa.invalidate(); // Pede para a placa de vídeo redesenhar o mapa atualizado
    }

    /**
     * Calcula se o usuário está mais próximo de chegar com o movimento do GPS
     */
    private void atualizarInformacoesDeTrajetoEmTempoReal(GeoPoint coordenadaAtual, GeoPoint coordenadaDestino) {
        double distanciaQueFalta = coordenadaAtual.distanceToAsDouble(coordenadaDestino);
        double tempoEstimadoRestante = distanciaQueFalta / 8.3; // Velocidade arbitrária de cálculo (30km/h)

        vinculacaoLayout.tvNomeDoLocal.setText(formatarTextoTempoEDistancia(tempoEstimadoRestante, distanciaQueFalta));

        // Se faltar 15 metros, avisa que chegou
        if (distanciaQueFalta < 15.0) {
            estaNavegandoAtivamente = false;
            vinculacaoLayout.tvLocalizacaoEstado.setText("Você chegou ao seu destino! 🎉");
            Toast.makeText(getContext(), "Você chegou ao seu destino!", Toast.LENGTH_LONG).show();
        }
    }

    // =========================================================================
    // 6. FORMATAÇÃO VISUAL E AUXILIARES
    // =========================================================================

    /**
     * Coloca o pino onde o usuário clicar (ou move se ele já existir)
     */
    private void adicionarOuMoverMarcadorDeDestino(GeoPoint coordenada) {
        if (marcadorDestinoSelecionado == null) {
            marcadorDestinoSelecionado = new Marker(mapa);
            marcadorDestinoSelecionado.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapa.getOverlays().add(marcadorDestinoSelecionado);
        }
        marcadorDestinoSelecionado.setPosition(coordenada);
        mapa.getController().animateTo(coordenada);
        mapa.invalidate();
    }

    /**
     * Converte Latitude/Longitude em Nome da Rua / Cidade para mostrar ao usuário
     */
    private void obterEnderecoGeocodificado(GeoPoint coordenada) {
        vinculacaoLayout.tvCoordenadas.setText(String.format(Locale.getDefault(), "Lat: %.4f | Lon: %.4f", coordenada.getLatitude(), coordenada.getLongitude()));

        Executors.newSingleThreadExecutor().execute(() -> {
            Geocoder descodificador = new Geocoder(requireContext(), Locale.getDefault());
            String ruaEncontrada = "Local Selecionado";
            String cidadeEncontrada = "";

            try {
                List<Address> locais = descodificador.getFromLocation(coordenada.getLatitude(), coordenada.getLongitude(), 1);
                if (locais != null && !locais.isEmpty()) {
                    Address endereco = locais.get(0);
                    ruaEncontrada = endereco.getSubLocality() != null ? endereco.getSubLocality() : endereco.getThoroughfare();
                    cidadeEncontrada = (endereco.getLocality() != null ? endereco.getLocality() : "") +
                            (endereco.getAdminArea() != null ? ", " + endereco.getAdminArea() : "");
                }
            } catch (IOException ignorado) {} // Não faz nada se a internet falhar em buscar o nome da rua

            final String nomeFinalDaRua = ruaEncontrada;
            final String nomeFinalDaCidade = cidadeEncontrada;

            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) return;
                if (nomeFinalDaRua != null) vinculacaoLayout.tvNomeDoLocal.setText(nomeFinalDaRua);
                vinculacaoLayout.tvLocalizacaoEstado.setText(nomeFinalDaCidade);
            });
        });
    }

    /**
     * Extrai os comandos de navegação ("vire à esquerda", "siga") do JSON da Rota
     */
    private void escreverPassosDaNavegacaoNaTela(JSONObject jsonCompletoRota) {
        try {
            // Usa SpannableStringBuilder em vez de StringBuilder para suportar as imagens
            SpannableStringBuilder textoComOsPassos = new SpannableStringBuilder();
            JSONArray blocosDaRota = jsonCompletoRota.getJSONArray("legs");

            if (blocosDaRota.length() > 0) {
                JSONArray passos = blocosDaRota.getJSONObject(0).getJSONArray("steps");
                textoComOsPassos.append("🟢 Ponto de Partida\n\n");

                for (int i = 0; i < passos.length(); i++) {
                    JSONObject informacaoDoPasso = passos.getJSONObject(i);
                    String nomeDaRua = informacaoDoPasso.optString("name", "");
                    double distanciaDoPasso = informacaoDoPasso.optDouble("distance", 0);

                    JSONObject manobraDestePasso = informacaoDoPasso.optJSONObject("maneuver");
                    String direcaoManobra = (manobraDestePasso != null) ? manobraDestePasso.optString("modifier", "") : "";

                    // 1. Pegamos o ID do Ícone (int) e o Texto descritivo (String)
                    int idIcone = obterIconeDeDirecao(direcaoManobra);
                    String textoDaManobra = obterTextoDeDirecao(direcaoManobra);

                    // 2. Coloca a imagem (Drawable) dentro do texto
                    Drawable icone = ContextCompat.getDrawable(requireContext(), idIcone);
                    if (icone != null) {
                        // Define a largura e altura do ícone no texto (ex: 50x50 pixels)
                        icone.setBounds(0, 0, 50, 50);
                        int posicaoInicial = textoComOsPassos.length();

                        textoComOsPassos.append("  "); // Adiciona um espaço que será substituído pela imagem

                        textoComOsPassos.setSpan(
                                new ImageSpan(icone, ImageSpan.ALIGN_BOTTOM),
                                posicaoInicial,
                                posicaoInicial + 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                    }

                    // 3. Adiciona o texto ao lado do ícone
                    textoComOsPassos.append(textoDaManobra).append(" ");

                    if (!nomeDaRua.isEmpty()) {
                        textoComOsPassos.append("por ").append(nomeDaRua);
                    } else {
                        textoComOsPassos.append("no trajeto");
                    }

                    if (distanciaDoPasso > 0) {
                        textoComOsPassos.append(" (").append((char) distanciaDoPasso).append(" m)");
                    }
                    textoComOsPassos.append("\n\n");
                }

                textoComOsPassos.append("📍 Chegada ao Destino");

                // Passa o resultado final para a tela
                vinculacaoLayout.tvEtapasDetalhes.setText(textoComOsPassos);
                vinculacaoLayout.layoutEtapasExpandido.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retorna o ID do ícone SVG correspondente
     */
    private int obterIconeDeDirecao(String direcaoSeta) {
        if (direcaoSeta == null) return R.drawable.ic_seta_cima;

        if (direcaoSeta.contains("sharp right")) return R.drawable.ic_circulo_curva_acentuada_direita;
        if (direcaoSeta.contains("sharp left"))  return R.drawable.ic_circulo_curva_acentuada_esquerda;
        if (direcaoSeta.contains("slight right")) return R.drawable.ic_seta_direita;
        if (direcaoSeta.contains("slight left"))  return R.drawable.ic_seta_esquerda;
        if (direcaoSeta.contains("right"))        return R.drawable.ic_seta_direita;
        if (direcaoSeta.contains("left"))         return R.drawable.ic_seta_esquerda;
        if (direcaoSeta.contains("uturn") || direcaoSeta.contains("return")) return R.drawable.ic_seta_retorno;

        return R.drawable.ic_seta_cima;
    }

    /**
     * Retorna o texto explicativo da manobra
     */
    private String obterTextoDeDirecao(String direcaoSeta) {
        if (direcaoSeta == null) return "Siga em frente";

        if (direcaoSeta.contains("sharp right")) return "Curva acentuada à direita";
        if (direcaoSeta.contains("sharp left"))  return "Curva acentuada à esquerda";
        if (direcaoSeta.contains("slight right")) return "Mantenha-se à direita";
        if (direcaoSeta.contains("slight left"))  return "Mantenha-se à esquerda";
        if (direcaoSeta.contains("right"))        return "Vire à direita";
        if (direcaoSeta.contains("left"))         return "Vire à esquerda";
        if (direcaoSeta.contains("uturn") || direcaoSeta.contains("return")) return "Faça o retorno";

        return "Siga em frente";
    }

    /**
     * Apaga todos os desenhos das ruas do mapa
     */
    private void limparLinhasDesenhadasNoMapa() {
        for (Polyline linha : linhasDesenhadasDaRota) {
            mapa.getOverlays().remove(linha);
        }
        linhasDesenhadasDaRota.clear();
        vinculacaoLayout.tvEtapasDetalhes.setText("• Deslize para cima para ver as instruções detalhadas após calcular a rota.");
        mapa.invalidate();
    }

    /**
     * Muda a cor do botão que o usuário clicou (Carro, Pé, Bicicleta) para mostrar que ele tá ativo
     */
    private void atualizarVisualBotoesTransporte(Button botaoClicadoAtivo, Button botaoDesligado1, Button botaoDesligado2) {
        if (botaoClicadoAtivo != null) {
            // Fundo: Cor primária do projeto (definida no colors.xml)
            botaoClicadoAtivo.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.cor_primaria));
            // Texto: Branco para dar destaque e contraste contra a cor primária
            botaoClicadoAtivo.setTextColor(Color.WHITE);
        }
        if (botaoDesligado1 != null) {
            // Fundo: Cor de fundo do projeto (substitua 'cor_de_fundo' pelo nome real no seu colors.xml)
            botaoDesligado1.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.cor_borda));
            // Texto: Mantive cinza para indicar que está inativo
            botaoDesligado1.setTextColor(Color.parseColor("#94A3B8"));
        }
        if (botaoDesligado2 != null) {
            // Fundo: Cor de fundo do projeto
            botaoDesligado2.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.cor_borda));
            botaoDesligado2.setTextColor(Color.parseColor("#94A3B8"));
        }
    }

    /**
     * Converte o tempo (em segundos) e a distância (em metros) - ("5 min (1.2 km)")
     */
    private String formatarTextoTempoEDistancia(double segundosTotais, double metrosTotais) {
        int minutosFormatados = (int) Math.round(segundosTotais / 60.0);
        if (minutosFormatados < 1) minutosFormatados = 1;

        String stringDistanciaFormatada = (metrosTotais >= 1000)
                ? String.format(Locale.getDefault(), "%.1f km", metrosTotais / 1000.0)
                : String.format(Locale.getDefault(), "%d m", (int) metrosTotais);

        return String.format(Locale.getDefault(), "%d min (%s)", minutosFormatados, stringDistanciaFormatada);
    }
}