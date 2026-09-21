package com.thyagotoledo.companions.core.planner;

/** Resultado factual da execução no mundo; IA nunca pode inventar sucesso. */
public final class ActionResult {
    public enum Code { PROGRESS, SUCCEEDED, NO_PATH, FULL_INVENTORY, TOOL_REQUIRED,
        PROTECTED, TARGET_GONE, UNSUPPORTED, CANCELLED, FAILED }

    private final Code code;
    private final int collected;
    private final String detail;

    private ActionResult(Code code, int collected, String detail) {
        this.code = code != null ? code : Code.FAILED;
        this.collected = Math.max(0, collected);
        this.detail = detail != null ? detail : "";
    }

    public static ActionResult progress(int collected, String detail) { return new ActionResult(Code.PROGRESS, collected, detail); }
    public static ActionResult succeeded(int collected, String detail) { return new ActionResult(Code.SUCCEEDED, collected, detail); }
    public static ActionResult failed(Code code, String detail) { return new ActionResult(code, 0, detail); }
    public Code getCode() { return code; }
    public int getCollected() { return collected; }
    public String getDetail() { return detail; }
    public boolean isTerminal() { return code == Code.SUCCEEDED || code == Code.CANCELLED || code == Code.FAILED || code == Code.UNSUPPORTED; }
}
