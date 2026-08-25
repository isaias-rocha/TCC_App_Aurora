package com.equipe1.aurora.ui.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.equipe1.aurora.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    // 1. Declaração das variáveis dos componentes da tela
    private TextInputEditText etNome;
    private TextInputEditText etTelefone;
    private TextInputEditText etEmailCadastro;
    private TextInputEditText etSenhaCadastro;
    private TextInputEditText etConfirmarSenha;
    private MaterialButton btnCadastrar;
    private TextView tvVoltarLogin;
    private TextView tvVoltarLoginDesc;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initComponents();

        // 3. Aplicação da Máscara Automática de Telefone: (XX) XXXXX-XXXX
        aplicarMascaraTelefone();

        // 4. Configuração de clique para realizar o cadastro
        btnCadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executarCadastro();
            }
        });

        // 5. Navegação: Volta para a tela de Login ao clicar no texto principal ou na descrição
        View.OnClickListener irParaLoginListener = v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Fecha a tela atual para não acumular na pilha
        };

        tvVoltarLogin.setOnClickListener(irParaLoginListener);
        tvVoltarLoginDesc.setOnClickListener(irParaLoginListener);
    }

    // 2. Inicialização das views encontradas no XML
    private void initComponents() {
        etNome              = findViewById(R.id.et_nome);
        etTelefone          = findViewById(R.id.et_telefone);
        etEmailCadastro     = findViewById(R.id.et_email_cadastro);
        etSenhaCadastro     = findViewById(R.id.et_senha_cadastro);
        etConfirmarSenha    = findViewById(R.id.et_confirmar_senha_cadastro);

        btnCadastrar        = findViewById(R.id.btn_cadastrar);
        tvVoltarLogin       = findViewById(R.id.tv_ir_para_login);
        tvVoltarLoginDesc   = findViewById(R.id.tv_ir_para_login_desc);
    }

    /**
     * Método responsável por formatar o campo de telefone dinamicamente enquanto a pessoa digita.
     */
    private void aplicarMascaraTelefone() {
        etTelefone.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdating) {
                    isUpdating = false;
                    return;
                }

                // Remove tudo que não for número digitado
                String unformatted = s.toString().replaceAll("[^\\d]", "");
                StringBuilder formatted = new StringBuilder();

                // Monta a máscara gradativamente dependendo da quantidade de dígitos
                if (unformatted.length() > 0) {
                    formatted.append("(");
                    if (unformatted.length() <= 2) {
                        formatted.append(unformatted);
                    } else {
                        formatted.append(unformatted.substring(0, 2)).append(") ");
                        if (unformatted.length() <= 7) {
                            formatted.append(unformatted.substring(2));
                        } else {
                            formatted.append(unformatted.substring(2, 7))
                                    .append("-")
                                    .append(unformatted.substring(7, Math.min(unformatted.length(), 11)));
                        }
                    }
                }

                isUpdating = true;
                etTelefone.setText(formatted.toString());
                etTelefone.setSelection(formatted.length()); // Mantém o cursor de digitação no final
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Método que valida as entradas de texto e processa a criação da conta.
     */
    private void executarCadastro() {
        String nome           = etNome.getText().toString().trim();
        String telefone       = etTelefone.getText().toString().trim();
        String email          = etEmailCadastro.getText().toString().trim();
        String senha          = etSenhaCadastro.getText().toString().trim();
        String confirmarSenha = etConfirmarSenha.getText().toString().trim();

        // Validação: Nome
        if (TextUtils.isEmpty(nome)) {
            etNome.setError("O nome é obrigatório");
            etNome.requestFocus();
            return;
        }

        // Validação: Telefone vazio
        if (TextUtils.isEmpty(telefone)) {
            etTelefone.setError("O telefone é obrigatório");
            etTelefone.requestFocus();
            return;
        }

        // Validação: Extrai apenas os números para conferir se tem DDD + 8 ou 9 dígitos
        String numerosTelefone = telefone.replaceAll("[^\\d]", "");
        if (numerosTelefone.length() < 10) {
            etTelefone.setError("Insira um telefone válido com o DDD completo");
            etTelefone.requestFocus();
            return;
        }

        // Validação: E-mail
        if (TextUtils.isEmpty(email)) {
            etEmailCadastro.setError("O e-mail é obrigatório");
            etEmailCadastro.requestFocus();
            return;
        }

        // Validação básica de formato de e-mail
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailCadastro.setError("Insira um e-mail válido");
            etEmailCadastro.requestFocus();
            return;
        }

        // Validação: Senha - caso esteja vazio
        if (TextUtils.isEmpty(senha)) {
            etSenhaCadastro.setError("A senha é obrigatória");
            etSenhaCadastro.requestFocus();
            return;
        }

        // Validação: Senha - caso for menor que 8 digitos
        if (senha.length() < 8) {
            etSenhaCadastro.setError("A senha deve ter pelo menos 8 caracteres");
            etSenhaCadastro.requestFocus();
            return;
        }

        // Validação: Confirmar Senha preenchida
        if (TextUtils.isEmpty(confirmarSenha)) {
            etConfirmarSenha.setError("Confirme sua senha");
            etConfirmarSenha.requestFocus();
            return;
        }

        // Validação: Igualdade das senhas usando .equals() para Strings
        if (!senha.equals(confirmarSenha)) {
            etConfirmarSenha.setError("As senhas não estão iguais");
            etConfirmarSenha.requestFocus();
            return;
        }

        // TODO: Enviar nome, telefone, email e senha para o seu Banco de Dados / Firebase
        Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_LONG).show();
        finish();
    }
}