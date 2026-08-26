package cz.maxtechnik.opm.client.editor;

import cz.maxtechnik.opm.client.ui.UiKit;

public class FileNameInputHandler {
	private final UiKit.TextFieldState state;

	public FileNameInputHandler(String initialFileName) {
		this.state = new UiKit.TextFieldState(initialFileName != null ? initialFileName : "my_recipe");
	}

	public String getFileName() { return state.getText(); }
	public void setFileName(String fileName) { state.setText(fileName); }

	public boolean isFocused() { return state.isFocused(); }
	public void setFocused(boolean focused) { state.setFocused(focused); }
	public int getCursor() { return state.getCursor(); }

	public boolean handleClick(int mx, int my, int fieldX, int fieldY, int fieldW, int fieldH) {
		return state.handleClick(mx, my, fieldX, fieldY, fieldW, fieldH);
	}

	public boolean keyPressed(int key) {
		return state.handleKey(key);
	}

	public boolean charTyped(char chr) {
		if (!state.isFocused()) return false;
		if (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == '/') {
			return state.handleChar(chr);
		}
		return true;
	}
}
