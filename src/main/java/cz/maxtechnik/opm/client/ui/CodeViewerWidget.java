package cz.maxtechnik.opm.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class CodeViewerWidget {
	// Konstanty rozměrů & výběru
	private static final int SEL = 0x553399FF;
	private static final int LH = 10, TOOLBAR_H = 22, SEARCH_H = 16, ARROW_W = 12;

	// Syntax barvy
	private static final int SYN_STRING = 0xFFCE9178, SYN_NUM = 0xFFB5CEA8;
	private static final int SYN_BOOL = 0xFF569CD6, SYN_KEY = 0xFF9CDCFE;
	private static final int SYN_TYPE = 0xFF4EC9B0, SYN_CONST = 0xFF4FC1FF;
	private static final int SYN_BRACE = 0xFFFFD700, SYN_BRACKET = 0xFFDA70D6, SYN_PUNCT = 0xFF808080;

	public record LineEntry(String text, int lineNum) {}

	// Custom tlačítko v toolbaru
	public record ToolbarButton(String label, int width, BiConsumer<Integer, Integer> onClick) {}

	private record ButtonState(ToolbarButton btn, int x, int y, boolean hover) {}

	private final Font font;
	private final String rawText;
	private List<LineEntry> lines;
	private final List<ToolbarButton> extraButtons = new ArrayList<>();
	private final List<ButtonState> buttonStates = new ArrayList<>();

	// Geometrie
	private int x, y, w, h;
	private int toolbarY, boxX, boxY, boxW, boxH, lineNumW;
	private int sX, sY, sW;
	private int copyBtnX, copyBtnY;
	private static final int COPY_W = 40;

	// Stav
	private final Scrollbar scrollbar = new Scrollbar();
	private int scrollOffset;
	private final java.util.Set<Integer> selectedLines = new java.util.TreeSet<>();
	private int anchorLine = -1;
	private boolean searchFocused;
	private String searchQuery = "";
	private final List<Integer> searchHits = new ArrayList<>();
	private int searchIdx, searchCursor;
	private boolean hCopy, hPrev, hNext;
	private long lastClickTime;
	private int lastClickLine = -1;

	// Copy feedback
	private String feedback;
	private long feedbackUntil;
	private int feedbackX, feedbackY;

	public CodeViewerWidget(Font font, String rawText) {
		this.font = font;
		this.rawText = rawText;
	}

	// Přidá custom tlačítko do toolbaru
	public void addButton(String label, int width, BiConsumer<Integer, Integer> onClick) {
		extraButtons.add(new ToolbarButton(label, width, onClick));
	}

	// Nastaví pozici a rozměry celého widgetu
	public void setBounds(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		recalc();
		int totalLines = rawText.split("\n", -1).length;
		int maxNum = Math.max(99, totalLines);
		lineNumW = font.width(String.valueOf(maxNum)) + 12;
		lines = buildLines(rawText);
	}

	private void recalc() {
		toolbarY = y;
		int tcy = toolbarY + (TOOLBAR_H - 16) / 2;
		copyBtnX = x + 4;
		copyBtnY = tcy;

		buttonStates.clear();
		int btnX = copyBtnX + COPY_W + 4;
		for (ToolbarButton btn : extraButtons) {
			buttonStates.add(new ButtonState(btn, btnX, tcy, false));
			btnX += btn.width() + 4;
		}
		sX = btnX + 2;
		sW = Math.max(20, x + w - sX - 36 - ARROW_W * 2 - 12);
		sY = tcy;
		boxX = x + 3;
		boxY = toolbarY + TOOLBAR_H + 2;
		boxW = w - 6;
		boxH = y + h - boxY - 3;
	}

	// RENDER ────────────────────────────────────────────────────────
	public void render(GuiGraphics g, int mx, int my) {
		if (lines == null) return;
		g.fill(x, toolbarY, x + w, toolbarY + TOOLBAR_H, UiKit.C_HEADER);
		g.fill(x, toolbarY + TOOLBAR_H, x + w, toolbarY + TOOLBAR_H + 1, UiKit.C_BORDER);
		hCopy = drawBtn(g, "Copy", copyBtnX, copyBtnY, COPY_W, mx, my);
		for (int i = 0; i < buttonStates.size(); i++) {
			ButtonState bs = buttonStates.get(i);
			boolean hover = drawBtn(g, bs.btn().label(), bs.x(), bs.y(), bs.btn().width(), mx, my);
			buttonStates.set(i, new ButtonState(bs.btn(), bs.x(), bs.y(), hover));
		}

		renderSearch(g, mx, my);

		g.fill(boxX, boxY, boxX + boxW, boxY + boxH, UiKit.C_BG);
		renderCode(g);

		if (feedback != null && System.currentTimeMillis() < feedbackUntil)
			g.drawString(font, feedback, feedbackX, feedbackY, 0xFF55FF55, true);
		else feedback = null;
	}

	private boolean drawBtn(GuiGraphics g, String label, int bx, int by, int bw, int mx, int my) {
		boolean hover = hit(mx, my, bx, by, bw, 16);
		UiKit.drawGhostButton(g, font, label, bx, by, bw, 16, hover, 0xFF353535, 0xFFFFFFFF);
		return hover;
	}

	private void renderSearch(GuiGraphics g, int mx, int my) {
		int rightLimit = x + w - 4;
		int availableW = Math.max(20, rightLimit - sX);
		int arrowSpace = searchHits.size() > 1 ? (36 + ARROW_W * 2 + 4) : (!searchHits.isEmpty() ? 36 : 0);
		sW = Math.max(20, availableW - arrowSpace);

		UiKit.drawInputField(g, font, searchQuery, "Search...", searchCursor, searchFocused, sX, sY, sW, SEARCH_H);

		int countX = sX + sW + 2, prevX = countX + 36, nextX = prevX + ARROW_W + 2;
		if (!searchHits.isEmpty() && countX + 30 <= rightLimit) {
			g.drawString(font, (searchIdx + 1) + "/" + searchHits.size(), countX, sY + 4, UiKit.C_LABEL, false);
		}
		if (searchHits.size() > 1 && nextX + ARROW_W <= rightLimit) {
			hPrev = hit(mx, my, prevX, sY, ARROW_W, SEARCH_H);
			hNext = hit(mx, my, nextX, sY, ARROW_W, SEARCH_H);
			UiKit.drawGhostButton(g, font, "<", prevX, sY, ARROW_W, SEARCH_H, hPrev, 0xFF353535, 0xFFFFFFFF);
			UiKit.drawGhostButton(g, font, ">", nextX, sY, ARROW_W, SEARCH_H, hNext, 0xFF353535, 0xFFFFFFFF);
		}
	}

	private void renderCode(GuiGraphics g) {
		int vis = boxH / LH;
		int contentH = (lines != null ? lines.size() : 0) * LH;
		scrollbar.update(boxH, contentH);
		scrollOffset = (int) (scrollbar.scroll / LH);
		int hl = searchHits.isEmpty() ? -1 : searchHits.get(Math.min(searchIdx, searchHits.size() - 1));
		g.enableScissor(boxX + 2, boxY + 2, boxX + boxW - 4, boxY + boxH - 2);
		g.fill(boxX + lineNumW, boxY, boxX + lineNumW + 1, boxY + boxH, 0xFF282828);
		int ly = boxY + 3 - (int) (scrollbar.scroll % LH);
		for (int i = scrollOffset; i < Math.min(scrollOffset + vis + 2, lines.size()); i++) {
			LineEntry e = lines.get(i);
			if (selectedLines.contains(i)) g.fill(boxX + 2, ly - 1, boxX + boxW - 4, ly + LH - 1, SEL);
			if (i == hl) g.fill(boxX + 2, ly - 1, boxX + boxW - 4, ly + LH - 1, 0x553A3A1A);
			if (e.lineNum() >= 0) {
				String ns = String.valueOf(e.lineNum());
				g.drawString(font, ns, boxX + lineNumW - 4 - font.width(ns), ly, UiKit.C_MUTED, false);
			}
			drawSyntaxLine(g, e.text(), boxX + lineNumW + 4, ly, searchQuery);
			ly += LH;
		}
		g.disableScissor();
		if (contentH > boxH) {
			scrollbar.render(g, boxX + boxW - 4, boxY);
		}
	}

	// INPUT ────────────────────────────────────────────────────────
	public boolean mouseClicked(int mx, int my, int button) {
		if (button != 0) return false;
		if (!hit(mx, my, x, y, w, h)) return false;
		if (hCopy) {
			copySelection(mx, my);
			return true;
		}
		for (ButtonState bs : buttonStates) {
			if (bs.hover()) {
				bs.btn().onClick().accept(mx, my);
				return true;
			}
		}
		if (hPrev && searchHits.size() > 1) {
			scrollToMatch(searchIdx - 1);
			return true;
		}
		if (hNext && searchHits.size() > 1) {
			scrollToMatch(searchIdx + 1);
			return true;
		}
		if (hit(mx, my, sX, sY, sW, SEARCH_H)) {
			searchFocused = true;
			searchCursor = searchQuery.length();
			return true;
		}
		searchFocused = false;

		if (scrollbar.startDragIfHit(mx, my)) return true;

		int li = lineAt(my);
		if (li >= 0) {
			long now = System.currentTimeMillis();
			if (li == lastClickLine && now - lastClickTime < 400) {
				selectedLines.remove(li);
				lastClickLine = -1;
			} else if (hasShift()) {
				int start = anchorLine >= 0 ? anchorLine : li;
				int min = Math.min(start, li), max = Math.max(start, li);
				selectedLines.clear();
				for (int i = min; i <= max; i++) selectedLines.add(i);
			} else if (hasCtrl()) {
				if (selectedLines.contains(li)) selectedLines.remove(li);
				else selectedLines.add(li);
				anchorLine = li;
			} else {
				selectedLines.clear();
				selectedLines.add(li);
				anchorLine = li;
			}
			lastClickTime = now;
			lastClickLine = li;
			return true;
		} else if (!hit(mx, my, boxX, boxY, boxW, boxH)) {
			selectedLines.clear();
			anchorLine = -1;
		}
		return false;
	}

	public boolean mouseDragged(int my) {
		return scrollbar.mouseDragged(my);
	}

	public void mouseReleased() {
		scrollbar.stopDrag();
	}

	public boolean mouseScrolled(double sy, int mx, int my) {
		if (!hit(mx, my, x, y, w, h)) return false;
		scrollbar.handleScroll(sy, LH * 2);
		return true;
	}

	public boolean keyPressed(int key, int mods) {
		if (key == 67 && (mods & 2) != 0) {
			Minecraft mc = Minecraft.getInstance();
			copySelection(mc.getWindow().getGuiScaledWidth() / 2, mc.getWindow().getGuiScaledHeight() / 2);
			return true;
		}
		if (searchFocused) {
			if (key == 259 && !searchQuery.isEmpty() && searchCursor > 0) {
				searchQuery = searchQuery.substring(0, searchCursor - 1) + searchQuery.substring(searchCursor);
				searchCursor--;
				updateSearch();
			} else if (key == 261 && searchCursor < searchQuery.length()) {
				searchQuery = searchQuery.substring(0, searchCursor) + searchQuery.substring(searchCursor + 1);
				updateSearch();
			} else if (key == 263) searchCursor = Math.max(0, searchCursor - 1);
			else if (key == 262) searchCursor = Math.min(searchQuery.length(), searchCursor + 1);
			else if (key == 256) searchFocused = false;
			else if ((key == 257 || key == 335) && !searchHits.isEmpty()) scrollToMatch(searchIdx + 1);
			return true;
		}
		return false;
	}

	public boolean charTyped(char chr) {
		if (searchFocused) {
			searchQuery = searchQuery.substring(0, searchCursor) + chr + searchQuery.substring(searchCursor);
			searchCursor++;
			updateSearch();
			return true;
		}
		return false;
	}

	// DATA ────────────────────────────────────────────────────────
	private List<LineEntry> buildLines(String text) {
		List<LineEntry> result = new ArrayList<>();
		int maxW = boxW - 16 - lineNumW;
		int num = 1;
		for (String line : text.split("\n", -1)) {
			if (font.width(line) <= maxW) {
				result.add(new LineEntry(line, num++));
				continue;
			}
			int ic = 0;
			while (ic < line.length() && line.charAt(ic) == ' ') ic++;
			String pad = "  " + " ".repeat(ic);
			String rem = line;
			boolean first = true;
			while (!rem.isEmpty()) {
				int lmw = first ? maxW : maxW - font.width(pad);
				String fit = font.plainSubstrByWidth(rem, lmw);
				int chars = fit.length();
				if (chars < rem.length()) {
					for (int j = chars - 1; j > 0; j--)
						if (" ,:{}[]".indexOf(fit.charAt(j)) >= 0) {
							chars = j + 1;
							break;
						}
				}
				if (chars == 0) chars = 1;
				result.add(new LineEntry(first ? rem.substring(0, chars) : pad + rem.substring(0, chars), first ? num++ : -1));
				rem = rem.substring(chars);
				first = false;
			}
		}
		return result;
	}

	// SYNTAX ────────────────────────────────────────────────────────
	private void drawSyntaxLine(GuiGraphics g, String line, int lx, int ly, String query) {
		int cx = lx;
		boolean inStr = false;
		char strCh = 0;
		StringBuilder tok = new StringBuilder();
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (inStr) {
				tok.append(c);
				if (c == strCh && line.charAt(i - 1) != '\\') {
					inStr = false;
					cx = drawTok(g, tok, cx, ly, SYN_STRING);
				}
			} else if (c == '"' || c == '\'') {
				cx = flushTok(g, tok, cx, ly);
				inStr = true;
				strCh = c;
				tok.append(c);
			} else if (":{}[],=".indexOf(c) >= 0) {
				cx = flushTok(g, tok, cx, ly);
				int sc = (c == '{' || c == '}') ? SYN_BRACE : (c == '[' || c == ']') ? SYN_BRACKET : SYN_PUNCT;
				g.drawString(font, String.valueOf(c), cx, ly, sc, false);
				cx += font.width(String.valueOf(c));
			} else tok.append(c);
		}
		if (!tok.isEmpty()) drawTok(g, tok, cx, ly, inStr ? SYN_STRING : tokenColor(tok.toString().trim()));
		if (!query.isBlank()) {
			int idx = line.toLowerCase().indexOf(query.toLowerCase());
			if (idx >= 0) {
				int px = font.width(line.substring(0, idx));
				int pw = font.width(line.substring(idx, idx + query.length()));
				g.fill(lx + px - 1, ly - 1, lx + px + pw + 1, ly + LH - 1, 0x55FFFF00);
			}
		}
	}

	private int flushTok(GuiGraphics g, StringBuilder tok, int tx, int ty) {
		if (tok.isEmpty()) return tx;
		return drawTok(g, tok, tx, ty, tokenColor(tok.toString().trim()));
	}

	private int drawTok(GuiGraphics g, StringBuilder tok, int tx, int ty, int color) {
		String s = tok.toString();
		g.drawString(font, s, tx, ty, color, false);
		tok.setLength(0);
		return tx + font.width(s);
	}

	static int tokenColor(String t) {
		if (t.isEmpty()) return 0xFFD4D4D4;
		if (t.matches("-?\\d+(\\.\\d+)?[bBsSlLfFdD]?") || t.matches("-?\\.\\d+[fFdD]?") || t.matches("0[xX][0-9a-fA-F]+") || t.matches("[IBLS];"))
			return SYN_NUM;
		if (t.equals("true") || t.equals("false")) return SYN_BOOL;
		if (t.matches("[A-Z][A-Z0-9_]+")) return SYN_CONST;
		if (t.length() > 1 && Character.isUpperCase(t.charAt(0)) && t.chars().anyMatch(Character::isLowerCase))
			return SYN_TYPE;
		return SYN_KEY;
	}

	// HELPERS ────────────────────────────────────────────────────────
	private void updateSearch() {
		searchHits.clear();
		searchIdx = 0;
		if (searchQuery.isBlank()) return;
		String q = searchQuery.toLowerCase();
		for (int i = 0; i < lines.size(); i++) if (lines.get(i).text().toLowerCase().contains(q)) searchHits.add(i);
		if (!searchHits.isEmpty()) scrollToMatch(0);
	}

	private void scrollToMatch(int idx) {
		if (searchHits.isEmpty()) return;
		searchIdx = Math.clamp(idx, 0, searchHits.size() - 1);
		int targetLine = searchHits.get(searchIdx);
		scrollbar.scroll = Math.max(0, targetLine * LH - boxH / 2);
	}

	public void clip(String text, int mx, int my) {
		UiKit.copyToClipboard(text);
		feedback = "Copied!";
		feedbackX = mx + 8;
		feedbackY = my - 10;
		feedbackUntil = System.currentTimeMillis() + 1200;
	}

	private void copySelection(int mx, int my) {
		StringBuilder sb = new StringBuilder();
		if (selectedLines.isEmpty()) {
			sb.append(rawText);
		} else {
			boolean first = true;
			for (int i : selectedLines) {
				if (i >= 0 && i < lines.size()) {
					if (!first) sb.append("\n");
					sb.append(lines.get(i).text());
					first = false;
				}
			}
		}
		clip(sb.toString(), mx, my);
	}

	private int lineAt(int my) {
		if (my < boxY || my >= boxY + boxH) return -1;
		int li = scrollOffset + (my - boxY - 3) / LH;
		return (li >= 0 && li < lines.size()) ? li : -1;
	}

	private boolean hit(int mx, int my, int hx, int hy, int hw, int hh) {
		return mx >= hx && mx <= hx + hw && my >= hy && my <= hy + hh;
	}

	private static boolean hasShift() {
		return net.minecraft.client.gui.screens.Screen.hasShiftDown();
	}

	private static boolean hasCtrl() {
		return net.minecraft.client.gui.screens.Screen.hasControlDown();
	}
}