package com.equipe1.aurora.ui.auth;

import static com.equipe1.aurora.R.id.btn_logar;
import static com.equipe1.aurora.R.id.tv_ir_para_cadastro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.equipe1.aurora.R;
import com.equipe1.aurora.ui.home.HomeActivity;
import com.equipe1.aurora.ui.main.MainActivity;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
// variáveis
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
        login.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
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


    private void iniciarComponentes() {

        email =  findViewById(R.id.et_email);
        senha =  findViewById(R.id.et_senha);
        login = findViewById(btn_logar);
        loginGoogle = findViewById(R.id.btn_logar_google);
        cadastrarUsuario = findViewById(tv_ir_para_cadastro);
        checkBox = findViewById(R.id.check_lembrarCredenciais);
        esqueceuSenha = findViewById(R.id.tv_ir_para_esqueceuSenha);

    }


}
