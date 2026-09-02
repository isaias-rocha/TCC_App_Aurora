package com.equipe1.aurora.ui.map;

// --- IMPORTS DAS BIBLIOTECAS DO ANDROID E MATERIAL DESIGN ---
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.equipe1.aurora.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

// --- IMPORTS DE REDE E JSON ---
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
    // 1. VARIÁVEIS E COMPONENTES
    // =========================================================================

    // Componentes do Mapa
    private MapView map;
    private MyLocationNewOverlay myLocationOverlay;
    private Marker selectedMarker;

    // Controle de Rotas e Navegação
    private final List<Polyline> routePolylines = new ArrayList<>();
    private final List<JSONObject> routesJsonList = new ArrayList<>();
    private boolean isNavigating = false; // Indica se o usuário está em deslocamento ativo
    private String currentTransportProfile = "driving"; // Perfis: driving, walking, bike

    // Componentes de Interface (UI)
    private TextView tvNomeDoLocal, tvLocalizacaoEstado, tvCoordenadas, tvEtapasDetalhes;
    private EditText etSearch;
    private FloatingActionButton btnNavigate, btnMyLocation;
    private Button btnModeCar, btnModeWalk, btnModeBike;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private LinearLayout layoutEtapasExpandido;

    // Gerenciador de Permissões de Localização (Android 6.0+)
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if ((fineLocationGranted != null && fineLocationGranted) ||
                        (coarseLocationGranted != null && coarseLocationGranted)) {
                    ativarLocalizacaoUsuario();
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
        // Carrega as configurações do OSMDroid para armazenar os tiles do mapa
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        View root = inflater.inflate(R.layout.fragment_mapa, container, false);

        // Inicializar Views da Interface
        inicializarViews(root);

        // Configurar o BottomSheet Expansível
        View bottomSheetView = root.findViewById(R.id.bottomSheet);
        if (bottomSheetView != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN); // Escondido até tocar em um ponto
        }

        // Configurações básicas do Mapa
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(17.0);

        // Configuração de Listeners e Eventos
        setupMapClickListener();
        setupSearchListener();
        setupTransportModeListeners();
        setupButtonListeners();

        // Processa busca prévia se recebida via Arguments
        processarBuscaRecebida();

        // Checa permissões de GPS
        checarPermissoesELocalizar();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
    }

    // =========================================================================
    // 3. INICIALIZAÇÃO DE VIEWS E LISTENERS
    // =========================================================================

    private void inicializarViews(View root) {
        map = root.findViewById(R.id.map);
        etSearch = root.findViewById(R.id.etSearch);
        tvNomeDoLocal = root.findViewById(R.id.tv_nomeDoLocal);
        tvLocalizacaoEstado = root.findViewById(R.id.tv_localizacaoEstado);
        tvCoordenadas = root.findViewById(R.id.tv_coordenadas);
        tvEtapasDetalhes = root.findViewById(R.id.tvEtapasDetalhes);
        layoutEtapasExpandido = root.findViewById(R.id.layoutEtapasExpandido);

        btnNavigate = root.findViewById(R.id.btnNavigate);
        btnMyLocation = root.findViewById(R.id.btnMyLocation);

        btnModeCar = root.findViewById(R.id.btnModeCar);
        btnModeWalk = root.findViewById(R.id.btnModeWalk);
        btnModeBike = root.findViewById(R.id.btnModeBike);
    }

    private void setupButtonListeners() {
        // Centraliza a câmera no usuário
        if (btnMyLocation != null) {
            btnMyLocation.setOnClickListener(v -> centralizarNaLocalizacaoAtual());
        }

        // Inicia o cálculo de rota
        if (btnNavigate != null) {
            btnNavigate.setOnClickListener(v -> executarCalculoDeRota());
        }
    }

    // Listener para seleção do perfil de transporte (Carro, A pé, Bicicleta)
    private void setupTransportModeListeners() {
        View.OnClickListener listener = v -> {
            int id = v.getId();
            if (id == R.id.btnModeCar) {
                currentTransportProfile = "driving";
                atualizarBotoesTransporte(btnModeCar, btnModeWalk, btnModeBike);
            } else if (id == R.id.btnModeWalk) {
                currentTransportProfile = "walking";
                atualizarBotoesTransporte(btnModeWalk, btnModeCar, btnModeBike);
            } else if (id == R.id.btnModeBike) {
                currentTransportProfile = "bike";
                atualizarBotoesTransporte(btnModeBike, btnModeCar, btnModeWalk);
            }

            // Recalcula a rota automaticamente se já houver um destino selecionado
            if (selectedMarker != null) {
                executarCalculoDeRota();
            }
        };

        if (btnModeCar != null) btnModeCar.setOnClickListener(listener);
        if (btnModeWalk != null) btnModeWalk.setOnClickListener(listener);
        if (btnModeBike != null) btnModeBike.setOnClickListener(listener);
    }

    // =========================================================================
    // 4. PERMISSÕES E GEOLOCALIZAÇÃO EM TEMPO REAL
    // =========================================================================

    private void checarPermissoesELocalizar() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            ativarLocalizacaoUsuario();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void ativarLocalizacaoUsuario() {
        if (getContext() == null || map == null) return;

        GpsMyLocationProvider provider = new GpsMyLocationProvider(requireContext());

        // Overlay com suporte a atualizações de movimento em tempo real
        myLocationOverlay = new MyLocationNewOverlay(provider, map) {
            @Override
            public void onLocationChanged(android.location.Location location, org.osmdroid.views.overlay.mylocation.IMyLocationProvider source) {
                super.onLocationChanged(location, source);

                // Rastreia o movimento do usuário se estiver no modo de navegação ativa
                if (location != null && isNavigating && selectedMarker != null) {
                    GeoPoint posicaoAtual = new GeoPoint(location.getLatitude(), location.getLongitude());
                    GeoPoint destino = selectedMarker.getPosition();

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            map.getController().animateTo(posicaoAtual);
                            atualizarProgressoEmTempoReal(posicaoAtual, destino);
                        });
                    }
                }
            }
        };

        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();

        // Ao obter a primeira posição do GPS
        myLocationOverlay.runOnFirstFix(() -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (getArguments() == null || !getArguments().containsKey("search_query")) {
                        GeoPoint minhaPosicao = myLocationOverlay.getMyLocation();
                        if (minhaPosicao != null) {
                            map.getController().animateTo(minhaPosicao);
                            obterEnderecoGeocodificado(minhaPosicao);
                        }
                    }
                });
            }
        });

        map.getOverlays().add(myLocationOverlay);
        map.invalidate();
    }

    private void centralizarNaLocalizacaoAtual() {
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            GeoPoint minhaPosicao = myLocationOverlay.getMyLocation();
            map.getController().animateTo(minhaPosicao);
            myLocationOverlay.enableFollowLocation();
            obterEnderecoGeocodificado(minhaPosicao);

            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        } else {
            Toast.makeText(getContext(), "Buscando sinal de GPS...", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // 5. TOQUES NO MAPA E PESQUISA DE ENDEREÇOS
    // =========================================================================

    private void setupMapClickListener() {
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                // Ao clicar no mapa, encerra a navegação anterior e limpa as linhas
                isNavigating = false;
                limparLinhasDeRota();

                if (myLocationOverlay != null) {
                    myLocationOverlay.disableFollowLocation();
                }

                adicionarOuMoverMarcador(p);
                obterEnderecoGeocodificado(p);

                // Exibe o BottomSheet recolhido para apresentar o local
                if (bottomSheetBehavior != null) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        map.getOverlays().add(new MapEventsOverlay(mapEventsReceiver));
    }

    private void setupSearchListener() {
        if (etSearch == null) return;
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                buscarLocalizacao(etSearch.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void buscarLocalizacao(String query) {
        if (query.isEmpty()) return;

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                GeoPoint point = new GeoPoint(address.getLatitude(), address.getLongitude());

                if (myLocationOverlay != null) {
                    myLocationOverlay.disableFollowLocation();
                }

                adicionarOuMoverMarcador(point);
                if (tvNomeDoLocal != null) tvNomeDoLocal.setText(address.getFeatureName());
                if (tvLocalizacaoEstado != null) tvLocalizacaoEstado.setText(address.getLocality() + ", " + address.getAdminArea());

                if (bottomSheetBehavior != null) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            } else {
                Toast.makeText(getContext(), "Local não encontrado", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Erro ao buscar local", Toast.LENGTH_SHORT).show();
        }
    }

    private void processarBuscaRecebida() {
        if (getArguments() != null && getArguments().containsKey("search_query")) {
            String query = getArguments().getString("search_query");
            if (query != null && !query.isEmpty()) {
                if (etSearch != null) etSearch.setText(query);
                buscarLocalizacao(query);
            }
        }
    }

    // =========================================================================
    // 6. CÁLCULO, MULTI-ROTAS E NAVEGAÇÃO INTERATIVA
    // =========================================================================

    private void executarCalculoDeRota() {
        if (selectedMarker == null) {
            Toast.makeText(getContext(), "Selecione um ponto no mapa primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }

        GeoPoint destino = selectedMarker.getPosition();
        GeoPoint origem = (myLocationOverlay != null) ? myLocationOverlay.getMyLocation() : null;

        if (origem == null) {
            Toast.makeText(getContext(), "Aguardando sinal do GPS...", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Calculando rotas...", Toast.LENGTH_SHORT).show();
        calcularETracarRota(origem, destino, currentTransportProfile);
    }

    // Faz a chamada à API do OSRM solicitando alternativas (`alternatives=true`)
    private void calcularETracarRota(GeoPoint origem, GeoPoint destino, String profile) {
        Executors.newSingleThreadExecutor().execute(() -> {
            routesJsonList.clear();

            try {
                String urlString = String.format(Locale.US,
                        "https://router.project-osrm.org/route/v1/%s/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&alternatives=true&steps=true",
                        profile,
                        origem.getLongitude(), origem.getLatitude(),
                        destino.getLongitude(), destino.getLatitude());

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONArray routes = jsonObject.getJSONArray("routes");

                    for (int i = 0; i < routes.length(); i++) {
                        routesJsonList.add(routes.getJSONObject(i));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                if (!routesJsonList.isEmpty()) {
                    exibirRotasNoMapa(0); // Desenha a rota principal (índice 0)
                    isNavigating = true; // Inicia o rastreamento do GPS

                    if (myLocationOverlay != null) {
                        myLocationOverlay.enableFollowLocation();
                    }
                } else {
                    Toast.makeText(getContext(), "Não foi possível encontrar rotas.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Desenha as rotas no mapa: Principal (Roxa) e Alternativas (Cinza)
    private void exibirRotasNoMapa(int selectedIndex) {
        limparLinhasDeRota();

        for (int i = 0; i < routesJsonList.size(); i++) {
            try {
                JSONObject route = routesJsonList.get(i);
                JSONArray coordinates = route.getJSONObject("geometry").getJSONArray("coordinates");

                List<GeoPoint> pontos = new ArrayList<>();
                for (int j = 0; j < coordinates.length(); j++) {
                    JSONArray pt = coordinates.getJSONArray(j);
                    pontos.add(new GeoPoint(pt.getDouble(1), pt.getDouble(0)));
                }

                Polyline polyline = new Polyline(map);
                polyline.setPoints(pontos);

                final int routeIndex = i;
                boolean isSelected = (i == selectedIndex);

                if (isSelected) {
                    // Rota Selecionada: Roxa e Larga
                    polyline.getOutlinePaint().setColor(Color.parseColor("#6C5CE7"));
                    polyline.getOutlinePaint().setStrokeWidth(16f);

                    // Atualiza textos do Card no BottomSheet
                    double distanceMeters = route.getDouble("distance");
                    double durationSeconds = route.getDouble("duration");

                    if (tvNomeDoLocal != null) {
                        tvNomeDoLocal.setText(formatarTempoEDistancia(durationSeconds, distanceMeters));
                    }
                    if (tvLocalizacaoEstado != null) {
                        tvLocalizacaoEstado.setText(routesJsonList.size() > 1 ?
                                "Rota " + (selectedIndex + 1) + " ativa (Navegando...)" : "Em navegação...");
                    }
                    atualizarInstrucoesEtapas(route);

                } else {
                    // Rota Alternativa: Cinza e Mais Fina
                    polyline.getOutlinePaint().setColor(Color.parseColor("#64748B"));
                    polyline.getOutlinePaint().setStrokeWidth(10f);
                }

                // Permite tocar em uma linha cinza para alternar para aquela rota!
                polyline.setOnClickListener((p, mapView, eventPos) -> {
                    exibirRotasNoMapa(routeIndex);
                    Toast.makeText(getContext(), "Rota " + (routeIndex + 1) + " selecionada", Toast.LENGTH_SHORT).show();
                    return true;
                });

                routePolylines.add(polyline);

                // Garante que a rota selecionada fique por cima das outras no mapa
                if (isSelected) {
                    map.getOverlays().add(polyline);
                } else {
                    map.getOverlays().add(0, polyline);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        map.invalidate();
    }

    // Recalcula o tempo/distância durante a navegação em tempo real
    private void atualizarProgressoEmTempoReal(GeoPoint posicaoAtual, GeoPoint destino) {
        double distanciaRestanteMetros = posicaoAtual.distanceToAsDouble(destino);
        double tempoRestanteSegundos = distanciaRestanteMetros / 8.3; // Estimativa de velocidade média urbana

        if (tvNomeDoLocal != null) {
            tvNomeDoLocal.setText(formatarTempoEDistancia(tempoRestanteSegundos, distanciaRestanteMetros));
        }

        // Se o usuário chegar a menos de 15m do destino
        if (distanciaRestanteMetros < 15.0) {
            isNavigating = false;
            if (tvLocalizacaoEstado != null) {
                tvLocalizacaoEstado.setText("Você chegou ao seu destino! 🎉");
            }
            Toast.makeText(getContext(), "Você chegou ao seu destino!", Toast.LENGTH_LONG).show();
        }
    }

    // =========================================================================
    // 7. MÉTODOS AUXILIARES E FORMATADORES
    // =========================================================================

    private void adicionarOuMoverMarcador(GeoPoint ponto) {
        if (selectedMarker == null) {
            selectedMarker = new Marker(map);
            selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(selectedMarker);
        }
        selectedMarker.setPosition(ponto);
        map.getController().animateTo(ponto);
        map.invalidate();
    }

    private void obterEnderecoGeocodificado(GeoPoint ponto) {
        if (tvCoordenadas != null) {
            tvCoordenadas.setText(String.format(Locale.getDefault(), "Lat: %.4f | Lon: %.4f", ponto.getLatitude(), ponto.getLongitude()));
        }

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(ponto.getLatitude(), ponto.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String bairroOuRua = address.getSubLocality() != null ? address.getSubLocality() : address.getThoroughfare();
                String cidadeEstado = (address.getLocality() != null ? address.getLocality() : "") +
                        (address.getAdminArea() != null ? ", " + address.getAdminArea() : "");

                if (tvNomeDoLocal != null) tvNomeDoLocal.setText(bairroOuRua != null ? bairroOuRua : "Local Selecionado");
                if (tvLocalizacaoEstado != null) tvLocalizacaoEstado.setText(cidadeEstado);
            }
        } catch (IOException e) {
            if (tvNomeDoLocal != null) tvNomeDoLocal.setText("Ponto Selecionado");
        }
    }

    private void atualizarInstrucoesEtapas(JSONObject routeJson) {
        try {
            StringBuilder etapasTexto = new StringBuilder();
            JSONArray legs = routeJson.getJSONArray("legs");

            if (legs.length() > 0) {
                JSONArray steps = legs.getJSONObject(0).getJSONArray("steps");

                etapasTexto.append("🟢 Ponto de Partida\n\n");

                for (int s = 0; s < steps.length(); s++) {
                    JSONObject step = steps.getJSONObject(s);
                    String streetName = step.optString("name", "");
                    double stepDistance = step.optDouble("distance", 0);

                    JSONObject maneuver = step.optJSONObject("maneuver");
                    String modifier = (maneuver != null) ? maneuver.optString("modifier", "") : "";
                    String type = (maneuver != null) ? maneuver.optString("type", "") : "";

                    String icone = obterIconeManobra(type, modifier);

                    etapasTexto.append(icone).append(" ");

                    if (!streetName.isEmpty()) {
                        etapasTexto.append("Siga por ").append(streetName);
                    } else {
                        etapasTexto.append("Continue no trajeto");
                    }

                    if (stepDistance > 0) {
                        etapasTexto.append(" (").append((int) stepDistance).append(" m)");
                    }
                    etapasTexto.append("\n\n");
                }

                etapasTexto.append("📍 Chegada ao Destino");

                if (tvEtapasDetalhes != null) {
                    tvEtapasDetalhes.setText(etapasTexto.toString());
                }

                if (layoutEtapasExpandido != null) {
                    layoutEtapasExpandido.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String obterIconeManobra(String type, String modifier) {
        if (modifier.contains("right")) return "↗️ Virar à direita";
        if (modifier.contains("left")) return "↖️ Virar à esquerda";
        if (modifier.contains("slight right")) return "↗️ Mantenha-se à direita";
        if (modifier.contains("slight left")) return "↖️ Mantenha-se à esquerda";
        if (modifier.contains("sharp right")) return "↪️ Curva acentuada à direita";
        if (modifier.contains("sharp left")) return "↩️ Curva acentuada à esquerda";
        if (modifier.contains("uturn")) return "🔄 Faça o retorno";
        return "⬆️ Siga em frente";
    }

    private void limparLinhasDeRota() {
        for (Polyline line : routePolylines) {
            map.getOverlays().remove(line);
        }
        routePolylines.clear();

        if (tvEtapasDetalhes != null) {
            tvEtapasDetalhes.setText("• Deslize para cima para ver as instruções detalhadas após calcular a rota.");
        }

        map.invalidate();
    }

    private void atualizarBotoesTransporte(Button ativo, Button inativo1, Button inativo2) {
        if (ativo != null) {
            ativo.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_purple));
            ativo.setTextColor(Color.WHITE);
        }
        if (inativo1 != null) {
            inativo1.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
            inativo1.setTextColor(Color.parseColor("#94A3B8"));
        }
        if (inativo2 != null) {
            inativo2.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
            inativo2.setTextColor(Color.parseColor("#94A3B8"));
        }
    }

    private String formatarTempoEDistancia(double segundos, double metros) {
        int minutos = (int) Math.round(segundos / 60.0);
        if (minutos < 1) minutos = 1;

        String textoDistancia = (metros >= 1000)
                ? String.format(Locale.getDefault(), "%.1f km", metros / 1000.0)
                : String.format(Locale.getDefault(), "%d m", (int) metros);

        return String.format(Locale.getDefault(), "%d min (%s)", minutos, textoDistancia);
    }
}