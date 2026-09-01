package com.equipe1.aurora.ui.auth;

import static com.equipe1.aurora.R.id.btn_logar;
import static com.equipe1.aurora.R.id.tv_ir_para_cadastro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.equipe1.aurora.R;
import com.equipe1.aurora.ui.home.HomeActivity;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
// variáveis

    ImageButton voltar;
    Button login, loginGoogle;
    TextView esqueceuSenha, cadastrarUsuario;
    TextInputEditText email, senha;
    CheckBox checkBox;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        iniciarComponentes();

        // ações de clique
        voltar.setOnClickListener((view -> {
            finish(); // sair do app
        }));

        login.setOnClickListener(v -> {
            if (validarDados() ) {
                if (checkBox.isChecked())   {
                    preferences = (SharedPreferences) getSharedPreferences ("login_Credenciais", 0); // nome do arquivo, modo privado (guardar na pasta do app.)
                    SharedPreferences.Editor dados = preferences.edit();
                    dados.putString("email: ", email.getText().toString() );
                    dados.putString("senha: ", senha.getText().toString() );

                }
            }
            // limpar 0 campo
            email.setText("");
            senha.setText("");

            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            // Abrir a nova tela
            startActivity(intent);
        });

        cadastrarUsuario.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            // Abrir a nova tela cadastro
            startActivity(intent);
        });

        esqueceuSenha.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, EsqueceuSenhaActivity.class);
            // Abrir a nova tela esqueceuSenha
            startActivity(intent);
        });

        checkBox.setOnClickListener(view -> {
            checkBox.isChecked();

        } );
    }

    // validar campo email e senha, caso estejam vazio
    private boolean validarDados() {
        boolean retorno = true;

        if (email.getText().toString().isEmpty() )  {
            retorno = false;
            email.setError("Campo Nome não pode estar vazio!");
        }
        if (senha.getText().toString().isEmpty() )  {
            retorno = false;
            email.setError("Campo Nome não pode estar vázio!");
        }
        return retorno;
    }

    private void iniciarComponentes() {

        email =  findViewById(R.id.et_email);
        senha =  findViewById(R.id.et_senha);
        login = findViewById(btn_logar);
        loginGoogle = findViewById(R.id.btn_logar_google);
        voltar = findViewById(R.id.btn_voltar);
        cadastrarUsuario = findViewById(tv_ir_para_cadastro);
        checkBox = findViewById(R.id.check_lembrarCredenciais);
        esqueceuSenha = findViewById(R.id.tv_ir_para_esqueceuSenha);
    }
}