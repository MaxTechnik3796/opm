package cz.maxtechnik.opm.client.screen;

public final class EditorColors {
    private EditorColors() {}

    // ── Barvy ────────────────────────────────────────────────────────────────
    public static final int C_BG       = 0xFF181818;
    public static final int C_BORDER   = 0xFF000000;
    public static final int C_TAB      = 0xFF282828;
    public static final int C_TAB_SEL  = 0xFF4A4A6A;
    public static final int C_TAB_CR   = 0xFF352010;
    public static final int C_TAB_CRS  = 0xFF603810;
    public static final int C_SLOT     = 0xFF3A3A3A;
    public static final int C_SLOT_HOV = 0xFF5A5A5A;
    public static final int C_SLOT_DR  = 0xFF3A5A3A;
    public static final int C_SLOT_RES = 0xFF224422;
    public static final int C_INV      = 0xFF141414;
    public static final int C_TEXT     = 0xFFEEEEEE;
    public static final int C_LABEL    = 0xFFAAAAAA;
    public static final int C_BTN      = 0xFF383838;
    public static final int C_BTN_H    = 0xFF585858;
    public static final int C_BTN_G    = 0xFF1E4A1E;
    public static final int C_BTN_GH   = 0xFF2A6A2A;

    // ── Layout konstanty ─────────────────────────────────────────────────────
    /** Item slot size (px) */
    public static final int SS = 18;
    /** Item slot padding (px) */
    public static final int SP = 2;
    /** Tab bar height (px) */
    public static final int TAB_H = 22;
    /** Inventory columns */
    public static final int INV_COLS = 9;

    // ── Spinner velikosti ────────────────────────────────────────────────────
    /** Standardní spinner tlačítka (count) */
    public static final int SPIN_W = 10, SPIN_H = 8;
    /** Mini spinner (mixing/crushing/fan/pressing výstupy) */
    public static final int MINI_SPIN = 9;

    // ── Standardní in→out řádek ──────────────────────────────────────────────
    /** Vzdálenost mezi inputem a result slotem (kraj→kraj) */
    public static final int IO_GAP = 40;
    /** Vzdálenost od cx pro input slot (vlevo) */
    public static final int IO_INPUT_OFFSET = 60;

    // ── Scrollbar ────────────────────────────────────────────────────────────
    public static final int SB_W       = 4;
    public static final int C_SB_BG    = 0xFF111111;
    public static final int C_SB_THUMB = 0xFF666666;
}