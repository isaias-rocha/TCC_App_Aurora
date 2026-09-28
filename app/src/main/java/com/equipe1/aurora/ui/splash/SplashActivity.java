package com.equipe1.aurora.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.equipe1.aurora.R;
import com.equipe1.aurora.databinding.ActivitySplashBinding;
import com.equipe1.aurora.ui.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int DURACAO_SPLASH = 3000; // 3 segundos
    private ActivitySplashBinding vinculo;
    private Handler manipulador;
    private Runnable acaoNavegacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vinculo = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(vinculo.getRoot());

        iniciarAnimacaoLogo();
        agendarTransicaoParaLogin();
    }

    private void iniciarAnimacaoLogo() {
        Animation animacaoEntrada = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        vinculo.ivLogo.startAnimation(animacaoEntrada);
    }

    private void agendarTransicaoParaLogin() {
        manipulador = new Handler(Looper.getMainLooper());
        acaoNavegacao = this::abrirTelaDeLogin;

        manipulador.postDelayed(acaoNavegacao, DURACAO_SPLASH);
    }

    private void abrirTelaDeLogin() {
        Intent intencao = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intencao);

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (manipulador != null && acaoNavegacao != null) {
            manipulador.removeCallbacks(acaoNavegacao);
        }
    }
}