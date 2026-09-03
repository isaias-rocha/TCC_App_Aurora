package com.equipe1.aurora.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.equipe1.aurora.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class CadastroActivity extends AppCompatActivity {
    private ImageButton btnVoltar;
    private TextInputEditText etNome, etTelefone, etEmailCadastro, etSenhaCadastro, etConfirmarSenha;
    private MaterialButton btnCadastrar;
    private TextView tvVoltarLogin, tvVoltarLoginDesc;

    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initComponents();
        configurarObservadores();
        configurarCliques();
        aplicarMascaraTelefone();
    }


    private void configurarObservadores() {
        // Observadores de erros dos campos de formulário
        viewModel.getNomeRegError().observe(this, erro ->           { etNome.setError(erro); etNome.requestFocus(); });
        viewModel.getTelefoneRegError().observe(this, erro ->       { etTelefone.setError(erro); etTelefone.requestFocus(); });
        viewModel.getEmailRegError().observe(this, erro ->          { etEmailCadastro.setError(erro); etEmailCadastro.requestFocus(); });
        viewModel.getSenhaRegError().observe(this, erro ->          { etSenhaCadastro.setError(erro); etSenhaCadastro.requestFocus(); });
        viewModel.getConfirmarSenhaRegError().observe(this, erro -> { etConfirmarSenha.setError(erro); etConfirmarSenha.requestFocus(); });

        viewModel.getMensagemToast().observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        );

        viewModel.getCadastroSucesso().observe(this, sucesso -> {
            if (sucesso) finish();
        });
    }

    private void configurarCliques() {
        View.OnClickListener irParaLoginListener = v -> finish();

        btnVoltar.setOnClickListener(irParaLoginListener);
        tvVoltarLogin.setOnClickListener(irParaLoginListener);
        tvVoltarLoginDesc.setOnClickListener(irParaLoginListener);

        btnCadastrar.setOnClickListener(v -> viewModel.validarCadastro(
                getTexto(etNome),
                getTexto(etTelefone),
                getTexto(etEmailCadastro),
                getTexto(etSenhaCadastro),
                getTexto(etConfirmarSenha)
        ));
    }

    private void initComponents() {
        btnVoltar = findViewById(R.id.btn_voltar);
        etNome = findViewById(R.id.et_nome);
        etEmailCadastro = findViewById(R.id.et_email_cadastro);
        etTelefone = findViewById(R.id.et_telefone);
        etSenhaCadastro = findViewById(R.id.et_senha_cadastro);
        etConfirmarSenha = findViewById(R.id.et_confirmar_senha_cadastro);
        btnCadastrar = findViewById(R.id.btn_cadastrar);
        tvVoltarLogin = findViewById(R.id.tv_ir_para_login);
        tvVoltarLoginDesc = findViewById(R.id.tv_ir_para_login_desc);
    }

    private String getTexto(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void aplicarMascaraTelefone() {
        etTelefone.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdating) { isUpdating = false; return; }

                String unformatted = s.toString().replaceAll("[^\\d]", "");
                StringBuilder formatted = new StringBuilder();

                if (unformatted.length() > 0) {
                    formatted.append("(");
                    if (unformatted.length() <= 2) {
                        formatted.append(unformatted);
                    } else {
                        formatted.append(unformatted.substring(0, 2)).append(") ");
                        if (unformatted.length() <= 11) {
                            formatted.append(unformatted.substring(2));
                        } else {
                            formatted.append(unformatted.substring(2, 11))
                                    .append("-")
                                    .append(unformatted.substring(11, Math.min(unformatted.length(), 11)));
                        }
                    }
                }

                isUpdating = true;
                etTelefone.setText(formatted.toString());
                etTelefone.setSelection(formatted.length());
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }
}