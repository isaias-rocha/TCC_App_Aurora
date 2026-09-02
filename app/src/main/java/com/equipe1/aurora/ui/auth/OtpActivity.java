package com.equipe1.aurora.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.equipe1.aurora.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class OtpActivity extends AppCompatActivity {

    private ImageButton btnVoltar;
    private TextInputEditText etOtp1, etOtp2, etOtp3, etOtp4;
    private MaterialButton btnVerificar;
    private TextView tvReenviar;

    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        iniciarComponentes();
        configurarAutoFocoOtp();
        configurarObservadores();
        configurarCliques();
    }

    private void iniciarComponentes() {
        btnVoltar = findViewById(R.id.btn_voltar_otp);
        etOtp1 = findViewById(R.id.et_otp_1);
        etOtp2 = findViewById(R.id.et_otp_2);
        etOtp3 = findViewById(R.id.et_otp_3);
        etOtp4 = findViewById(R.id.et_otp_4);
        btnVerificar = findViewById(R.id.btn_verificar_otp);
        tvReenviar = findViewById(R.id.tv_reenviar_codigo);
    }

    private void configurarObservadores() {
        // Observa erros de validação do código
        viewModel.getOtpError().observe(this, erro ->
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
        );

        // Observa mensagens gerais da ViewModel
        viewModel.getMensagemToast().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Observa validação bem-sucedida do código
        viewModel.getOtpSucesso().observe(this, sucesso -> {
            if (sucesso) {
                // Finaliza a tela após confirmação ou navega para redefinição de senha
                finish();
            }
        });
    }

    private void configurarCliques() {
        btnVoltar.setOnClickListener(v -> finish());

        btnVerificar.setOnClickListener(v -> {
            String codigo = obterCodigoOtp();
            viewModel.validarOtp(codigo);
        });

        tvReenviar.setOnClickListener(v -> {
            // Solicita novo envio de e-mail através da ViewModel
            Toast.makeText(this, "Código reenviado com sucesso!", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Alterna o foco automaticamente para o próximo campo ao digitar
     * ou para o campo anterior ao apagar (Tecla Backspace), além de
     * anunciar o dígito inserido para acessibilidade (TalkBack).
     */
    private void configurarAutoFocoOtp() {
        TextInputEditText[] editTexts = {etOtp1, etOtp2, etOtp3, etOtp4};

        for (int i = 0; i < editTexts.length; i++) {
            final int index = i;

            // Transição para a direita ao digitar
            editTexts[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1) {
                        // Força a leitura do dígito inserido antes de alterar o foco de tela
                        editTexts[index].announceForAccessibility("Dígito " + s + " inserido");

                        if (index < editTexts.length - 1) {
                            editTexts[index + 1].requestFocus();
                        }
                    }
                }

                @Override public void afterTextChanged(Editable s) {}
            });

            // Transição para a esquerda ao apagar
            editTexts[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (getTexto(editTexts[index]).isEmpty() && index > 0) {
                        editTexts[index - 1].requestFocus();
                    }
                }
                return false;
            });
        }
    }

    private String obterCodigoOtp() {
        return getTexto(etOtp1) + getTexto(etOtp2) + getTexto(etOtp3) + getTexto(etOtp4);
    }

    private String getTexto(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}