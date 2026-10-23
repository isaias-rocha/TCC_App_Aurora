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
import android.util.Log;
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
import com.equipe1.aurora.domain.model.AreaRisco;
import com.equipe1.aurora.domain.model.BairroPoligono;
import com.equipe1.aurora.domain.model.OcorrenciaCrime;
import com.equipe1.aurora.rede.api.ApiClient;
import com.equipe1.aurora.rede.api.ApiService;
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
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapaFragment extends Fragment {

    // =========================================================================
    // 1. VARIÁVEIS E COMPONENTES
    // =========================================================================

    private final List<Overlay> camadasRiscosList = new ArrayList<>();
    private boolean exibirAreasRisco = true;
    private FloatingActionButton btnToggleRiscos;

    private MapView map;
    private MyLocationNewOverlay myLocationOverlay;
    private Marker selectedMarker;

    private final List<Polyline> routePolylines = new ArrayList<>();
    private final List<JSONObject> routesJsonList = new ArrayList<>();
    private boolean isNavigating = false;
    private String currentTransportProfile = "driving";

    private TextView tvNomeDoLocal, tvLocalizacaoEstado, tvCoordenadas, tvEtapasDetalhes;
    private EditText etSearch;
    private FloatingActionButton btnNavigate, btnMyLocation;
    private Button btnModeCar, btnModeWalk, btnModeBike;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private LinearLayout layoutEtapasExpandido;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if ((fineLocationGranted != null && fineLocationGranted) ||
                        (coarseLocationGranted != null && coarseLocationGranted)) {
                    ativarLocalizacaoUsuario();
                } else if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Permissão de localização negada.", Toast.LENGTH_SHORT).show();
                }
            });

    // =========================================================================
    // 2. CICLO DE VIDA DO FRAGMENT
    // =========================================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        View root = inflater.inflate(R.layout.fragment_mapa, container, false);

        inicializarViews(root);

        View bottomSheetView = root.findViewById(R.id.bottomSheet);
        if (bottomSheetView != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(17.0);

        setupMapClickListener();
        setupSearchListener();
        setupTransportModeListeners();
        setupButtonListeners();

        processarBuscaRecebida();
        checarPermissoesELocalizar();

        carregarTodasAsCamadasDeRisco();

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

        btnToggleRiscos = root.findViewById(R.id.btnToggleRiscos);
        if (btnToggleRiscos != null) {
            btnToggleRiscos.setOnClickListener(v -> alternarVisualizacaoRiscos());
        }

        btnModeCar = root.findViewById(R.id.btnModeCar);
        btnModeWalk = root.findViewById(R.id.btnModeWalk);
        btnModeBike = root.findViewById(R.id.btnModeBike);
    }

    private void setupButtonListeners() {
        if (btnMyLocation != null) {
            btnMyLocation.setOnClickListener(v -> centralizarNaLocalizacaoAtual());
        }

        if (btnNavigate != null) {
            btnNavigate.setOnClickListener(v -> executarCalculoDeRota());
        }
    }

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

            if (selectedMarker != null) {
                executarCalculoDeRota();
            }
        };

        if (btnModeCar != null) btnModeCar.setOnClickListener(listener);
        if (btnModeWalk != null) btnModeWalk.setOnClickListener(listener);
        if (btnModeBike != null) btnModeBike.setOnClickListener(listener);
    }

    // =========================================================================
    // 4. PERMISSÕES E GEOLOCALIZAÇÃO
    // =========================================================================

    private void checarPermissoesELocalizar() {
        if (!isAdded() || getContext() == null) return;

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
        if (!isAdded() || getContext() == null || map == null) return;

        GpsMyLocationProvider provider = new GpsMyLocationProvider(requireContext());

        myLocationOverlay = new MyLocationNewOverlay(provider, map) {
            @Override
            public void onLocationChanged(android.location.Location location, org.osmdroid.views.overlay.mylocation.IMyLocationProvider source) {
                super.onLocationChanged(location, source);

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
        } else if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), "Buscando sinal de GPS...", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // 5. TOQUES NO MAPA E PESQUISA
    // =========================================================================

    private void setupMapClickListener() {
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                isNavigating = false;
                limparLinhasDeRota();

                if (myLocationOverlay != null) {
                    myLocationOverlay.disableFollowLocation();
                }

                adicionarOuMoverMarcador(p);
                obterEnderecoGeocodificado(p);

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
        if (query.isEmpty() || !isAdded() || getContext() == null) return;

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
    // 6. CÁLCULO E ROTAS
    // =========================================================================

    private void executarCalculoDeRota() {
        if (selectedMarker == null) {
            if (isAdded() && getContext() != null) Toast.makeText(getContext(), "Selecione um ponto no mapa primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }

        GeoPoint destino = selectedMarker.getPosition();
        GeoPoint origem = (myLocationOverlay != null) ? myLocationOverlay.getMyLocation() : null;

        if (origem == null) {
            if (isAdded() && getContext() != null) Toast.makeText(getContext(), "Aguardando sinal do GPS...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isAdded() && getContext() != null) Toast.makeText(getContext(), "Calculando rotas...", Toast.LENGTH_SHORT).show();
        calcularETracarRota(origem, destino, currentTransportProfile);
    }

    private void calcularETracarRota(GeoPoint origem, GeoPoint destino, String profile) {
        Executors.newSingleThreadExecutor().execute(() -> {
            routesJsonList.clear();
            HttpURLConnection conn = null;

            try {
                String urlString = String.format(Locale.US,
                        "https://router.project-osrm.org/route/v1/%s/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&alternatives=true&steps=true",
                        profile,
                        origem.getLongitude(), origem.getLatitude(),
                        destino.getLongitude(), destino.getLatitude());

                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
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
                Log.e("MAPA_ERRO", "Erro no cálculo OSRM", e);
            } finally {
                if (conn != null) conn.disconnect();
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded() || getContext() == null) return;

                if (!routesJsonList.isEmpty()) {
                    exibirRotasNoMapa(0);
                    isNavigating = true;

                    if (myLocationOverlay != null) {
                        myLocationOverlay.enableFollowLocation();
                    }
                } else {
                    Toast.makeText(getContext(), "Não foi possível encontrar rotas.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

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
                    polyline.getOutlinePaint().setColor(Color.parseColor("#6C5CE7"));
                    polyline.getOutlinePaint().setStrokeWidth(16f);

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
                    polyline.getOutlinePaint().setColor(Color.parseColor("#64748B"));
                    polyline.getOutlinePaint().setStrokeWidth(10f);
                }

                polyline.setOnClickListener((p, mapView, eventPos) -> {
                    exibirRotasNoMapa(routeIndex);
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Rota " + (routeIndex + 1) + " selecionada", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });

                routePolylines.add(polyline);

                if (isSelected) {
                    map.getOverlays().add(polyline);
                } else {
                    map.getOverlays().add(0, polyline);
                }

            } catch (Exception e) {
                Log.e("MAPA_ERRO", "Erro ao exibir rota no mapa", e);
            }
        }

        map.invalidate();
    }

    private void atualizarProgressoEmTempoReal(GeoPoint posicaoAtual, GeoPoint destino) {
        double distanciaRestanteMetros = posicaoAtual.distanceToAsDouble(destino);
        double tempoRestanteSegundos = distanciaRestanteMetros / 8.3;

        if (tvNomeDoLocal != null) {
            tvNomeDoLocal.setText(formatarTempoEDistancia(tempoRestanteSegundos, distanciaRestanteMetros));
        }

        if (distanciaRestanteMetros < 15.0) {
            isNavigating = false;
            if (tvLocalizacaoEstado != null) {
                tvLocalizacaoEstado.setText("Você chegou ao seu destino! 🎉");
            }
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Você chegou ao seu destino!", Toast.LENGTH_LONG).show();
            }
        }
    }

    // =========================================================================
    // 7. MÉTODOS AUXILIARES E INTEGRAÇÃO DE ÁREAS DE RISCO, BAIRROS E CRIMES
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
        if (!isAdded() || getContext() == null) return;

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
            Log.e("MAPA_ERRO", "Erro ao formatar etapas da rota", e);
        }
    }

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
        if (!isAdded() || getContext() == null) return;

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

    // --- CARREGAR TODAS AS CAMADAS DE RISCO ---
    private void carregarTodasAsCamadasDeRisco() {
        removerCamadasDeRiscoDoMapa();
        carregarAreasDeRisco();
        carregarPoligonosBairros();
        carregarOcorrenciasCrimes();
    }

    // 1. Círculos e Pontos de Áreas de Risco
    private void carregarAreasDeRisco() {
        ApiService apiService = ApiClient.getApiService();

        apiService.listarRiscos().enqueue(new Callback<List<AreaRisco>>() {
            @Override
            public void onResponse(Call<List<AreaRisco>> call, Response<List<AreaRisco>> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<AreaRisco> listaRiscos = response.body();

                    for (AreaRisco risco : listaRiscos) {
                        try {
                            double latitude = risco.getLatitude();
                            double longitude = risco.getLongitude();

                            if (latitude == 0.0 || longitude == 0.0) continue;

                            GeoPoint ponto = new GeoPoint(latitude, longitude);

                            Marker marker = new Marker(map);
                            marker.setPosition(ponto);
                            marker.setTitle(risco.getNomeArea() + (risco.getTipoCrime() != null ? " (" + risco.getTipoCrime() + ")" : ""));
                            marker.setSnippet(risco.getDescricaoArea());
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                            configurarIconePorCrime(marker, risco.getTipoCrime());

                            map.getOverlays().add(marker);
                            camadasRiscosList.add(marker);

                            Polygon circle = new Polygon(map);
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

                            map.getOverlays().add(circle);
                            camadasRiscosList.add(circle);

                        } catch (Exception e) {
                            Log.e("MAPA_ERRO", "Erro ao processar área de risco: " + e.getMessage());
                        }
                    }
                    map.invalidate();
                }
            }

            @Override
            public void onFailure(Call<List<AreaRisco>> call, Throwable t) {
                Log.e("MAPA_ERRO", "Falha na conexão com a API de riscos: " + t.getMessage());
            }
        });
    }

    // 2. Polígonos dos Bairros com Cores por Nível de Risco
    private void carregarPoligonosBairros() {
        ApiService apiService = ApiClient.getApiService();

        apiService.listarBairros().enqueue(new Callback<List<BairroPoligono>>() {
            @Override
            public void onResponse(Call<List<BairroPoligono>> call, Response<List<BairroPoligono>> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    for (BairroPoligono bairro : response.body()) {

                        // ✅ Pega a lista de GeoPoints diretamente da classe de modelo
                        List<GeoPoint> pontosGeo = bairro.getCoordenadasVertices();

                        if (pontosGeo == null || pontosGeo.isEmpty()) continue;

                        Polygon poligono = new Polygon(map);
                        poligono.setPoints(pontosGeo); // Passa a lista direto para o OSMDroid
                        poligono.setTitle("Bairro: " + bairro.getNomeBairro() + " (Risco: " + bairro.getNivelRisco() + ")");

                        String nivelRisco = bairro.getNivelRisco() != null ? bairro.getNivelRisco().toUpperCase() : "MEDIO";
                        switch (nivelRisco) {
                            case "ALTO":
                                poligono.setFillColor(Color.parseColor("#4DFF0000")); // Vermelho (30%)
                                poligono.setStrokeColor(Color.parseColor("#FF0000"));
                                break;
                            case "MEDIO":
                                poligono.setFillColor(Color.parseColor("#4DFFFF00")); // Amarelo (30%)
                                poligono.setStrokeColor(Color.parseColor("#D4AC0D"));
                                break;
                            default: // "BAIXO"
                                poligono.setFillColor(Color.parseColor("#4D00FF00")); // Verde (30%)
                                poligono.setStrokeColor(Color.parseColor("#00FF00"));
                                break;
                        }

                        poligono.setStrokeWidth(3f);

                        // Adiciona na posição 0 para ficar atrás dos marcadores e rotas
                        map.getOverlays().add(0, poligono);
                        camadasRiscosList.add(poligono);
                    }
                    map.invalidate();
                }
            }

            @Override
            public void onFailure(Call<List<BairroPoligono>> call, Throwable t) {
                Log.e("MAPA_ERRO", "Falha na conexão com a API de bairros: " + t.getMessage());
            }
        });
    }

    // 3. Marcadores de Ocorrências e Crimes Importados
    private void carregarOcorrenciasCrimes() {
        ApiService apiService = ApiClient.getApiService();

        apiService.listarCrimes().enqueue(new Callback<List<OcorrenciaCrime>>() {
            @Override
            public void onResponse(Call<List<OcorrenciaCrime>> call, Response<List<OcorrenciaCrime>> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    for (OcorrenciaCrime crime : response.body()) {
                        GeoPoint ponto = new GeoPoint(crime.getLatitude(), crime.getLongitude());

                        Marker marker = new Marker(map);
                        marker.setPosition(ponto);
                        marker.setTitle(crime.getTipoCrime());
                        marker.setSnippet(crime.getDescricao());
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                        configurarIconePorCrime(marker, crime.getTipoCrime());

                        map.getOverlays().add(marker);
                        camadasRiscosList.add(marker);
                    }
                    map.invalidate();
                }
            }

            @Override
            public void onFailure(Call<List<OcorrenciaCrime>> call, Throwable t) {
                Log.e("MAPA_ERRO", "Falha na conexão com a API de crimes: " + t.getMessage());
            }
        });
    }

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

    private void alternarVisualizacaoRiscos() {
        if (!isAdded() || getContext() == null) return;

        exibirAreasRisco = !exibirAreasRisco;

        if (exibirAreasRisco) {
            carregarTodasAsCamadasDeRisco();
            Toast.makeText(getContext(), "Áreas e Bairros de Risco Ativados", Toast.LENGTH_SHORT).show();
            if (btnToggleRiscos != null) {
                btnToggleRiscos.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_dark));
            }
        } else {
            removerCamadasDeRiscoDoMapa();
            map.invalidate();
            Toast.makeText(getContext(), "Áreas de Risco Ocultadas", Toast.LENGTH_SHORT).show();
            if (btnToggleRiscos != null) {
                btnToggleRiscos.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
            }
        }
    }

    private void removerCamadasDeRiscoDoMapa() {
        for (Overlay overlay : camadasRiscosList) {
            map.getOverlays().remove(overlay);
        }
        camadasRiscosList.clear();
    }
}