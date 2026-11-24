package com.equipe1.aurora.ui.auth;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.equipe1.aurora.domain.model.Endereco;
import com.equipe1.aurora.domain.repository.EnderecoRepository;
import com.equipe1.aurora.domain.usecase.ConsultarCepUseCase;
import com.equipe1.aurora.infra.dto.UsuarioDto;
import com.equipe1.aurora.infra.network.api.ApiClient;
import com.equipe1.aurora.infra.repository.EnderecoRepositoryImpl;
import com.equipe1.aurora.infra.storage.EnderecoPreferencesStorage;

public class AuthViewModel extends ViewModel {

    // Caso de uso de consulta de CEP (Clean Architecture)
    private final ConsultarCepUseCase consultarCepUseCase;

    public AuthViewModel() {
        // Inicializa o caso de uso com a implementação concreta do repositório ViaCEP
        this.consultarCepUseCase = new ConsultarCepUseCase(new EnderecoRepositoryImpl());
    }

    // --- Estados para Login ---
    private final MutableLiveData<String> emailLoginError = new MutableLiveData<>();
    private final MutableLiveData<String> senhaLoginError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loginSucesso = new MutableLiveData<>();
    private final MutableLiveData<UsuarioDto> usuarioLogado = new MutableLiveData<>();

    // --- Estados para Cadastro (Dados Pessoais) ---
    private final MutableLiveData<String> nomeRegError = new MutableLiveData<>();
    private final MutableLiveData<String> telefoneRegError = new MutableLiveData<>();
    private final MutableLiveData<String> emailRegError = new MutableLiveData<>();
    private final MutableLiveData<String> senhaRegError = new MutableLiveData<>();
    private final MutableLiveData<String> confirmarSenhaRegError = new MutableLiveData<>();

    // --- Estados para Cadastro (Card de Endereço - Campos Obrigatórios) ---
    private final MutableLiveData<String> cepRegError = new MutableLiveData<>();
    private final MutableLiveData<String> logradouroRegError = new MutableLiveData<>();
    private final MutableLiveData<String> bairroRegError = new MutableLiveData<>();
    private final MutableLiveData<String> cidadeRegError = new MutableLiveData<>();
    private final MutableLiveData<String> estadoRegError = new MutableLiveData<>();
    private final MutableLiveData<String> numeroRegError = new MutableLiveData<>();

    // --- Estado do Retorno da ViaCEP ---
    private final MutableLiveData<Endereco> enderecoViaCep = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cadastroSucesso = new MutableLiveData<>();

    // --- Estados para Recuperação de Senha e OTP ---
    private final MutableLiveData<String> emailEsqueceuError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> envioEmailSucesso = new MutableLiveData<>();
    private final MutableLiveData<String> otpError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> otpSucesso = new MutableLiveData<>();

    // --- Estado para Mensagens Gerais ---
    private final MutableLiveData<String> mensagemToast = new MutableLiveData<>();

    // ============================================================================================
    // GETTERS PÚBLICOS (LiveData)
    // ============================================================================================

    public LiveData<UsuarioDto> getUsuarioLogado() { return usuarioLogado; }
    public LiveData<String> getEmailLoginError() { return emailLoginError; }
    public LiveData<String> getSenhaLoginError() { return senhaLoginError; }
    public LiveData<Boolean> getLoginSucesso() { return loginSucesso; }

    public LiveData<String> getNomeRegError() { return nomeRegError; }
    public LiveData<String> getTelefoneRegError() { return telefoneRegError; }
    public LiveData<String> getEmailRegError() { return emailRegError; }
    public LiveData<String> getSenhaRegError() { return senhaRegError; }
    public LiveData<String> getConfirmarSenhaRegError() { return confirmarSenhaRegError; }

    public LiveData<String> getCepRegError() { return cepRegError; }
    public LiveData<String> getLogradouroRegError() { return logradouroRegError; }
    public LiveData<String> getBairroRegError() { return bairroRegError; }
    public LiveData<String> getCidadeRegError() { return cidadeRegError; }
    public LiveData<String> getEstadoRegError() { return estadoRegError; }
    public LiveData<String> getNumeroRegError() { return numeroRegError; }

    public LiveData<Endereco> getEnderecoViaCep() { return enderecoViaCep; }
    public LiveData<Boolean> getCadastroSucesso() { return cadastroSucesso; }

    public LiveData<String> getEmailEsqueceuError() { return emailEsqueceuError; }
    public LiveData<Boolean> getEnvioEmailSucesso() { return envioEmailSucesso; }
    public LiveData<String> getOtpError() { return otpError; }
    public LiveData<Boolean> getOtpSucesso() { return otpSucesso; }
    public LiveData<String> getMensagemToast() { return mensagemToast; }

    // ============================================================================================
    // CONSULTA DE CEP (VIACEP)
    // ============================================================================================

    /**
     * Aciona o caso de uso para consultar o CEP na API ViaCEP e emite os dados para a Activity.
     */
    public void consultarCep(String cep) {
        // Limpa erro anterior do campo CEP antes da nova consulta
        cepRegError.setValue(null);

        consultarCepUseCase.executar(cep, new EnderecoRepository.ConsultaCepCallback() {
            @Override
            public void onSucesso(Endereco endereco) {
                // Emite o endereço preenchido para a Activity atualizar os campos da tela
                enderecoViaCep.setValue(endereco);
                mensagemToast.setValue("Endereço encontrado!");
            }

            @Override
            public void onErro(String mensagemErro) {
                // Exibe o erro no campo CEP e notifica via Toast
                cepRegError.setValue(mensagemErro);
                mensagemToast.setValue(mensagemErro);
            }
        });
    }

    // ============================================================================================
    // LOGIN E AUTENTICAÇÃO
    // ============================================================================================


    public void realizarLogin(String email, String senha, boolean lembrarCredenciais, SharedPreferences preferences) {
        boolean valido = true;

        if (TextUtils.isEmpty(email)) {
            emailLoginError.setValue("O e-mail não pode estar vazio!");
            valido = false;
        }

        if (TextUtils.isEmpty(senha)) {
            senhaLoginError.setValue("A senha não pode estar vazia!");
            valido = false;
        }

        if (!valido) return;

        UsuarioDto loginDto = new UsuarioDto();
        loginDto.setEmail(email);
        loginDto.setSenha(senha);

        ApiClient.getApiService().realizarLoginApi(loginDto)
                .enqueue(new retrofit2.Callback<UsuarioDto>() {
                    @Override
                    public void onResponse(retrofit2.Call<UsuarioDto> call, retrofit2.Response<UsuarioDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            UsuarioDto usuarioDto = response.body();
                            usuarioLogado.setValue(usuarioDto);

                            if (lembrarCredenciais && preferences != null) {
                                SharedPreferences.Editor dados = preferences.edit();
                                dados.putString("email", email);
                                dados.putString("senha", senha);
                                dados.apply();
                            }
                            mensagemToast.setValue("Login realizado com sucesso!");
                            loginSucesso.setValue(true);
                        } else {
                            mensagemToast.setValue("E-mail ou senha inválidos!");
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<UsuarioDto> call, Throwable t) {
                        mensagemToast.setValue("Falha de conexão com o servidor: " + t.getMessage());
                    }
                });
    }

    public void autenticarComGoogle(String idToken) {
        if (TextUtils.isEmpty(idToken)) {
            mensagemToast.setValue("Falha ao obter credenciais do Google.");
            return;
        }
        mensagemToast.setValue("Autenticado com sucesso via Google!");
        loginSucesso.setValue(true);
    }

    // ============================================================================================
    // CADASTRO COMPLETO (DADOS PESSOAIS + ENDEREÇO)
    // ============================================================================================

    /**
     * Valida os dados pessoais e os campos obrigatórios de endereço (todos exceto complemento e apelido).
     * Envia ao backend via Retrofit e persiste o endereço localmente no SharedPreferences.
     */
    public void executarCadastro(
            String nome,
            String telefone,
            String email,
            String senha,
            String confirmarSenha,
            String cep,
            String logradouro,
            String bairro,
            String cidade,
            String estado,
            String numero,
            String complemento, // Opcional
            String apelido,     // Opcional
            SharedPreferences preferences
    ) {
        // --- 1. VALIDAÇÕES DE DADOS PESSOAIS ---
        if (TextUtils.isEmpty(nome)) {
            nomeRegError.setValue("O nome é obrigatório");
            return;
        }

        if (TextUtils.isEmpty(telefone)) {
            telefoneRegError.setValue("O telefone é obrigatório");
            return;
        }

        String numerosTelefone = telefone.replaceAll("[^\\d]", "");
        if (numerosTelefone.length() < 10) {
            telefoneRegError.setValue("Insira um telefone válido com o DDD completo");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailRegError.setValue("O e-mail é obrigatório");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailRegError.setValue("Insira um e-mail válido");
            return;
        }

        if (TextUtils.isEmpty(senha)) {
            senhaRegError.setValue("A senha é obrigatória");
            return;
        }

        if (senha.length() < 8) {
            senhaRegError.setValue("A senha deve ter pelo menos 8 caracteres");
            return;
        }

        if (TextUtils.isEmpty(confirmarSenha)) {
            confirmarSenhaRegError.setValue("Confirme sua senha");
            return;
        }

        if (!senha.equals(confirmarSenha)) {
            confirmarSenhaRegError.setValue("As senhas não estão iguais");
            return;
        }

        // --- 2. VALIDAÇÕES DOS CAMPOS OBRIGATÓRIOS DO CARD DE ENDEREÇO ---
        String cepLimpo = cep != null ? cep.replaceAll("[^\\d]", "") : "";
        if (TextUtils.isEmpty(cepLimpo) || cepLimpo.length() != 8) {
            cepRegError.setValue("Informe um CEP válido com 8 dígitos");
            return;
        }

        if (TextUtils.isEmpty(logradouro)) {
            logradouroRegError.setValue("O logradouro é obrigatório");
            return;
        }

        if (TextUtils.isEmpty(bairro)) {
            bairroRegError.setValue("O bairro é obrigatório");
            return;
        }

        if (TextUtils.isEmpty(cidade)) {
            cidadeRegError.setValue("A cidade é obrigatória");
            return;
        }

        if (TextUtils.isEmpty(estado)) {
            estadoRegError.setValue("Selecione a UF do estado");
            return;
        }

        if (TextUtils.isEmpty(numero)) {
            numeroRegError.setValue("O número é obrigatório");
            return;
        }

        // --- 3. MONTAGEM DOS OBJETOS E ENVIO ---
        // Monta o objeto de endereço (complemento e apelido podem ser vazios)
        Endereco endereco = new Endereco(
                cepLimpo,
                logradouro,
                bairro,
                cidade,
                estado,
                numero,
                complemento,
                apelido
        );

        // Cria o DTO do usuário para envio ao Spring Boot
        UsuarioDto novoUsuario = new UsuarioDto(nome, email, senha, numerosTelefone, "mock_firebase_uid");
        // Caso seu UsuarioDto já possua campos/objeto de endereço, preencha-os aqui:
        // novoUsuario.setEndereco(endereco);

        ApiClient.getApiService().cadastrarUsuario(novoUsuario)
                .enqueue(new retrofit2.Callback<UsuarioDto>() {
                    @Override
                    public void onResponse(retrofit2.Call<UsuarioDto> call, retrofit2.Response<UsuarioDto> response) {
                        if (response.isSuccessful()) {
                            // Salva o endereço no SharedPreferences após o cadastro bem-sucedido
                            EnderecoPreferencesStorage.salvarEndereco(preferences, endereco);

                            mensagemToast.setValue("Conta criada com sucesso!");
                            cadastroSucesso.setValue(true);
                        } else {
                            String erroServidor = "Erro desconhecido";
                            try {
                                if (response.errorBody() != null) {
                                    erroServidor = response.errorBody().string();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            mensagemToast.setValue("Erro do Servidor (" + response.code() + "): " + erroServidor);
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<UsuarioDto> call, Throwable t) {
                        mensagemToast.setValue("Falha de conexão com o servidor: " + t.getMessage());
                    }
                });
    }

    // ============================================================================================
    // RECUPERAÇÃO DE SENHA E OTP
    // ============================================================================================

    public void enviarEmailRecuperacao(String email) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEsqueceuError.setValue("Digite um e-mail válido para envio");
            return;
        }
        mensagemToast.setValue("Código enviado para o e-mail!");
        envioEmailSucesso.setValue(true);
    }

    public void validarOtp(String codigo) {
        if (TextUtils.isEmpty(codigo) || codigo.length() < 4) {
            otpError.setValue("Digite um código válido");
            return;
        }
        mensagemToast.setValue("Código validado com sucesso!");
        otpSucesso.setValue(true);
    }
}