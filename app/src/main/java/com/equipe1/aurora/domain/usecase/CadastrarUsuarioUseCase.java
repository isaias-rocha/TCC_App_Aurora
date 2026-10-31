package com.equipe1.aurora.domain.usecase;

// =========================================================================
// 1. IMPORTAÇÕES DE DOMÍNIO
// =========================================================================
import com.equipe1.aurora.domain.model.Usuario;
import com.equipe1.aurora.domain.repository.AuthRepository;

// =========================================================================
// 2. CASO DE USO: CADASTRAR USUÁRIO
// =========================================================================
// Encapsula a regra de negócio para registro de novos usuários.
public class CadastrarUsuarioUseCase {

    // =========================================================================
    // 3. ATRIBUTOS E INJEÇÃO DE DEPENDÊNCIA
    // =========================================================================
    private final AuthRepository authRepository;

    public CadastrarUsuarioUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    // =========================================================================
    // 4. EXECUÇÃO DO CASO DE USO
    // =========================================================================
    public void executar(Usuario usuario, AuthRepository.AuthCallback<Usuario> callback) {
        authRepository.cadastrarUsuario(usuario, callback);
    }
}