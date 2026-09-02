package com.equipe1.aurora.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.equipe1.aurora.R;
import com.equipe1.aurora.ui.main.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    // --- Componentes de UI ---
    private ImageButton btnVoltar;
    private TextInputEditText etEmail, etSenha;
    private CheckBox checkLembrar;
    private TextView tvEsqueceuSenha, tvIrParaCadastro;
    private MaterialButton btnLogar, btnLogarGoogle;

    // --- MVVM e Google Auth ---
    private AuthViewModel viewModel;
    private GoogleSignInClient googleSignInClient;
    private SharedPreferences preferences;

    // Launcher para capturar o resultado do Google Sign-In
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            viewModel.autenticarComGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Toast.makeText(this, "Erro Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        iniciarComponentes();
        configurarGoogleSignIn();
        configurarObservadores();
        configurarCliques();
    }

    private void configurarObservadores() {
        // Observa erros nos campos
        viewModel.getEmailLoginError().observe(this, erro -> {
            etEmail.setError(erro);
            etEmail.requestFocus();
        });

        viewModel.getSenhaLoginError().observe(this, erro -> {
            etSenha.setError(erro);
            etSenha.requestFocus();
        });

        // Observa Toasts gerais
        viewModel.getMensagemToast().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Observa Sucesso de Login
        viewModel.getLoginSucesso().observe(this, sucesso -> {
            if (sucesso) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void configurarCliques() {
        btnVoltar.setOnClickListener(v -> finish());

        btnLogar.setOnClickListener(v -> {
            String email = getTexto(etEmail);
            String senha = getTexto(etSenha);
            preferences = getSharedPreferences("login_credenciais", MODE_PRIVATE);

            viewModel.realizarLogin(email, senha, checkLembrar.isChecked(), preferences);
        });

        btnLogarGoogle.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        tvEsqueceuSenha.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, EsqueceuSenhaActivity.class))
        );

        tvIrParaCadastro.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, CadastroActivity.class))
        );
    }

    private void configurarGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("SEU_WEB_CLIENT_ID_AQUI")
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void iniciarComponentes() {
        btnVoltar = findViewById(R.id.btn_voltar);
        etEmail = findViewById(R.id.et_email);
        etSenha = findViewById(R.id.et_senha);
        checkLembrar = findViewById(R.id.check_lembrarCredenciais);
        tvEsqueceuSenha = findViewById(R.id.tv_ir_para_esqueceuSenha);
        btnLogar = findViewById(R.id.btn_logar);
        btnLogarGoogle = findViewById(R.id.btn_logar_google);
        tvIrParaCadastro = findViewById(R.id.tv_ir_para_cadastro);
    }

    private String getTexto(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}