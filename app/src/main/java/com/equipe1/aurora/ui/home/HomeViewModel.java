package com.equipe1.aurora.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    // Estados observáveis da tela (MutableLiveData privado para encapsulamento)
    private final MutableLiveData<String> nomeUsuario = new MutableLiveData<>();
    private final MutableLiveData<List<CirculoUsuario>> circulosUsuario = new MutableLiveData<>();
    private final MutableLiveData<List<ContatoProximo>> contatosProximos = new MutableLiveData<>();

    // Construtor: Inicializa o carregamento dos dados da Home
    public HomeViewModel() {
        carregarDadosUsuario();
        carregarCirculosUsuario();
        carregarContatosProximos();
    }

    // Getters públicos que expõem apenas LiveData imutável para a View
    public LiveData<String> getObterNomeUsuario() {
        return nomeUsuario;
    }

    public LiveData<List<CirculoUsuario>> getObterCirculosUsuario() {
        return circulosUsuario;
    }

    public LiveData<List<ContatoProximo>> getObterContatosProximos() {
        return contatosProximos;
    }

    // Simulação de busca dos dados do perfil do usuário via UseCase/Repository
    private void carregarDadosUsuario() {
        // Exemplo: BuscarUsuarioUseCase.executar()
        nomeUsuario.setValue("Usuário");
    }

    // Simulação de busca dos círculos de segurança ativos do usuário
    private void carregarCirculosUsuario() {
        // Exemplo: ObterCirculosUseCase.executar()
        List<CirculoUsuario> listaCirculos = new ArrayList<>();
        listaCirculos.add(new CirculoUsuario("Etec", "4 membros ativos"));
        circulosUsuario.setValue(listaCirculos);
    }

    // Simulação de busca de contatos próximos cadastrados
    private void carregarContatosProximos() {
        // Exemplo: ObterContatosProximosUseCase.executar()
        List<ContatoProximo> listaContatos = new ArrayList<>();
        listaContatos.add(new ContatoProximo("Ana Silva", "200 m de distância"));
        contatosProximos.setValue(listaContatos);
    }

    // Modelo interno para representação de Círculos na camada de UI
    public static class CirculoUsuario {
        private final String titulo;
        private final String quantidadeMembros;

        public CirculoUsuario(String titulo, String quantidadeMembros) {
            this.titulo = titulo;
            this.quantidadeMembros = quantidadeMembros;
        }

        public String getTitulo() {
            return titulo;
        }

        public String getQuantidadeMembros() {
            return quantidadeMembros;
        }
    }

    // Modelo interno para representação de Contatos Próximos na camada de UI
    public static class ContatoProximo {
        private final String nome;
        private final String distancia;

        public ContatoProximo(String nome, String distancia) {
            this.nome = nome;
            this.distancia = distancia;
        }

        public String getNome() {
            return nome;
        }

        public String getDistancia() {
            return distancia;
        }
    }
}