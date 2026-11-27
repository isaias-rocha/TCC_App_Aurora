package com.equipe1.aurora.domain.usecase;

import android.text.TextUtils;
import com.equipe1.aurora.domain.repository.EnderecoRepository;

// Caso de uso responsável por validar o formato do CEP antes de acionar o repositório
public class ConsultarCepUseCase {

    private final EnderecoRepository repository;

    public ConsultarCepUseCase(EnderecoRepository repository) {
        this.repository = repository;
    }

    public void executar(String cepDigitado, EnderecoRepository.ConsultaCepCallback callback) {
        // Verifica se o CEP está vazio
        if (TextUtils.isEmpty(cepDigitado)) {
            callback.onErro("Digite o CEP para consultar");
            return;
        }

        // Remove qualquer caractere que não seja número (sanitização)
        String cepLimpo = cepDigitado.replaceAll("[^\\d]", "");

        // Valida se o CEP possui exatamente 8 dígitos numéricos
        if (cepLimpo.length() != 8) {
            callback.onErro("O CEP deve conter 8 dígitos");
            return;
        }

        // Dispara a busca no repositório
        repository.consultarCep(cepLimpo, callback);
    }
}