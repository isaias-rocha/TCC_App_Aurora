package com.equipe1.aurora.ui.auth;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * AuthViewModel: O "cérebro" do fluxo de autenticação.
 *
 * Responsabilidades:
 * 1. Processar e validar todos os dados de formulários (Login, Cadastro, Esqueceu Senha, OTP).
 * 2. Gerenciar a autenticação reativa via provedores sociais (Google Sign-In).
 * 3. Manter e expor o estado da UI através de LiveData (mensagens de erro, sinais de sucesso, toasts).
 * 4. Isolar a regra de negócio das Activities (Views), garantindo que a tela apenas "reaja" às mudanças.
 */
public class AuthViewModel extends ViewModel {

    // ============================================================================================
    // 1. ESTADOS INTERNOS (MutableLiveData)
    // Usamos MutableLiveData internamente para podermos alterar (setValue) o valor dos dados.
    // Eles são declarados como 'private' para evitar que as Activities alterem os dados diretamente.
    // ============================================================================================

    // --- Estados para Login (Tradicional e Social) ---
    private final MutableLiveData<String> emailLoginError = new MutableLiveData<>();
    private final MutableLiveData<String> senhaLoginError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loginSucesso = new MutableLiveData<>();

    // --- Estados para Cadastro ---
    private final MutableLiveData<String> nomeRegError = new MutableLiveData<>();
    private final MutableLiveData<String> telefoneRegError = new MutableLiveData<>();
    private final MutableLiveData<String> emailRegError = new MutableLiveData<>();
    private final MutableLiveData<String> senhaRegError = new MutableLiveData<>();
    private final MutableLiveData<String> confirmarSenhaRegError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cadastroSucesso = new MutableLiveData<>();

    // --- Estados para Recuperação de Senha e OTP ---
    private final MutableLiveData<String> emailEsqueceuError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> envioEmailSucesso = new MutableLiveData<>();
    private final MutableLiveData<String> otpError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> otpSucesso = new MutableLiveData<>();

    // --- Estado para Mensagens Gerais ---
    private final MutableLiveData<String> mensagemToast = new MutableLiveData<>();


    // ============================================================================================
    // 2. EXPOSIÇÃO PÚBLICA (LiveData - Apenas Leitura)
    // Convertemos para LiveData simples para que a Activity consiga apenas "observar" os dados,
    // sem a permissão de modificar seus valores. É o princípio do encapsulamento no MVVM.
    // ============================================================================================

    // Getters para a tela de Login
    public LiveData<String> getEmailLoginError() { return emailLoginError; }
    public LiveData<String> getSenhaLoginError() { return senhaLoginError; }
    public LiveData<Boolean> getLoginSucesso() { return loginSucesso; }

    // Getters para a tela de Cadastro
    public LiveData<String> getNomeRegError() { return nomeRegError; }
    public LiveData<String> getTelefoneRegError() { return telefoneRegError; }
    public LiveData<String> getEmailRegError() { return emailRegError; }
    public LiveData<String> getSenhaRegError() { return senhaRegError; }
    public LiveData<String> getConfirmarSenhaRegError() { return confirmarSenhaRegError; }
    public LiveData<Boolean> getCadastroSucesso() { return cadastroSucesso; }

    // Getters para Recuperação de Senha e OTP
    public LiveData<String> getEmailEsqueceuError() { return emailEsqueceuError; }
    public LiveData<Boolean> getEnvioEmailSucesso() { return envioEmailSucesso; }
    public LiveData<String> getOtpError() { return otpError; }
    public LiveData<Boolean> getOtpSucesso() { return otpSucesso; }

    // Getter para mensagens em formato de Toast
    public LiveData<String> getMensagemToast() { return mensagemToast; }


    // ============================================================================================
    // 3. MÉTODOS DE NEGÓCIO E VALIDAÇÕES
    // ============================================================================================

    /**
     * Valida os campos do formulário de login tradicional e realiza a persistência local se solicitado.
     *
     * @param email Email digitado no campo.
     * @param senha Senha digitada no campo.
     * @param lembrarCredenciais Estado da CheckBox "Lembrar-me".
     * @param preferences Instância para salvar dados localmente no dispositivo.
     */
    public void realizarLogin(String email, String senha, boolean lembrarCredenciais, SharedPreferences preferences) {
        boolean valido = true;

        // Validação 1: O campo de e-mail está em branco?
        if (TextUtils.isEmpty(email)) {
            emailLoginError.setValue("O e-mail não pode estar vazio!");
            valido = false;
        }

        // Validação 2: O campo de senha está em branco?
        if (TextUtils.isEmpty(senha)) {
            senhaLoginError.setValue("A senha não pode estar vazia!");
            valido = false;
        }

        // Se alguma validação falhou, interrompe a execução
        if (!valido) return;

        // Regra de Persistência: Salva os dados caso o usuário tenha marcado a opção
        if (lembrarCredenciais && preferences != null) {
            SharedPreferences.Editor dados = preferences.edit();
            dados.putString("email", email);
            dados.putString("senha", senha);
            dados.apply(); // Salva de forma assíncrona
        }

        // Notifica a Activity do sucesso no login
        loginSucesso.setValue(true);
    }

    /**
     * Processa o token de autenticação recebido do SDK do Google Sign-In.
     * Funciona como Login/Cadastro unificado (se o e-mail não existir na base, é cadastrado automaticamente).
     *
     * @param idToken Token de credencial fornecido pelo SDK do Google após o consentimento do usuário.
     */
    public void autenticarComGoogle(String idToken) {
        // Validação: Garante que o Token retornado não seja nulo ou vazio
        if (TextUtils.isEmpty(idToken)) {
            mensagemToast.setValue("Falha ao obter credenciais do Google.");
            return;
        }

        /*
         * Exemplo de integração com Firebase Auth (caso utilize):
         *
         * FirebaseAuth auth = FirebaseAuth.getInstance();
         * AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
         *
         * auth.signInWithCredential(credential)
         *     .addOnCompleteListener(task -> {
         *         if (task.isSuccessful()) {
         *             mensagemToast.setValue("Login social realizado!");
         *             loginSucesso.setValue(true);
         *         } else {
         *             mensagemToast.setValue("Erro ao autenticar com o servidor.");
         *         }
         *     });
         */

        // Simulação de Sucesso Reativo:
        mensagemToast.setValue("Autenticado com sucesso via Google!");
        loginSucesso.setValue(true);
    }

    /**
     * Valida em cascata todos os dados exigidos para a criação manual de conta de usuário.
     * Executa a interrupção no primeiro erro encontrado para focar a atenção do usuário no campo correto.
     */
    public void validarCadastro(String nome, String telefone, String email, String senha, String confirmarSenha) {

        // 1. Validação de Nome
        if (TextUtils.isEmpty(nome)) {
            nomeRegError.setValue("O nome é obrigatório");
            return;
        }

        // 2. Validação de Telefone Vazio
        if (TextUtils.isEmpty(telefone)) {
            telefoneRegError.setValue("O telefone é obrigatório");
            return;
        }

        // Sanitização: Remove caracteres não numéricos
        String numerosTelefone = telefone.replaceAll("[^\\d]", "");

        // 3. Validação de Tamanho do Telefone (DDD + Número)
        if (numerosTelefone.length() < 10) {
            telefoneRegError.setValue("Insira um telefone válido com o DDD completo");
            return;
        }

        // 4. Validação de E-mail Vazio
        if (TextUtils.isEmpty(email)) {
            emailRegError.setValue("O e-mail é obrigatório");
            return;
        }

        // 5. Validação do Formato do E-mail
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailRegError.setValue("Insira um e-mail válido");
            return;
        }

        // 6. Validação de Senha Vazia
        if (TextUtils.isEmpty(senha)) {
            senhaRegError.setValue("A senha é obrigatória");
            return;
        }

        // 7. Validação de Tamanho Mínimo da Senha
        if (senha.length() < 8) {
            senhaRegError.setValue("A senha deve ter pelo menos 8 caracteres");
            return;
        }

        // 8. Validação de Campo Confirmar Senha Vazio
        if (TextUtils.isEmpty(confirmarSenha)) {
            confirmarSenhaRegError.setValue("Confirme sua senha");
            return;
        }

        // 9. Validação de Igualdade das Senhas
        if (!senha.equals(confirmarSenha)) {
            confirmarSenhaRegError.setValue("As senhas não estão iguais");
            return;
        }

        // Sucesso
        mensagemToast.setValue("Conta criada com sucesso!");
        cadastroSucesso.setValue(true);
    }

    /**
     * Valida o e-mail digitado para iniciar o fluxo de recuperação de senha.
     */
    public void enviarEmailRecuperacao(String email) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEsqueceuError.setValue("Digite um e-mail válido para envio");
            return;
        }

        mensagemToast.setValue("Código enviado para o e-mail!");
        envioEmailSucesso.setValue(true);
    }

    /**
     * Valida se o código de verificação numérico (OTP) atende ao tamanho mínimo exigido.
     */
    public void validarOtp(String codigo) {
        if (TextUtils.isEmpty(codigo) || codigo.length() < 4) {
            otpError.setValue("Digite o código completo de 4 dígitos");
            return;
        }

        mensagemToast.setValue("Código verificado com sucesso!");
        otpSucesso.setValue(true);
    }
}