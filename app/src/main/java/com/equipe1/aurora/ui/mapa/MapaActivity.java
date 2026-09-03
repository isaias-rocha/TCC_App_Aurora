package com.equipe1.aurora.ui.mapa;

import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.equipe1.aurora.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class MapaActivity extends AppCompatActivity {
    private MapView map = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Carrega as configurações do osmdroid (obrigatório para requisições de tiles)
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


        // 2. Vincula o componente do XML (certifique-se de que o id no XML é @+id/map)
        map = findViewById(R.id.map);

        if (map != null) {
            // Define o renderizador visual padrão do OpenStreetMap
            map.setTileSource(TileSourceFactory.MAPNIK);

            // Ativa gestos de zoom (pinça na tela)
            map.setMultiTouchControls(true);

            // 3. Configura a câmera (nível de zoom e ponto inicial)
            IMapController mapController = map.getController();
            mapController.setZoom(15.0);

            // Exemplo: Coordenadas de São Paulo (-23.550520, -46.633308)
            GeoPoint startPoint = new GeoPoint(-23.550520, -46.633308);
            mapController.setCenter(startPoint);
        }
    }

    // 4. Métodos do ciclo de vida necessários para evitar vazamento de memória e atualizar tiles
    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause();
        }
    }
}