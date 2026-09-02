package com.equipe1.aurora.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.equipe1.aurora.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class EsqueceuSenhaActivity extends AppCompatActivity {
    private ImageButton btnVoltar;
    private TextInputEditText etEmailEsqueceu;
    private MaterialButton btnEnviarCodigo;
    private TextView tvVoltarLogin;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esqueceu_senha);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initComponents();
        configurarObservadores();
        configurarCliques();
    }

    private void configurarObservadores() {
        viewModel.getEmailEsqueceuError().observe(this, erro -> {
            etEmailEsqueceu.setError(erro);
            etEmailEsqueceu.requestFocus();
        });

        viewModel.getMensagemToast().observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        );

        viewModel.getEnvioEmailSucesso().observe(this, sucesso -> {
            if (sucesso) {
                startActivity(new Intent(EsqueceuSenhaActivity.this, OtpActivity.class));
                finish();
            }
        });
    }

    private void configurarCliques() {
        btnVoltar.setOnClickListener(v -> finish());
        tvVoltarLogin.setOnClickListener(v -> finish());

        btnEnviarCodigo.setOnClickListener(v -> {
            String email = etEmailEsqueceu.getText() != null ? etEmailEsqueceu.getText().toString().trim() : "";
            viewModel.enviarEmailRecuperacao(email);
        });
    }

    private void initComponents() {
        btnVoltar = findViewById(R.id.btn_voltar);
        etEmailEsqueceu = findViewById(R.id.et_email);
        btnEnviarCodigo = findViewById(R.id.btn_enviar_codigo);
        tvVoltarLogin = findViewById(R.id.tv_voltar_login);
    }
}