package com.thyagotoledo.companions.neoforge.client.gui;

import com.thyagotoledo.companions.core.model.CompanionSnapshot;
import com.thyagotoledo.companions.core.model.CompanionCommandRequest;
import com.thyagotoledo.companions.neoforge.network.NeoForgeCompanionPayloads;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

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
        MINING("Mineracao"),
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

    // Snapshot em tempo real recebido do servidor via CustomPacketPayload
    private static CompanionSnapshot activeSnapshot = null;
    private static UUID activeSnapshotRequestId = null;
    private static long activeSnapshotRevision = -1L;
    private static String pendingStatusFeedback = null;

    // Campos de input
    private EditBox chatBox;
    private EditBox skinBox;
    private EditBox customOreBox;

    // Estado da aba de mineracao
    private static final java.util.Set<String> selectedMiningOres = new java.util.LinkedHashSet<>();
    private static boolean mineAllSelected = true;

    // Estado da integracao Tensura (auto-detecta se o mod tensura estiver instalado)
    private static boolean tensuraIntegrationActive = false;
    private static boolean modelSlimActive = false;

    private String lastStatusMessage = "Pronto para receber ordens.";

    public static void setActiveSnapshot(CompanionSnapshot snapshot) {
        setActiveSnapshot(null, snapshot);
    }

    public static void setActiveSnapshot(UUID requestId, CompanionSnapshot snapshot) {
        if (snapshot == null) return;
        if (activeSnapshot != null && snapshot.getRevision() < activeSnapshotRevision) return;
        activeSnapshot = snapshot;
        activeSnapshotRequestId = requestId;
        activeSnapshotRevision = snapshot.getRevision();
    }

    public static CompanionSnapshot getActiveSnapshot() {
        return activeSnapshot;
    }

    public static UUID getActiveSnapshotRequestId() {
        return activeSnapshotRequestId;
    }

    public static void clearSessionState() {
        activeSnapshot = null;
        activeSnapshotRequestId = null;
        activeSnapshotRevision = -1L;
        pendingStatusFeedback = null;
    }

    public static void setLastStatusFeedback(String speech) {
        if (speech != null && !speech.trim().isEmpty()) {
            pendingStatusFeedback = speech.trim();
        }
    }


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

        if (pendingStatusFeedback != null) {
            this.lastStatusMessage = pendingStatusFeedback;
            pendingStatusFeedback = null;
        }

        // Solicita snapshot atualizado do servidor via CustomPacketPayload
        try {
            PacketDistributor.sendToServer(new NeoForgeCompanionPayloads.RequestSnapshotPayload());
        } catch (Throwable ignored) {
        }

        int panelWidth = 350;
        int panelHeight = 220;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        // 1. Barra Superior de Navegacao por Abas
        int tabW = 76;
        int tabH = 18;
        int tabY = top + 22;

        addRenderableWidget(Button.builder(Component.literal(currentTab == Tab.ACTIONS ? "[1. Acoes]" : "1. Acoes"), b -> switchTab(Tab.ACTIONS))
                .bounds(left + 12, tabY, tabW, tabH)
                .tooltip(Tooltip.create(Component.literal("Ordens e sobrevivencia vanilla")))
                .build());

        addRenderableWidget(Button.builder(Component.literal(currentTab == Tab.MINING ? "[2. Minerar]" : "2. Minerar"), b -> switchTab(Tab.MINING))
                .bounds(left + 92, tabY, tabW, tabH)
                .tooltip(Tooltip.create(Component.literal("Escolha de minerios e painel de mineracao")))
                .build());

        addRenderableWidget(Button.builder(Component.literal(currentTab == Tab.SKINS ? "[3. Skins]" : "3. Skins"), b -> switchTab(Tab.SKINS))
                .bounds(left + 172, tabY, tabW, tabH)
                .tooltip(Tooltip.create(Component.literal("Personalizar skin do companheiro")))
                .build());

        addRenderableWidget(Button.builder(Component.literal(currentTab == Tab.INTEGRATIONS ? "[4. Modpacks]" : "4. Modpacks"), b -> switchTab(Tab.INTEGRATIONS))
                .bounds(left + 252, tabY, tabW, tabH)
                .tooltip(Tooltip.create(Component.literal("Configurar modpacks e integracoes (Tensura)")))
                .build());

        // Botao Fechar (X)
        addRenderableWidget(Button.builder(Component.literal("X"), b -> this.onClose())
                .bounds(left + panelWidth - 20, top + 5, 15, 15)
                .tooltip(Tooltip.create(Component.literal("Fechar painel")))
                .build());

        // 2. Conteudo Especifico da Aba Selecionada
        switch (this.currentTab) {
            case ACTIONS -> initActionsTab(left, top, panelWidth, panelHeight);
            case MINING -> initMiningTab(left, top, panelWidth, panelHeight);
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

        addRenderableWidget(Button.builder(Component.literal("Mochila"), b -> sendOrder("/companion inventory"))
                .bounds(left + 14, top + 128, 48, btnH)
                .tooltip(Tooltip.create(Component.literal("Acessa o inventario do companheiro.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Guardar"), b -> sendOrder("/companion deposit"))
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
        addRenderableWidget(Button.builder(Component.literal("Me Seguir"), b -> sendOrder("/companion mode follow"))
                .bounds(col1, top + 46, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Ordena ao companheiro que te acompanhe.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Ficar Aqui"), b -> sendOrder("/companion mode stay"))
                .bounds(col2, top + 46, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Ordena ao companheiro que aguarde no local.")))
                .build());

        // Linha 2
        addRenderableWidget(Button.builder(Component.literal("Defender"), b -> sendOrder("/companion mode defend"))
                .bounds(col1, top + 70, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Ativa postura de vigia e protecao defensiva.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Puxar para Ca"), b -> sendOrder("/companion recall"))
                .bounds(col2, top + 70, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Chama o companheiro com seguranca para perto.")))
                .build());

        // Linha 3
        addRenderableWidget(Button.builder(Component.literal("Visao Remota"), b -> {
                    if (this.minecraft != null) this.minecraft.setScreen(new RemoteViewScreen(this));
                })
                .bounds(col1, top + 94, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Visualiza pelos olhos do companheiro (camera de vigia).")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Pega Madeira"), b -> sendOrder("/companion action wood"))
                .bounds(col2, top + 94, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Ordena a coleta automatica de madeira florestal.")))
                .build());

        // Linha 4
        addRenderableWidget(Button.builder(Component.literal("Minerar"), b -> {
                    switchTab(Tab.MINING);
                    this.lastStatusMessage = "Escolha os minerios desejados na aba de mineracao.";
                })
                .bounds(col1, top + 118, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Abre a aba de mineracao para escolher minerios e iniciar escavacao.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Plantar"), b -> sendOrder("/companion action farm"))
                .bounds(col2, top + 118, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Ordena a colheita de safras maduras e replantio de sementes.")))
                .build());

        // Linha 5
        addRenderableWidget(Button.builder(Component.literal("Marcar Bau"), b -> sendOrder("/companion chest"))
                .bounds(col1, top + 142, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Registra o bau mais proximo como estoque e deposito do companheiro.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Ajuda / Info"), b -> sendOrder("/companion help"))
                .bounds(col2, top + 142, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Exibe comandos e guia completo no chat.")))
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
    // ABA 2: MINERACAO & ESCOLHA DE MINERIOS
    // ==========================================
    private void initMiningTab(int left, int top, int panelWidth, int panelHeight) {
        int oreBtnW = 76;
        int oreBtnH = 18;

        // Linha 1 de Minerios
        addRenderableWidget(Button.builder(Component.literal(oreButtonLabel("diamante", "Diamante")), b -> toggleOre("diamante"))
                .bounds(left + 14, top + 60, oreBtnW, oreBtnH)
                .tooltip(Tooltip.create(Component.literal("Alternar mineracao de minerios de diamante")))
                .build());

        addRenderableWidget(Button.builder(Component.literal(oreButtonLabel("ferro", "Ferro")), b -> toggleOre("ferro"))
                .bounds(left + 94, top + 60, oreBtnW, oreBtnH)
                .tooltip(Tooltip.create(Component.literal("Alternar mineracao de minerios de ferro")))
                .build());

        addRenderableWidget(Button.builder(Component.literal(oreButtonLabel("carvao", "Carvao")), b -> toggleOre("carvao"))
                .bounds(left + 174, top + 60, oreBtnW, oreBtnH)
                .tooltip(Tooltip.create(Component.literal("Alternar mineracao de carvao")))
                .build());

        addRenderableWidget(Button.builder(Component.literal(oreButtonLabel("ouro", "Ouro")), b -> toggleOre("ouro"))
                .bounds(left + 254, top + 60, oreBtnW, oreBtnH)
                .tooltip(Tooltip.create(Component.literal("Alternar mineracao de ouro")))
                .build());

        // Linha 2 de Minerios
        addRenderableWidget(Button.builder(Component.literal(oreButtonLabel("redstone", "Redstone")), b -> toggleOre("redstone"))
                .bounds(left + 14, top + 82, oreBtnW, oreBtnH)
                .tooltip(Tooltip.create(Component.literal("Alternar mineracao de redstone")))
                .build());

        addRenderableWidget(Button.builder(Component.literal(oreButtonLabel("lapis", "Lapis")), b -> toggleOre("lapis"))
                .bounds(left + 94, top + 82, oreBtnW, oreBtnH)
                .tooltip(Tooltip.create(Component.literal("Alternar mineracao de lapis-lazuli")))
                .build());

        addRenderableWidget(Button.builder(Component.literal(oreButtonLabel("netherite", "Netherite")), b -> toggleOre("netherite"))
                .bounds(left + 174, top + 82, oreBtnW, oreBtnH)
                .tooltip(Tooltip.create(Component.literal("Alternar mineracao de netherite / debris ancestral")))
                .build());

        addRenderableWidget(Button.builder(Component.literal(oreButtonLabel("cobre", "Cobre")), b -> toggleOre("cobre"))
                .bounds(left + 254, top + 82, oreBtnW, oreBtnH)
                .tooltip(Tooltip.create(Component.literal("Alternar mineracao de cobre")))
                .build());

        // Linha 3: Presets de Selecao Rapida
        addRenderableWidget(Button.builder(Component.literal(mineAllSelected ? "[X] Todos" : "[ ] Todos"), b -> selectAllOres())
                .bounds(left + 14, top + 104, 82, 18)
                .tooltip(Tooltip.create(Component.literal("Minerar qualquer minerio por ordem de valor")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("So Diamante"), b -> selectSoloOre("diamante"))
                .bounds(left + 100, top + 104, 80, 18)
                .tooltip(Tooltip.create(Component.literal("Foco exclusivo em minerar apenas diamantes")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("So Ferro"), b -> selectSoloOre("ferro"))
                .bounds(left + 184, top + 104, 76, 18)
                .tooltip(Tooltip.create(Component.literal("Foco exclusivo em minerar apenas ferro")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Limpar"), b -> clearOreSelection())
                .bounds(left + 264, top + 104, 66, 18)
                .tooltip(Tooltip.create(Component.literal("Desmarcar todos os minerios selecionados")))
                .build());

        // Linha 4: Minerio Customizado / Modpack
        this.customOreBox = new EditBox(this.font, left + 14, top + 128, 160, 20, Component.literal("ModOre"));
        this.customOreBox.setMaxLength(32);
        this.customOreBox.setHint(Component.literal("Outro minerio (ex: zinc, tin)..."));
        addRenderableWidget(this.customOreBox);

        addRenderableWidget(Button.builder(Component.literal("+ Adicionar"), b -> addCustomOre())
                .bounds(left + 178, top + 128, 74, 20)
                .tooltip(Tooltip.create(Component.literal("Adiciona o minerio digitado a lista de alvos")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("So Este"), b -> setSoloCustomOre())
                .bounds(left + 256, top + 128, 74, 20)
                .tooltip(Tooltip.create(Component.literal("Foca exclusivamente no minerio digitado")))
                .build());

        // Linha 5: Botoes de Acao Principal
        addRenderableWidget(Button.builder(Component.literal("INICIAR MINERACAO"), b -> startMiningWithSelected())
                .bounds(left + 14, top + 168, 156, 22)
                .tooltip(Tooltip.create(Component.literal("Inicia a mineracao com os minerios selecionados")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Parar"), b -> stopMining())
                .bounds(left + 174, top + 168, 78, 22)
                .tooltip(Tooltip.create(Component.literal("Interrompe a mineracao e retorna para o modo Me Seguir")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Mochila"), b -> sendOrder("/companion inventory"))
                .bounds(left + 256, top + 168, 74, 22)
                .tooltip(Tooltip.create(Component.literal("Acessa o inventario do companheiro")))
                .build());
    }

    private String oreButtonLabel(String key, String displayName) {
        if (mineAllSelected) {
            return "[ ] " + displayName;
        }
        return (selectedMiningOres.contains(key) ? "[X] " : "[ ] ") + displayName;
    }

    private void toggleOre(String key) {
        mineAllSelected = false;
        if (selectedMiningOres.contains(key)) {
            selectedMiningOres.remove(key);
            if (selectedMiningOres.isEmpty()) {
                mineAllSelected = true;
            }
        } else {
            selectedMiningOres.add(key);
        }
        this.clearWidgets();
        this.init();
    }

    private void selectAllOres() {
        selectedMiningOres.clear();
        mineAllSelected = true;
        this.lastStatusMessage = "Modo: minerar todos os minerios por valor.";
        this.clearWidgets();
        this.init();
    }

    private void selectSoloOre(String key) {
        selectedMiningOres.clear();
        selectedMiningOres.add(key);
        mineAllSelected = false;
        this.lastStatusMessage = "Foco exclusivo definido para: " + key + ".";
        this.clearWidgets();
        this.init();
    }

    private void clearOreSelection() {
        selectedMiningOres.clear();
        mineAllSelected = false;
        this.lastStatusMessage = "Selecao limpa. Escolha os minerios desejados.";
        this.clearWidgets();
        this.init();
    }

    private void addCustomOre() {
        if (this.customOreBox != null) {
            String text = this.customOreBox.getValue().trim().toLowerCase(java.util.Locale.ROOT);
            if (!text.isEmpty()) {
                mineAllSelected = false;
                selectedMiningOres.add(text);
                this.lastStatusMessage = "Adicionado minerio alvo: " + text;
                this.customOreBox.setValue("");
                this.clearWidgets();
                this.init();
            }
        }
    }

    private void setSoloCustomOre() {
        if (this.customOreBox != null) {
            String text = this.customOreBox.getValue().trim().toLowerCase(java.util.Locale.ROOT);
            if (!text.isEmpty()) {
                selectedMiningOres.clear();
                selectedMiningOres.add(text);
                mineAllSelected = false;
                this.lastStatusMessage = "Foco exclusivo definido para: " + text + ".";
                this.customOreBox.setValue("");
                this.clearWidgets();
                this.init();
            }
        }
    }

    private void startMiningWithSelected() {
        String filter;
        if (mineAllSelected || selectedMiningOres.isEmpty()) {
            filter = "all";
        } else {
            filter = String.join(",", selectedMiningOres);
        }
        sendOrder("/companion mine " + filter);
        this.lastStatusMessage = "Iniciando mineracao (" + (filter.equals("all") ? "Todos" : filter) + ")...";
    }

    private void stopMining() {
        sendOrder("/companion mode follow");
        this.lastStatusMessage = "Mineracao interrompida. Voltando a te seguir.";
    }

    private String getMiningSelectionSummary() {
        if (mineAllSelected || selectedMiningOres.isEmpty()) {
            return "Todos os minerios (Geral por valor)";
        }
        return String.join(", ", selectedMiningOres);
    }

    // ==========================================
    // ABA 3: APARENCIA & SKINS (JOGADOR E ANIME)
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
            addRenderableWidget(Button.builder(Component.literal("Status de Magiculas"), b -> sendOrder("/companion tensura status"))
                    .bounds(left + 14, top + 130, 150, 20)
                    .tooltip(Tooltip.create(Component.literal("Consulta EP, Rank, Magiculas e Aura no modpack Tensura.")))
                    .build());

            addRenderableWidget(Button.builder(Component.literal("Cerimonia de Nomear"), b -> sendOrder("/companion tensura name"))
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
                if (text.startsWith("/")) {
                    sendOrder(text);
                } else {
                    sendOrder("/companion chat " + text);
                }
                this.chatBox.setValue("");
            }
        }
    }

    private void sendOrder(String text) {
        if (text != null && !text.trim().isEmpty()) {
            this.lastStatusMessage = "Ordem enviada: \"" + text + "\"";
            if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.connection != null) {
                if (sendTypedPayload(text)) {
                    return;
                }
                if (text.startsWith("/")) {
                    this.minecraft.player.connection.sendCommand(text.substring(1));
                } else {
                    this.minecraft.player.connection.sendChat(text);
                }
            }
        }
    }

    private boolean sendTypedPayload(String text) {
        if (activeSnapshot == null || activeSnapshot.getCompanionUuid() == null) return false;
        CompanionCommandRequest request = CompanionCommandRequest.fromText(
                UUID.randomUUID(),
                activeSnapshot.getCompanionUuid(),
                text,
                "pt_br",
                activeSnapshot.getRevision()
        );
        if (!request.isValid()) return false;
        PacketDistributor.sendToServer(new NeoForgeCompanionPayloads.CommandPayload(
                request.getCompanionId(),
                text,
                request.getLocale(),
                request.getRequestId(),
                request.getExpectedRevision()
        ));
        return true;
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
            if (this.customOreBox != null && this.customOreBox.isFocused()) {
                addCustomOre();
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
            case MINING -> renderMiningTab(guiGraphics, left, top);
            case SKINS -> renderSkinsTab(guiGraphics, left, top);
            case INTEGRATIONS -> renderIntegrationsTab(guiGraphics, left, top);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderMiningTab(GuiGraphics guiGraphics, int left, int top) {
        guiGraphics.drawString(this.font, "Painel de Mineracao: Escolha os Minerios Alvo", left + 14, top + 46, 0xFF55FFFF);

        String summary = "Alvo Atual: " + getMiningSelectionSummary();
        guiGraphics.drawString(this.font, summary, left + 14, top + 152, 0xFFFFD700);

        String status = (this.lastStatusMessage != null && !this.lastStatusMessage.isEmpty())
                ? this.lastStatusMessage
                : "Estrategia: Vein Miner 3D, Escadas 1x2, Tuneis Retos e Cavernas.";
        guiGraphics.drawString(this.font, status, left + 14, top + 196, 0xFFFFFF55);
    }

    private void renderActionsTab(GuiGraphics guiGraphics, int left, int top) {
        // Divisoria vertical
        guiGraphics.fill(left + 174, top + 46, left + 175, top + 174, 0xFF33354A);

        CompanionSnapshot s = activeSnapshot != null ? activeSnapshot : CompanionSnapshot.empty(null);

        // Coluna Esquerda: Dados de Sobrevivencia Vanilla Reais
        guiGraphics.drawString(this.font, "Companheiro: " + s.getName(), left + 14, top + 46, 0xFFAAAAAA);
        guiGraphics.drawString(this.font, "Presenca: " + s.getPresenceDisplay(), left + 14, top + 56, 0xFF55FFFF);
        guiGraphics.drawString(this.font, "Modo: " + s.getModeDisplay(), left + 14, top + 68, 0xFF55FF55);
        guiGraphics.drawString(this.font, "Vida: " + s.getHealthDisplay(), left + 14, top + 80, 0xFFFF5555);
        guiGraphics.drawString(this.font, "Bau: " + s.getChestDisplay(), left + 14, top + 92, 0xFFAAAAFF);

        // Mensagem de feedback de ordem ou ultimo status do bot
        String displayMsg = (this.lastStatusMessage != null && !this.lastStatusMessage.isEmpty() && !this.lastStatusMessage.equals("Pronto para receber ordens."))
                ? this.lastStatusMessage
                : s.getLastMessage();
        guiGraphics.drawString(this.font, displayMsg, left + 14, top + 168, 0xFFFFFF55);
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

        CompanionSnapshot s = activeSnapshot != null ? activeSnapshot : CompanionSnapshot.empty(null);
        boolean isTensura = tensuraIntegrationActive || s.isTensuraActive();

        if (isTensura) {
            guiGraphics.drawString(this.font, "Modpack Alvo: Tensura Neo Otherworld", left + 14, top + 86, 0xFF00FF88);
            guiGraphics.drawString(this.font, "Raca: " + s.getTensuraRace() + " | Rank: " + s.getTensuraRank(), left + 14, top + 98, 0xFFFFD700);
            guiGraphics.drawString(this.font, "Valor Existencia (EP): " + String.format(java.util.Locale.ROOT, "%,d", s.getTensuraEp()), left + 14, top + 110, 0xFF55FFFF);
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
