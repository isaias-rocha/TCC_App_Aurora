package com.equipe1.aurora.domain.repository;

// =========================================================================
// 1. IMPORTAÇÕES DE DOMÍNIO
// =========================================================================
import com.equipe1.aurora.domain.model.Usuario;

// =========================================================================
// 2. INTERFACE DO REPOSITÓRIO (CONTRATO DE DOMÍNIO)
// =========================================================================
// Define as operações de autenticação sem depender da implementação de rede/banco.
public interface AuthRepository {

    // =========================================================================
    // 3. INTERFACE DE CALLBACK PARA OPERAÇÕES ASSÍNCRONAS
    // =========================================================================
    interface AuthCallback<T> {
        void onSuccess(T result);
        void onError(String mensagemErro);
    }

    // =========================================================================
    // 4. CONTRATOS DOS MÉTODOS DE AUTENTICAÇÃO
    // =========================================================================
    void realizarLogin(String email, String senha, AuthCallback<Usuario> callback);
    void cadastrarUsuario(Usuario usuario, AuthCallback<Usuario> callback);
}