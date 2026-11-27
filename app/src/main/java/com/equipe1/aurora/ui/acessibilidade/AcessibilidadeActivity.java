// =========================================================================
// FUNÇÃO DO ARQUIVO:
// Controla a interface da tela de Acessibilidade utilizando ViewBinding.
// Captura os cliques do usuário (tema claro/escuro, cor principal, modo
// daltonismo, alto contraste, tamanho da fonte, vibração e TalkBack),
// delega o salvamento ao AcessibilidadeViewModel e aplica em tempo real
// as cores, escala de fonte, feedback tátil e anúncios para TalkBack.
// =========================================================================

package com.equipe1.aurora.ui.acessibilidade;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import com.equipe1.aurora.databinding.ActivityAccessibilidadeBinding;
import com.equipe1.aurora.domain.model.ConfiguracaoAcessibilidade;
import com.equipe1.aurora.domain.model.CorPrincipal;
import com.equipe1.aurora.domain.model.TipoDaltonismo;
import com.equipe1.aurora.ui.theme.TemaDinamicoHelper;

public class AcessibilidadeActivity extends AppCompatActivity {

    // -------------------------------------------------------------------------
    // SEÇÃO: DECLARAÇÃO DE ATRIBUTOS (VIEWBINDING E VIEWMODEL)
    // -------------------------------------------------------------------------
    private ActivityAccessibilidadeBinding binding;
    private AcessibilidadeViewModel viewModel;

    // -------------------------------------------------------------------------
    // SEÇÃO: CICLO DE VIDA DA ACTIVITY
    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAccessibilidadeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AcessibilidadeViewModel.class);

        configurarListeners();
        observarViewModel();
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: CONFIGURAÇÃO DE EVENTOS E CLIQUES (LISTENERS)
    // -------------------------------------------------------------------------
    private void configurarListeners() {
        // // Botão Voltar
        binding.btnVoltarAcessibilidade.setOnClickListener(v -> {
            TemaDinamicoHelper.vibrarToque(this);
            finish();
        });

        // // Card 1: Tema Claro / Escuro
        binding.opcaoPreviewClaro.setOnClickListener(v -> {
            TemaDinamicoHelper.vibrarToque(this);
            viewModel.selecionarModoEscuro(false);
            TemaDinamicoHelper.anunciarParaTalkBack(this, v, "Modo Claro selecionado");
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        });

        binding.opcaoPreviewEscuro.setOnClickListener(v -> {
            TemaDinamicoHelper.vibrarToque(this);
            viewModel.selecionarModoEscuro(true);
            TemaDinamicoHelper.anunciarParaTalkBack(this, v, "Modo Escuro selecionado");
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        });

        // // Card 2: Cor Principal (círculos e menu suspenso)
        binding.corAzul.setOnClickListener(v -> selecionarCorComFeedback(v, CorPrincipal.AZUL));
        binding.corVerde.setOnClickListener(v -> selecionarCorComFeedback(v, CorPrincipal.VERDE));
        binding.corAmarelo.setOnClickListener(v -> selecionarCorComFeedback(v, CorPrincipal.AMARELO));
        binding.corVermelho.setOnClickListener(v -> selecionarCorComFeedback(v, CorPrincipal.VERMELHO));
        binding.btnDropdownCor.setOnClickListener(this::abrirMenuCores);

        // // Card 3: Modo Daltonismo
        binding.rowDaltonismo.setOnClickListener(v -> {
            TemaDinamicoHelper.vibrarToque(this);
            boolean novoEstado = !binding.switchDaltonismo.isChecked();
            viewModel.alternarDaltonismo(novoEstado);
            TemaDinamicoHelper.anunciarParaTalkBack(this, v,
                    novoEstado ? "Modo Daltonismo ativado" : "Modo Daltonismo desativado");
        });
        binding.btnTipoDaltonismo.setOnClickListener(this::abrirMenuDaltonismo);

        // // Card 4: Modo de Alto Contraste
        binding.rowAltoContraste.setOnClickListener(v -> {
            TemaDinamicoHelper.vibrarToque(this);
            boolean novoEstado = !binding.switchAltoContraste.isChecked();
            viewModel.alternarAltoContraste(novoEstado);
            TemaDinamicoHelper.anunciarParaTalkBack(this, v,
                    novoEstado ? "Modo de Alto Contraste ativado" : "Modo de Alto Contraste desativado");
        });

        // // Card 5: Tamanho da Fonte (aplica escala em tempo real ao deslizar)
        binding.sliderTamanhoFonte.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                int index = (int) value;
                TemaDinamicoHelper.vibrarToque(this);
                viewModel.selecionarTamanhoFonte(index);
                ConfiguracaoAcessibilidade configAtual = montarConfiguracaoAtual();
                TemaDinamicoHelper.anunciarParaTalkBack(this, slider,
                        "Tamanho da fonte ajustado para " + configAtual.getTamanhoFonteLabel());
            }
        });

        // // Card 6: Vibração
        binding.rowFeedbackTatil.setOnClickListener(v -> {
            boolean novoEstado = !binding.switchFeedbackTatil.isChecked();
            viewModel.alternarVibracao(novoEstado);
            if (novoEstado) {
                TemaDinamicoHelper.vibrarToque(this);
            }
            TemaDinamicoHelper.anunciarParaTalkBack(this, v,
                    novoEstado ? "Vibração ativada" : "Vibração desativada");
        });

        // // Card 7: Suporte a TalkBack
        binding.rowTalkback.setOnClickListener(v -> {
            TemaDinamicoHelper.vibrarToque(this);
            boolean novoEstado = !binding.switchLeitorTela.isChecked();
            viewModel.alternarTalkback(novoEstado);
            if (novoEstado) {
                TemaDinamicoHelper.anunciarParaTalkBack(this, v, "Otimização para TalkBack ativada");
            }
        });
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: OBSERVAÇÃO DE ESTADOS DO VIEWMODEL (LIVEDATA)
    // -------------------------------------------------------------------------
    private void observarViewModel() {
        // // Observa Tema Claro / Escuro
        viewModel.getModoEscuro().observe(this, isEscuro -> {
            int corInt = obterCorEfetivaAtual();
            atualizarVisualCardTema(isEscuro != null && isEscuro, corInt);
        });

        // // Observa Cor Principal
        viewModel.getCorPrincipal().observe(this, cor -> sincronizarTelaCompleta());

        // // Observa Modo Daltonismo e Tipo de Adaptação
        viewModel.getDaltonismoAtivo().observe(this, ativo -> {
            binding.switchDaltonismo.setChecked(ativo);
            binding.btnTipoDaltonismo.setEnabled(ativo);
            binding.rowTipoDaltonismo.setAlpha(ativo ? 1.0f : 0.5f);
            sincronizarTelaCompleta();
        });

        viewModel.getTipoDaltonismo().observe(this, tipo -> {
            binding.btnTipoDaltonismo.setText(tipo.getLabel());
            sincronizarTelaCompleta();
        });

        // // Observa Modo de Alto Contraste
        viewModel.getAltoContraste().observe(this, ativo -> {
            binding.switchAltoContraste.setChecked(ativo);
            sincronizarTelaCompleta();
        });

        // // Observa Tamanho da Fonte e redimensiona todos os textos imediatamente
        viewModel.getTamanhoFonteIndex().observe(this, index -> {
            binding.sliderTamanhoFonte.setValue(index);
            ConfiguracaoAcessibilidade config = montarConfiguracaoAtual();
            binding.tvValorTamanhoFonte.setText(config.getTamanhoFonteLabel());
            sincronizarTelaCompleta();
        });

        // // Observa Vibração e Suporte a TalkBack
        viewModel.getVibracaoAtiva().observe(this, binding.switchFeedbackTatil::setChecked);
        viewModel.getTalkbackOtimizado().observe(this, ativo -> {
            binding.switchLeitorTela.setChecked(ativo);
            sincronizarTelaCompleta();
        });
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: SINCRONIZAÇÃO VISUAL COMPLETA DA TELA ATIVA
    // -------------------------------------------------------------------------
    private void sincronizarTelaCompleta() {
        ConfiguracaoAcessibilidade config = montarConfiguracaoAtual();

        // // 1. Atualiza cores, bordas, escala de fonte e TalkBack em toda a hierarquia
        TemaDinamicoHelper.aplicarConfiguracoesNaTela(this, binding.getRoot(), config);

        // // 2. Atualiza ícones, bolinhas de seleção e previews do card de Tema
        int corInt = obterCorEfetivaAtual();
        int corTextoContraste = TemaDinamicoHelper.resolverAtributoCor(this, com.google.android.material.R.attr.colorOnPrimary);

        atualizarDetalhesEspecificosDaTela(config.getCorPrincipal(), corInt, corTextoContraste);
        atualizarVisualCardTema(config.isModoEscuro(), corInt);
    }

    private ConfiguracaoAcessibilidade montarConfiguracaoAtual() {
        Boolean escuro = viewModel.getModoEscuro().getValue();
        CorPrincipal cor = viewModel.getCorPrincipal().getValue();
        Boolean daltonismo = viewModel.getDaltonismoAtivo().getValue();
        TipoDaltonismo tipo = viewModel.getTipoDaltonismo().getValue();
        Boolean contraste = viewModel.getAltoContraste().getValue();
        Integer fonteIdx = viewModel.getTamanhoFonteIndex().getValue();
        Boolean vibracao = viewModel.getVibracaoAtiva().getValue();
        Boolean talkback = viewModel.getTalkbackOtimizado().getValue();

        return new ConfiguracaoAcessibilidade(
                escuro != null ? escuro : true,
                cor != null ? cor : CorPrincipal.AZUL,
                daltonismo != null ? daltonismo : false,
                tipo != null ? tipo : TipoDaltonismo.NENHUM,
                contraste != null ? contraste : false,
                fonteIdx != null ? fonteIdx : ConfiguracaoAcessibilidade.DEFAULT_FONTE_INDEX,
                vibracao != null ? vibracao : true,
                talkback != null ? talkback : true
        );
    }

    private int obterCorEfetivaAtual() {
        ConfiguracaoAcessibilidade config = montarConfiguracaoAtual();
        if (config.isDaltonismoEfetivo()) {
            return Color.parseColor(config.getHexCorEfetiva());
        }
        return TemaDinamicoHelper.resolverAtributoCor(this, androidx.appcompat.R.attr.colorPrimary);
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: ATUALIZAÇÃO VISUAL DO CARD DE TEMA (BORDA E CHECK CENTRAL)
    // -------------------------------------------------------------------------
    private void atualizarVisualCardTema(boolean isEscuro, int corPrimaria) {
        int strokeAtivoPx = (int) (2 * getResources().getDisplayMetrics().density);
        int strokeInativoPx = (int) (1 * getResources().getDisplayMetrics().density);
        int corTextoInativo = Color.parseColor("#9CA3AF");

        if (isEscuro) {
            binding.cardPreviewEscuro.setStrokeWidth(strokeAtivoPx);
            binding.cardPreviewEscuro.setStrokeColor(corPrimaria);
            binding.badgeCheckEscuro.setVisibility(View.VISIBLE);
            binding.badgeCheckEscuro.setCardBackgroundColor(corPrimaria);
            binding.tvLabelEscuro.setTextColor(corPrimaria);
            binding.tvLabelEscuro.setTypeface(null, Typeface.BOLD);
            binding.ivIconeLabelEscuro.setImageTintList(ColorStateList.valueOf(corPrimaria));

            binding.cardPreviewClaro.setStrokeWidth(strokeInativoPx);
            binding.cardPreviewClaro.setStrokeColor(Color.parseColor("#D1D5DB"));
            binding.badgeCheckClaro.setVisibility(View.GONE);
            binding.tvLabelClaro.setTextColor(corTextoInativo);
            binding.tvLabelClaro.setTypeface(null, Typeface.NORMAL);
            binding.ivIconeLabelClaro.setImageTintList(ColorStateList.valueOf(corTextoInativo));
        } else {
            binding.cardPreviewClaro.setStrokeWidth(strokeAtivoPx);
            binding.cardPreviewClaro.setStrokeColor(corPrimaria);
            binding.badgeCheckClaro.setVisibility(View.VISIBLE);
            binding.badgeCheckClaro.setCardBackgroundColor(corPrimaria);
            binding.tvLabelClaro.setTextColor(corPrimaria);
            binding.tvLabelClaro.setTypeface(null, Typeface.BOLD);
            binding.ivIconeLabelClaro.setImageTintList(ColorStateList.valueOf(corPrimaria));

            binding.cardPreviewEscuro.setStrokeWidth(strokeInativoPx);
            binding.cardPreviewEscuro.setStrokeColor(Color.parseColor("#3A3A44"));
            binding.badgeCheckEscuro.setVisibility(View.GONE);
            binding.tvLabelEscuro.setTextColor(corTextoInativo);
            binding.tvLabelEscuro.setTypeface(null, Typeface.NORMAL);
            binding.ivIconeLabelEscuro.setImageTintList(ColorStateList.valueOf(corTextoInativo));
        }
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: ATUALIZAÇÃO DOS ELEMENTOS ESPECÍFICOS DA TELA (ÍCONES E BOLINHAS)
    // -------------------------------------------------------------------------
    private void atualizarDetalhesEspecificosDaTela(CorPrincipal cor, int corInt, int corTextoContraste) {
        int strokeSelecionado = (int) (2.5f * getResources().getDisplayMetrics().density);

        binding.btnDropdownCor.setText(cor.getNome());

        binding.checkCorAzul.setVisibility(cor == CorPrincipal.AZUL ? View.VISIBLE : View.GONE);
        binding.corAzul.setStrokeWidth(cor == CorPrincipal.AZUL ? strokeSelecionado : 0);

        binding.checkCorVerde.setVisibility(cor == CorPrincipal.VERDE ? View.VISIBLE : View.GONE);
        binding.corVerde.setStrokeWidth(cor == CorPrincipal.VERDE ? strokeSelecionado : 0);

        binding.checkCorAmarelo.setVisibility(cor == CorPrincipal.AMARELO ? View.VISIBLE : View.GONE);
        binding.corAmarelo.setStrokeWidth(cor == CorPrincipal.AMARELO ? strokeSelecionado : 0);

        binding.checkCorVermelho.setVisibility(cor == CorPrincipal.VERMELHO ? View.VISIBLE : View.GONE);
        binding.corVermelho.setStrokeWidth(cor == CorPrincipal.VERMELHO ? strokeSelecionado : 0);

        ColorStateList tintList = ColorStateList.valueOf(corInt);
        binding.tvSecaoVisual.setTextColor(corInt);
        binding.tvSecaoInteracao.setTextColor(corInt);
        binding.ivIconeTema.setImageTintList(tintList);
        binding.ivIconeCor.setImageTintList(tintList);
        binding.ivIconeDaltonismo.setImageTintList(tintList);
        binding.ivIconeAltoContraste.setImageTintList(tintList);
        binding.ivIconeTamanhoFonte.setImageTintList(tintList);
        binding.ivIconeVibracao.setImageTintList(tintList);
        binding.ivIconeTalkback.setImageTintList(tintList);
        binding.ivCheckClaro.setImageTintList(ColorStateList.valueOf(corTextoContraste));
        binding.ivCheckEscuro.setImageTintList(ColorStateList.valueOf(corTextoContraste));
    }

    // -------------------------------------------------------------------------
    // SEÇÃO: AUXILIARES DE FEEDBACK E MENUS SUSPENSOS (POPUP MENUS)
    // -------------------------------------------------------------------------
    private void selecionarCorComFeedback(View anchor, CorPrincipal cor) {
        TemaDinamicoHelper.vibrarToque(this);
        viewModel.selecionarCorPrincipal(cor);
        TemaDinamicoHelper.anunciarParaTalkBack(this, anchor, "Cor principal alterada para " + cor.getNome());
    }

    private void abrirMenuCores(View anchor) {
        TemaDinamicoHelper.vibrarToque(this);
        PopupMenu popup = new PopupMenu(this, anchor);
        CorPrincipal[] cores = CorPrincipal.values();
        for (int i = 0; i < cores.length; i++) {
            popup.getMenu().add(0, i, i, cores[i].getNome());
        }
        popup.setOnMenuItemClickListener(item -> {
            selecionarCorComFeedback(anchor, cores[item.getItemId()]);
            return true;
        });
        popup.show();
    }

    private void abrirMenuDaltonismo(View anchor) {
        TemaDinamicoHelper.vibrarToque(this);
        PopupMenu popup = new PopupMenu(this, anchor);
        TipoDaltonismo[] tipos = TipoDaltonismo.getOpcoesSelecionaveis();
        for (int i = 0; i < tipos.length; i++) {
            popup.getMenu().add(0, i, i, tipos[i].getLabel());
        }
        popup.setOnMenuItemClickListener(item -> {
            TipoDaltonismo selecionado = tipos[item.getItemId()];
            TemaDinamicoHelper.vibrarToque(this);
            viewModel.selecionarTipoDaltonismo(selecionado);
            TemaDinamicoHelper.anunciarParaTalkBack(this, anchor, selecionado.getDescricaoAcessibilidade());
            return true;
        });
        popup.show();
    }
}