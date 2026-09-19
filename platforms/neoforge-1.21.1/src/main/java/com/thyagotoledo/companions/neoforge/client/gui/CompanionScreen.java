package com.thyagotoledo.companions.neoforge.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Interface grafica principal do mod Companions no Minecraft 1.21.1 / NeoForge.
 * Permite emitir ordens rapidas, consultar o diagnostico do companheiro,
 * visualizar atributos Tensura (EP, Rank, Magiculas) e enviar comandos em linguagem natural.
 */
public class CompanionScreen extends Screen {

    private final Screen parentScreen;
    private EditBox chatBox;
    private String lastStatusMessage = "Pronto para receber ordens.";

    public CompanionScreen(Screen parentScreen) {
        super(Component.literal("Companions - Painel de Controle"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        int panelWidth = 340;
        int panelHeight = 210;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        // Botoes de acoes rapidas (Coluna Direita)
        int btnW = 76;
        int btnH = 20;
        int col1 = left + 172;
        int col2 = left + 252;

        // Linha 1
        addRenderableWidget(Button.builder(Component.literal("Me Seguir"), b -> sendOrder("me segue"))
                .bounds(col1, top + 28, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Ordena ao companheiro que te acompanhe.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Ficar Aqui"), b -> sendOrder("fica aqui"))
                .bounds(col2, top + 28, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Ordena ao companheiro que aguarde no local.")))
                .build());

        // Linha 2
        addRenderableWidget(Button.builder(Component.literal("Defender"), b -> sendOrder("defenda"))
                .bounds(col1, top + 52, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Ativa postura de vigia e protecao defensiva.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Puxar para Ca"), b -> sendOrder("vem ca"))
                .bounds(col2, top + 52, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Chama o companheiro com seguranca para perto.")))
                .build());

        // Linha 3
        addRenderableWidget(Button.builder(Component.literal("Visao Remota"), b -> {
                    sendOrder("visao");
                    this.onClose();
                })
                .bounds(col1, top + 76, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Visualiza pelos olhos do companheiro (camera de vigia).")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Mochila"), b -> sendOrder("inventario"))
                .bounds(col2, top + 76, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Acessa o inventario de 27 slots do companheiro.")))
                .build());

        // Linha 4
        addRenderableWidget(Button.builder(Component.literal("Pega Madeira"), b -> sendOrder("pega madeira"))
                .bounds(col1, top + 100, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Ordena a coleta automatica de madeira respeitando claims.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Status Tensura"), b -> sendOrder("status de magiculas"))
                .bounds(col2, top + 100, btnW, btnH)
                .tooltip(Tooltip.create(Component.literal("Consulta EP, Rank, Magiculas e Aura no modpack Tensura.")))
                .build());

        // Linha 5: Nomear / Evoluir
        addRenderableWidget(Button.builder(Component.literal("Cerimonia de Nomear (Evoluir)"), b -> sendOrder("nomear"))
                .bounds(col1, top + 124, 156, btnH)
                .tooltip(Tooltip.create(Component.literal("Concede nome ao companheiro, multiplicando EP e evoluindo sua raca.")))
                .build());

        // Linha Inferior: Caixa de Chat e Botao Enviar
        this.chatBox = new EditBox(this.font, left + 14, top + 172, 230, 20, Component.literal("Mensagem"));
        this.chatBox.setMaxLength(128);
        this.chatBox.setHint(Component.literal("Digite uma ordem ou pergunta..."));
        addRenderableWidget(this.chatBox);

        addRenderableWidget(Button.builder(Component.literal("Enviar"), b -> submitChatInput())
                .bounds(left + 250, top + 172, 78, 20)
                .tooltip(Tooltip.create(Component.literal("Envia a ordem digitada para o companheiro.")))
                .build());

        // Botao Fechar (X)
        addRenderableWidget(Button.builder(Component.literal("X"), b -> this.onClose())
                .bounds(left + panelWidth - 22, top + 6, 16, 16)
                .tooltip(Tooltip.create(Component.literal("Fechar painel")))
                .build());
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
                this.minecraft.player.connection.sendChat(text);
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
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Fundo escuro semi-transparente cobrindo a tela inteira
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int panelWidth = 340;
        int panelHeight = 210;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        // Caixa principal do painel
        guiGraphics.fill(left, top, left + panelWidth, top + panelHeight, 0xEE12131C);
        guiGraphics.renderOutline(left, top, panelWidth, panelHeight, 0xFF00AAAA);

        // Titulo centralizado
        guiGraphics.drawCenteredString(this.font, "COMPANIONS - PAINEL DE CONTROLE", left + panelWidth / 2, top + 9, 0xFF55FFFF);

        // Divisoria vertical
        guiGraphics.fill(left + 165, top + 26, left + 166, top + 155, 0xFF33354A);

        // Coluna Esquerda: Status do Companheiro
        guiGraphics.drawString(this.font, "Companheiro Vinculado", left + 14, top + 28, 0xFFAAAAAA);
        guiGraphics.drawString(this.font, "Nara (Exploradora)", left + 14, top + 40, 0xFFFFFFFF);

        guiGraphics.drawString(this.font, "Modo Atual: SEGUINDO", left + 14, top + 56, 0xFF55FF55);
        guiGraphics.drawString(this.font, "Vida: 20 / 20", left + 14, top + 68, 0xFFFF5555);

        // Diagnostico Tensura
        guiGraphics.drawString(this.font, "Raca: Kijin (Evoluido)", left + 14, top + 84, 0xFFFFD700);
        guiGraphics.drawString(this.font, "Rank: Special A", left + 14, top + 96, 0xFFFFAA00);
        guiGraphics.drawString(this.font, "Valor Existencia (EP): 25.000", left + 14, top + 108, 0xFF55FFFF);
        guiGraphics.drawString(this.font, "Protecao FTB: Subordinado", left + 14, top + 120, 0xFF00FF88);
        guiGraphics.drawString(this.font, "Imunidade a Magiculas: Ativa", left + 14, top + 132, 0xFF88AAFF);

        // Mensagem de feedback de ordem
        guiGraphics.drawString(this.font, this.lastStatusMessage, left + 14, top + 152, 0xFFFFFF55);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
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
