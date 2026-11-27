package com.equipe1.aurora.ui.auth;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.equipe1.aurora.databinding.ActivityCadastroBinding;

public class CadastroActivity extends AppCompatActivity {

    // ViewBinding gerado automaticamente a partir de activity_cadastro.xml
    private ActivityCadastroBinding binding;

    // ViewModel compartilhado do fluxo de autenticação
    private AuthViewModel viewModel;

    // Instância do SharedPreferences para persistência local
    private SharedPreferences sharedPreferences;

    // Controla o estado atual de expansão do formulário de endereço
    private boolean isEnderecoExpandido = true;

    // Lista oficial das 27 Unidades Federativas (UFs) brasileiras
    private static final String[] UFS_BRASIL = {
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
            "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
            "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Infla o layout utilizando ViewBinding
        binding = ActivityCadastroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializa o ViewModel e o SharedPreferences
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        sharedPreferences = getSharedPreferences("aurora_prefs", MODE_PRIVATE);

        // Configurações iniciais da UI
        configurarDropdownEstados();
        configurarExpansaoEndereco();
        configurarConsultaCep();
        configurarObservadores();
        configurarCliques();
        aplicarMascaraTelefone();
    }

    /**
     * Popula o AutoCompleteTextView (et_estado) com a lista de UFs brasileiras.
     */
    private void configurarDropdownEstados() {
        ArrayAdapter<String> adapterUf = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                UFS_BRASIL
        );
        binding.etEstado.setAdapter(adapterUf);
    }

    /**
     * Configura o botão btn_expandir_endereco para alternar a visibilidade de form_endereco
     * e rotacionar suavemente o ícone da seta.
     */
    private void configurarExpansaoEndereco() {
        View.OnClickListener toggleListener = v -> {
            isEnderecoExpandido = !isEnderecoExpandido;

            if (isEnderecoExpandido) {
                // Exibe o formulário de endereço e gira a seta para cima (posição original)
                binding.formEndereco.setVisibility(View.VISIBLE);
                binding.btnExpandirEndereco.animate().rotation(0f).setDuration(200).start();
            } else {
                // Oculta o formulário de endereço e gira a seta 180 graus (apontando para baixo)
                binding.formEndereco.setVisibility(View.GONE);
                binding.btnExpandirEndereco.animate().rotation(180f).setDuration(200).start();
            }
        };

        // Permite expandir/recolher clicando tanto no botão da seta quanto no cabeçalho do card
        binding.btnExpandirEndereco.setOnClickListener(toggleListener);
        binding.headerEndereco.setOnClickListener(toggleListener);
    }

    /**
     * Dispara a consulta na ViaCEP ao clicar no ícone de lupa do campo CEP
     * ou automaticamente quando o usuário termina de digitar os 8 dígitos.
     */
    private void configurarConsultaCep() {
        // 1. Clique no ícone de pesquisa (endIcon) do TextInputLayout til_cep
        binding.tilCep.setEndIconOnClickListener(v ->
                viewModel.consultarCep(getTexto(binding.etCep))
        );

        // 2. Consulta automática ao atingir 8 dígitos no campo et_cep
        binding.etCep.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Limpa mensagem de erro anterior enquanto o usuário digita
                binding.tilCep.setError(null);

                String cepLimpo = s.toString().replaceAll("[^\\d]", "");
                if (cepLimpo.length() == 8) {
                    viewModel.consultarCep(cepLimpo);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Observa os LiveData expostos pelo AuthViewModel (erros, dados da ViaCEP e status do cadastro).
     */
    private void configurarObservadores() {
        // --- Observadores de Erros dos Dados Pessoais ---
        viewModel.getNomeRegError().observe(this, erro -> {
            binding.etNome.setError(erro);
            binding.etNome.requestFocus();
        });

        viewModel.getTelefoneRegError().observe(this, erro -> {
            binding.etTelefone.setError(erro);
            binding.etTelefone.requestFocus();
        });

        viewModel.getEmailRegError().observe(this, erro -> {
            binding.etEmailCadastro.setError(erro);
            binding.etEmailCadastro.requestFocus();
        });

        viewModel.getSenhaRegError().observe(this, erro -> {
            binding.etSenhaCadastro.setError(erro);
            binding.etSenhaCadastro.requestFocus();
        });

        viewModel.getConfirmarSenhaRegError().observe(this, erro -> {
            binding.etConfirmarSenhaCadastro.setError(erro);
            binding.etConfirmarSenhaCadastro.requestFocus();
        });

        // --- Observadores de Erros dos Campos Obrigatórios de Endereço ---
        viewModel.getCepRegError().observe(this, erro -> {
            if (erro != null) {
                expandirCardSeNecessario();
                binding.tilCep.setError(erro);
                binding.etCep.requestFocus();
            } else {
                binding.tilCep.setError(null);
            }
        });

        viewModel.getLogradouroRegError().observe(this, erro -> {
            expandirCardSeNecessario();
            binding.etLogradouro.setError(erro);
            binding.etLogradouro.requestFocus();
        });

        viewModel.getBairroRegError().observe(this, erro -> {
            expandirCardSeNecessario();
            binding.etBairro.setError(erro);
            binding.etBairro.requestFocus();
        });

        viewModel.getCidadeRegError().observe(this, erro -> {
            expandirCardSeNecessario();
            binding.etCidade.setError(erro);
            binding.etCidade.requestFocus();
        });

        viewModel.getEstadoRegError().observe(this, erro -> {
            expandirCardSeNecessario();
            binding.etEstado.setError(erro);
            binding.etEstado.requestFocus();
        });

        viewModel.getNumeroRegError().observe(this, erro -> {
            expandirCardSeNecessario();
            binding.etNumero.setError(erro);
            binding.etNumero.requestFocus();
        });

        // --- Observador do Retorno da ViaCEP ---
        viewModel.getEnderecoViaCep().observe(this, endereco -> {
            if (endereco != null) {
                // Preenche automaticamente os campos retornados pela ViaCEP
                binding.etLogradouro.setText(endereco.getLogradouro());
                binding.etBairro.setText(endereco.getBairro());
                binding.etCidade.setText(endereco.getCidade());

                // O parâmetro 'false' evita que o AutoCompleteTextView filtre e esconda as outras UFs
                binding.etEstado.setText(endereco.getEstado(), false);

                // Limpa possíveis erros anteriores dos campos preenchidos
                binding.etLogradouro.setError(null);
                binding.etBairro.setError(null);
                binding.etCidade.setError(null);
                binding.etEstado.setError(null);

                // Move o foco automaticamente para o campo "Número" para agilizar a digitação
                binding.etNumero.requestFocus();
            }
        });

        // --- Feedbacks Gerais e Conclusão ---
        viewModel.getMensagemToast().observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        );

        viewModel.getCadastroSucesso().observe(this, sucesso -> {
            if (Boolean.TRUE.equals(sucesso)) {
                finish();
            }
        });
    }

    /**
     * Garante que o card de endereço seja expandido caso ocorra um erro de validação enquanto estiver recolhido.
     */
    private void expandirCardSeNecessario() {
        if (!isEnderecoExpandido) {
            isEnderecoExpandido = true;
            binding.formEndereco.setVisibility(View.VISIBLE);
            binding.btnExpandirEndereco.animate().rotation(0f).setDuration(200).start();
        }
    }

    /**
     * Configura os eventos de clique da tela.
     */
    private void configurarCliques() {
        View.OnClickListener irParaLoginListener = v -> finish();

        binding.btnVoltar.setOnClickListener(irParaLoginListener);
        binding.tvIrParaLogin.setOnClickListener(irParaLoginListener);
        binding.tvIrParaLoginDesc.setOnClickListener(irParaLoginListener);

        // Envia todos os dados (pessoais + endereço) para validação e cadastro no ViewModel
        binding.btnCadastrar.setOnClickListener(v -> viewModel.executarCadastro(
                getTexto(binding.etNome),
                getTexto(binding.etTelefone),
                getTexto(binding.etEmailCadastro),
                getTexto(binding.etSenhaCadastro),
                getTexto(binding.etConfirmarSenhaCadastro),
                getTexto(binding.etCep),
                getTexto(binding.etLogradouro),
                getTexto(binding.etBairro),
                getTexto(binding.etCidade),
                getTexto(binding.etEstado),
                getTexto(binding.etNumero),
                getTexto(binding.etComplemento), // Opcional
                getTexto(binding.etApelido),     // Opcional
                sharedPreferences
        ));
    }

    /**
     * Método auxiliar genérico para extrair texto de qualquer EditText ou AutoCompleteTextView.
     */
    private String getTexto(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    /**
     * Aplica máscara dinâmica de telefone (XX) XXXXX-XXXX.
     */
    private void aplicarMascaraTelefone() {
        binding.etTelefone.addTextChangedListener(new TextWatcher() {
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
                binding.etTelefone.setText(formatted.toString());
                binding.etTelefone.setSelection(formatted.length());
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }
}