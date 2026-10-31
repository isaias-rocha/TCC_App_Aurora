package com.equipe1.aurora.domain.usecase;

// =========================================================================
// 1. IMPORTAÇÕES DE DOMÍNIO
// =========================================================================
import com.equipe1.aurora.domain.model.Usuario;
import com.equipe1.aurora.domain.repository.AuthRepository;

// =========================================================================
// 2. CASO DE USO: REALIZAR LOGIN
// =========================================================================
// Encapsula a regra de execução do login.
public class RealizarLoginUseCase {

    // =========================================================================
    // 3. ATRIBUTOS E INJEÇÃO DE DEPENDÊNCIA
    // =========================================================================
    private final AuthRepository authRepository;

    public RealizarLoginUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    // =========================================================================
    // 4. EXECUÇÃO DO CASO DE USO
    // =========================================================================
    public void executar(String email, String senha, AuthRepository.AuthCallback<Usuario> callback) {
        authRepository.realizarLogin(email, senha, callback);
    }
}