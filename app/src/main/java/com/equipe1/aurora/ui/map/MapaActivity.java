package com.equipe1.aurora.ui.map;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.equipe1.aurora.R;
import com.equipe1.aurora.domain.model.BairroPoligono;
import com.equipe1.aurora.domain.model.OcorrenciaCrime;
import com.equipe1.aurora.rede.api.ApiClient;
import com.equipe1.aurora.rede.api.ApiService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapaActivity extends AppCompatActivity {
    private MapView map = null;
    private FloatingActionButton btnToggleRiscos;
    private boolean exibirAreasRisco = true;

    private final List<Overlay> camadasRiscosList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        );

        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_mapa);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.map), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        map = findViewById(R.id.map);
        btnToggleRiscos = findViewById(R.id.btnToggleRiscos);

        if (btnToggleRiscos != null) {
            btnToggleRiscos.setOnClickListener(v -> alternarVisualizacaoRiscos());
        }

        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);

            IMapController mapController = map.getController();
            mapController.setZoom(15.0);

            GeoPoint startPoint = new GeoPoint(-23.550520, -46.633308);
            mapController.setCenter(startPoint);
        }

        carregarDadosDoMapa();
    }

    private void carregarDadosDoMapa() {
        ApiService apiService = ApiClient.getApiService();
        removerCamadasDeRiscoDoMapa();

        // 1. Carrega e desenha os Polígonos dos Bairros
        apiService.listarBairros().enqueue(new Callback<List<BairroPoligono>>() {
            @Override
            public void onResponse(Call<List<BairroPoligono>> call, Response<List<BairroPoligono>> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    for (BairroPoligono bairro : response.body()) {
                        try {
                            List<GeoPoint> pontosGeo = bairro.getCoordenadasVertices();

                            if (pontosGeo != null && !pontosGeo.isEmpty()) {
                                Polygon poligono = new Polygon(map);
                                poligono.setPoints(pontosGeo);
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
                                map.getOverlays().add(0, poligono);
                                camadasRiscosList.add(poligono);
                            }
                        } catch (Exception e) {
                            Log.e("MAPA_ERRO", "Erro ao desenhar polígono do bairro: " + e.getMessage());
                        }
                    }
                    map.invalidate();
                }
            }

            @Override
            public void onFailure(Call<List<BairroPoligono>> call, Throwable t) {
                Log.e("MAPA_ERRO", "Falha ao carregar bairros: " + t.getMessage());
            }
        });

        // 2. Carrega e plota os Ícones de Crimes por cima dos bairros
        apiService.listarCrimes().enqueue(new Callback<List<OcorrenciaCrime>>() {
            @Override
            public void onResponse(Call<List<OcorrenciaCrime>> call, Response<List<OcorrenciaCrime>> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    for (OcorrenciaCrime crime : response.body()) {
                        try {
                            GeoPoint ponto = new GeoPoint(crime.getLatitude(), crime.getLongitude());

                            Marker marker = new Marker(map);
                            marker.setPosition(ponto);
                            marker.setTitle(crime.getTipoCrime());
                            marker.setSnippet(crime.getDescricao());
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                            configurarIconePorCrime(marker, crime.getTipoCrime());

                            map.getOverlays().add(marker);
                            camadasRiscosList.add(marker);
                        } catch (Exception e) {
                            Log.e("MAPA_ERRO", "Erro ao plotar crime: " + e.getMessage());
                        }
                    }
                    map.invalidate();
                }
            }

            @Override
            public void onFailure(Call<List<OcorrenciaCrime>> call, Throwable t) {
                Log.e("MAPA_ERRO", "Falha ao carregar crimes: " + t.getMessage());
            }
        });
    }

    private void configurarIconePorCrime(Marker marker, String tipoCrime) {
        if (tipoCrime == null) return;

        String tipo = tipoCrime.toLowerCase();
        if (tipo.contains("roubo") || tipo.contains("assalto")) {
            marker.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces));
        } else if (tipo.contains("furto")) {
            marker.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_info_details));
        } else {
            marker.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_alert));
        }
    }

    private void alternarVisualizacaoRiscos() {
        exibirAreasRisco = !exibirAreasRisco;

        if (exibirAreasRisco) {
            carregarDadosDoMapa();
            Toast.makeText(this, "Áreas de Risco Ativadas", Toast.LENGTH_SHORT).show();
            if (btnToggleRiscos != null) {
                btnToggleRiscos.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));
            }
        } else {
            removerCamadasDeRiscoDoMapa();
            map.invalidate();
            Toast.makeText(this, "Áreas de Risco Ocultadas", Toast.LENGTH_SHORT).show();
            if (btnToggleRiscos != null) {
                btnToggleRiscos.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
            }
        }
    }

    private void removerCamadasDeRiscoDoMapa() {
        for (Overlay overlay : camadasRiscosList) {
            map.getOverlays().remove(overlay);
        }
        camadasRiscosList.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}