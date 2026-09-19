package com.thyagotoledo.companions.neoforge.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import org.lwjgl.glfw.GLFW;

/**
 * Interface grafica modular com abas do mod Companions no Minecraft 1.21.1 / NeoForge.
 * Possui 3 abas distintas:
 * - Aba 1: Acoes e Ordens de Sobrevivencia (Vanilla limpo).
 * - Aba 2: Aparencia e Skins (Skins de Jogador e Animes).
 * - Aba 3: Modpacks e Integracoes (Deteccao e controle de Tensura e outros modpacks).
 */
public class CompanionScreen extends Screen {

    public enum Tab {
        ACTIONS("Acoes & Ordens"),
        SKINS("Aparencia & Skins"),
        INTEGRATIONS("Modpacks & Integracoes");

        private final String title;

        Tab(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    private final Screen parentScreen;
    private Tab currentTab = Tab.ACTIONS;

    // Campos de input
    private EditBox chatBox;
    private EditBox skinBox;

    // Estado da integracao Tensura (auto-detecta se o mod tensura estiver instalado)
    private static boolean tensuraIntegrationActive = false;
    private static boolean modelSlimActive = false;

    private String lastStatusMessage = "Pronto para receber ordens.";

    public CompanionScreen(Screen parentScreen) {
        super(Component.literal("Companions - Painel de Controle"));
        this.parentScreen = parentScreen;

        // Auto-deteccao na primeira abertura: se o mod tensura estiver carregado, ativa
        try {
            if (ModList.get() != null && ModList.get().isLoaded("tensura")) {
                tensuraIntegrationActive = true;
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void init() {
        super.init();

        int panelWidth = 350;
        int panelHeight = 220;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        // 1. Barra Superior de Navegacao por Abas
        int tabW = 100;
        int tabH = 18;
        int tabY = top + 22;

        addRenderableWidget(Button.builder(Component.literal("1. Acoes"), b -> switchTab(Tab.ACTIONS))
                .bounds(left + 14, tabY, tabW, tabH)
                .tooltip(Tooltip.create(Component.literal("Ordens e sobrevivencia vanilla")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("2. Skins"), b -> switchTab(Tab.SKINS))
                .bounds(left + 118, tabY, tabW, tabH)
                .tooltip(Tooltip.create(Component.literal("Personalizar skin do companheiro")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("3. Modpacks"), b -> switchTab(Tab.INTEGRATIONS))
                .bounds(left + 222, tabY, tabW, tabH)
                .tooltip(Tooltip.create(Component.literal("Configurar modpacks e integracoes (Tensura)")))
                .build());

        // Botao Fechar (X)
        addRenderableWidget(Button.builder(Component.literal("X"), b -> this.onClose())
                .bounds(left + panelWidth - 22, top + 5, 16, 16)
                .tooltip(Tooltip.create(Component.literal("Fechar painel")))
                .build());

        // 2. Conteudo Especifico da Aba Selecionada
        switch (this.currentTab) {
            case ACTIONS -> initActionsTab(left, top, panelWidth, panelHeight);
            case SKINS -> initSkinsTab(left, top, panelWidth, panelHeight);
            case INTEGRATIONS -> initIntegrationsTab(left, top, panelWidth, panelHeight);
        }
    }

    private void switchTab(Tab targetTab) {
        this.currentTab = targetTab;
        this.clearWidgets();
        this.init();
    }

    // ==========================================
    // ABA 1: ACOES & ORDENS (VANILLA LIMPO)
    // ==========================================
    private void initActionsTab(int left, int top, int panelWidth, int panelHeight) {
        int btnW = 76;
        int btnH = 20;
        int col1 = left + 180;
        int col2 = left + 260;

        // Botoes da Coluna Esquerda de Conveniencia
        addRenderableWidget(Button.builder(Component.literal("Invocar Player"), b -> {
                    sendOrder("/companion spawn");
                    this.lastStatusMessage = "Companheiro entrando como jogador oficial no servidor...";
                })
                .bounds(left + 14, top + 104, 76, btnH)
                .tooltip(Tooltip.create(Component.literal("Invoca o companheiro como jogador oficial (consta no Tab e chat).")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Abrir p/ LAN"), b -> {
                    sendOrder("/companion lan");
                    this.lastStatusMessage = "Abrindo mundo para LAN e invocando companheiro...";
                })
                .bounds(left + 92, top + 104, 78, btnH)
                .tooltip(Tooltip.create(Component.literal("Abre o mundo para LAN e convoca o companheiro para jogar.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Mochila"), b -> sendOrder("inventario"))
                .bounds(left + 14, top + 128, 48, btnH)
                .tooltip(Tooltip.create(Component.literal("Acessa o inventario do companheiro.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Guardar"), b -> sendOrder("deposito"))
                .bounds(left + 64, top + 128, 50, btnH)
                .tooltip(Tooltip.create(Component.literal("Deposita drops e recursos no bau mais proximo.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Dispensar"), b -> {
                    sendOrder("/companion dismiss");
                    this.lastStatusMessage = "Companheiro dispensado do servidor.";
                })
                .bounds(left + 116, top + 128, 54, btnH)
                .tooltip(Tooltip.create(Component.literal("Desconecta o companheiro do servidor com mensagem de saida.")))
                .build());

        // Botoes de Acoes Rapidas (Coluna Direita)
        // Linha 1
        addRenderableWidget(Button.builder(Component.literal("Me Seguir"), b -> sendOrder("me segue"))
                .bounds(col1, top + 46, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Ordena ao companheiro que te acompanhe.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Ficar Aqui"), b -> sendOrder("fica aqui"))
                .bounds(col2, top + 46, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Ordena ao companheiro que aguarde no local.")))
                .build());

        // Linha 2
        addRenderableWidget(Button.builder(Component.literal("Defender"), b -> sendOrder("defenda"))
                .bounds(col1, top + 70, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Ativa postura de vigia e protecao defensiva.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Puxar para Ca"), b -> sendOrder("vem ca"))
                .bounds(col2, top + 70, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Chama o companheiro com seguranca para perto.")))
                .build());

        // Linha 3
        addRenderableWidget(Button.builder(Component.literal("Visao Remota"), b -> {
                    sendOrder("visao");
                    this.onClose();
                })
                .bounds(col1, top + 94, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Visualiza pelos olhos do companheiro (camera de vigia).")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Pega Madeira"), b -> sendOrder("pega madeira"))
                .bounds(col2, top + 94, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Ordena a coleta automatica de madeira florestal.")))
                .build());

        // Rodape: Caixa de Chat e Botao Enviar
        this.chatBox = new EditBox(this.font, left + 14, top + 186, 240, 20, Component.literal("Mensagem"));
        this.chatBox.setMaxLength(128);
        this.chatBox.setHint(Component.literal("Digite uma ordem ou pergunta..."));
        addRenderableWidget(this.chatBox);

        addRenderableWidget(Button.builder(Component.literal("Enviar"), b -> submitChatInput())
                .bounds(left + 258, top + 186, 78, 20)
                .tooltip(Tooltip.create(Component.literal("Envia a ordem digitada para o companheiro.")))
                .build());
    }

    // ==========================================
    // ABA 2: APARENCIA & SKINS (JOGADOR E ANIME)
    // ==========================================
    private void initSkinsTab(int left, int top, int panelWidth, int panelHeight) {
        // Campo de texto para digitar skin
        this.skinBox = new EditBox(this.font, left + 14, top + 72, 170, 20, Component.literal("Skin"));
        this.skinBox.setMaxLength(16);
        this.skinBox.setHint(Component.literal("Ex: Rimuru, Goku, Nick..."));
        addRenderableWidget(this.skinBox);

        // Botao Aplicar Skin
        addRenderableWidget(Button.builder(Component.literal("Aplicar"), b -> applyCustomSkin())
                .bounds(left + 188, top + 72, 68, 20)
                .tooltip(Tooltip.create(Component.literal("Aplica a skin digitada (baixa automaticamente da internet).")))
                .build());

        // Botao Restaurar Minha Skin
        addRenderableWidget(Button.builder(Component.literal("Minha Skin"), b -> resetToOwnerSkin())
                .bounds(left + 260, top + 72, 76, 20)
                .tooltip(Tooltip.create(Component.literal("Restaura a skin para a mesma do seu personagem.")))
                .build());

        // Atalhos de Presets Populares
        int btnPresetW = 60;
        int btnPresetH = 18;
        int presetY = top + 118;

        addRenderableWidget(Button.builder(Component.literal("Rimuru"), b -> quickSetSkin("Rimuru"))
                .bounds(left + 14, presetY, btnPresetW, btnPresetH)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Goku"), b -> quickSetSkin("Goku"))
                .bounds(left + 78, presetY, btnPresetW, btnPresetH)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Luffy"), b -> quickSetSkin("Luffy"))
                .bounds(left + 142, presetY, btnPresetW, btnPresetH)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Naruto"), b -> quickSetSkin("Naruto"))
                .bounds(left + 206, presetY, btnPresetW, btnPresetH)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Kirito"), b -> quickSetSkin("Kirito"))
                .bounds(left + 270, presetY, btnPresetW, btnPresetH)
                .build());

        // Toggle Modelo Slim / Classic
        String modelText = modelSlimActive ? "Modelo Bracos: Slim (3px / Alex)" : "Modelo Bracos: Classic (4px / Steve)";
        addRenderableWidget(Button.builder(Component.literal(modelText), b -> toggleArmModel())
                .bounds(left + 14, top + 154, 210, 20)
                .tooltip(Tooltip.create(Component.literal("Alterna a largura dos bracos entre 3px e 4px.")))
                .build());
    }

    private void quickSetSkin(String name) {
        if (this.skinBox != null) {
            this.skinBox.setValue(name);
        }
        sendOrder("/skin " + name);
        this.lastStatusMessage = "Skin de " + name + " aplicada com sucesso!";
    }

    private void applyCustomSkin() {
        if (this.skinBox != null) {
            String skin = this.skinBox.getValue().trim();
            if (!skin.isEmpty()) {
                sendOrder("/skin " + skin);
                this.lastStatusMessage = "Solicitada a skin: " + skin;
            }
        }
    }

    private void resetToOwnerSkin() {
        sendOrder("/skin reset");
        if (this.skinBox != null) {
            this.skinBox.setValue("");
        }
        this.lastStatusMessage = "Restaurada para a sua propria skin.";
    }

    private void toggleArmModel() {
        modelSlimActive = !modelSlimActive;
        this.lastStatusMessage = modelSlimActive ? "Modelo alterado para Slim (3px)." : "Modelo alterado para Classic (4px).";
        this.clearWidgets();
        this.init();
    }

    // ==========================================
    // ABA 3: MODPACKS & INTEGRACOES (TENSURA)
    // ==========================================
    private void initIntegrationsTab(int left, int top, int panelWidth, int panelHeight) {
        String toggleText = tensuraIntegrationActive
                ? "Integracao Tensura: [ ATIVADA ]"
                : "Integracao Tensura: [ DESATIVADA ]";

        addRenderableWidget(Button.builder(Component.literal(toggleText), b -> toggleTensuraIntegration())
                .bounds(left + 14, top + 48, 220, 20)
                .tooltip(Tooltip.create(Component.literal("Ativa ou desativa os atributos e rituais do mod Tensura Neo Otherworld.")))
                .build());

        if (tensuraIntegrationActive) {
            // Botoes de Acao Exclusivos Tensura
            addRenderableWidget(Button.builder(Component.literal("Status de Magiculas"), b -> sendOrder("status de magiculas"))
                    .bounds(left + 14, top + 130, 150, 20)
                    .tooltip(Tooltip.create(Component.literal("Consulta EP, Rank, Magiculas e Aura no modpack Tensura.")))
                    .build());

            addRenderableWidget(Button.builder(Component.literal("Cerimonia de Nomear"), b -> sendOrder("nomear"))
                    .bounds(left + 172, top + 130, 164, 20)
                    .tooltip(Tooltip.create(Component.literal("Concede nome ao companheiro, multiplicando EP e evoluindo sua raca.")))
                    .build());
        }
    }

    private void toggleTensuraIntegration() {
        tensuraIntegrationActive = !tensuraIntegrationActive;
        this.lastStatusMessage = tensuraIntegrationActive
                ? "Recursos do Tensura Neo Otherworld ativados!"
                : "Modo Vanilla puro reativado (recursos Tensura ocultos).";
        this.clearWidgets();
        this.init();
    }

    private void submitChatInput() {
        if (this.chatBox != null) {
            String text = this.chatBox.getValue().trim();
            if (!text.isEmpty()) {
                sendOrder(text);
                this.chatBox.setValue("");
            }
        }
    }

    private void sendOrder(String text) {
        if (text != null && !text.trim().isEmpty()) {
            this.lastStatusMessage = "Ordem enviada: \"" + text + "\"";
            if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.connection != null) {
                if (text.startsWith("/")) {
                    this.minecraft.player.connection.sendCommand(text.substring(1));
                } else {
                    this.minecraft.player.connection.sendChat(text);
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.chatBox != null && this.chatBox.isFocused()) {
                submitChatInput();
                return true;
            }
            if (this.skinBox != null && this.skinBox.isFocused()) {
                applyCustomSkin();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int panelWidth = 350;
        int panelHeight = 220;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        // Fundo principal do painel
        guiGraphics.fill(left, top, left + panelWidth, top + panelHeight, 0xEE12131C);
        guiGraphics.renderOutline(left, top, panelWidth, panelHeight, 0xFF00AAAA);

        // Titulo centralizado
        guiGraphics.drawCenteredString(this.font, "COMPANIONS - PAINEL DE CONTROLE", left + panelWidth / 2, top + 8, 0xFF55FFFF);

        // Linha divisoria abaixo das abas
        guiGraphics.fill(left + 10, top + 42, left + panelWidth - 10, top + 43, 0xFF33354A);

        // Renderizacao do conteudo especifico de cada aba
        switch (this.currentTab) {
            case ACTIONS -> renderActionsTab(guiGraphics, left, top);
            case SKINS -> renderSkinsTab(guiGraphics, left, top);
            case INTEGRATIONS -> renderIntegrationsTab(guiGraphics, left, top);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderActionsTab(GuiGraphics guiGraphics, int left, int top) {
        // Divisoria vertical
        guiGraphics.fill(left + 174, top + 46, left + 175, top + 174, 0xFF33354A);

        // Coluna Esquerda: Dados de Sobrevivencia Vanilla
        guiGraphics.drawString(this.font, "Companheiro:", left + 14, top + 46, 0xFFAAAAAA);
        guiGraphics.drawString(this.font, "Presenca: Jogador Real", left + 14, top + 56, 0xFF55FFFF);
        guiGraphics.drawString(this.font, "Modo: SEGUINDO", left + 14, top + 68, 0xFF55FF55);
        guiGraphics.drawString(this.font, "Vida: 20 / 20", left + 14, top + 80, 0xFFFF5555);

        // Mensagem de feedback de ordem
        guiGraphics.drawString(this.font, this.lastStatusMessage, left + 14, top + 168, 0xFFFFFF55);
    }

    private void renderSkinsTab(GuiGraphics guiGraphics, int left, int top) {
        guiGraphics.drawString(this.font, "Personalizacao de Skin & Aparencia", left + 14, top + 48, 0xFF55FFFF);
        guiGraphics.drawString(this.font, "Digite o nickname do jogador ou personagem de anime:", left + 14, top + 60, 0xFFAAAAAA);

        guiGraphics.drawString(this.font, "Presets de Anime Populares:", left + 14, top + 104, 0xFFAAAAAA);

        // Feedback / Status
        guiGraphics.drawString(this.font, this.lastStatusMessage, left + 14, top + 186, 0xFFFFFF55);
    }

    private void renderIntegrationsTab(GuiGraphics guiGraphics, int left, int top) {
        guiGraphics.drawString(this.font, "Modpacks Especiais & Integracoes", left + 14, top + 74, 0xFF55FFFF);

        if (tensuraIntegrationActive) {
            guiGraphics.drawString(this.font, "Modpack Alvo: Tensura Neo Otherworld", left + 14, top + 86, 0xFF00FF88);
            guiGraphics.drawString(this.font, "Raca: Kijin (Evoluido) | Rank: Special A", left + 14, top + 98, 0xFFFFD700);
            guiGraphics.drawString(this.font, "Valor Existencia (EP): 25.000 | Magiculas: Altas", left + 14, top + 110, 0xFF55FFFF);
        } else {
            guiGraphics.drawString(this.font, "Ambiente Vanilla Ativo.", left + 14, top + 90, 0xFFFFFFFF);
            guiGraphics.drawString(this.font, "Nenhuma integracao de fantasia ativada no momento.", left + 14, top + 104, 0xFFAAAAAA);
            guiGraphics.drawString(this.font, "A interface e ordens permanecem 100% puras e limpas.", left + 14, top + 116, 0xFFAAAAAA);
        }

        guiGraphics.drawString(this.font, this.lastStatusMessage, left + 14, top + 186, 0xFFFFFF55);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.parentScreen != null && this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }
}
