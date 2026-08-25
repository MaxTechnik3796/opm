package cz.maxtechnik.opm.client.editor;

public class FileNameInputHandler {
	private String fileName;
	private boolean focused = false;
	private int cursor;

	public FileNameInputHandler(String initialFileName) {
		this.fileName = initialFileName != null ? initialFileName : "my_recipe";
		this.cursor = this.fileName.length();
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName != null ? fileName : "";
		this.cursor = this.fileName.length();
	}

	public boolean isFocused() {
		return focused;
	}

	public void setFocused(boolean focused) {
		this.focused = focused;
		if (focused) {
			this.cursor = fileName.length();
		}
	}

	public int getCursor() {
		return cursor;
	}

	public boolean handleClick(int mx, int my, int fieldX, int fieldY, int fieldW, int fieldH) {
		if (mx >= fieldX && mx <= fieldX + fieldW && my >= fieldY && my <= fieldY + fieldH) {
			setFocused(true);
			return true;
		}
		setFocused(false);
		return false;
	}

	public boolean keyPressed(int key) {
		if (!focused) return false;

		if (key == 256) { // ESC
			focused = false;
			return true;
		}
		if (key == 259) { // BACKSPACE
			if (!fileName.isEmpty() && cursor > 0) {
				fileName = fileName.substring(0, cursor - 1) + fileName.substring(cursor);
				cursor--;
			}
			return true;
		}
		if (key == 261) { // DELETE
			if (cursor < fileName.length()) {
				fileName = fileName.substring(0, cursor) + fileName.substring(cursor + 1);
			}
			return true;
		}
		if (key == 263) { // LEFT
			cursor = Math.max(0, cursor - 1);
			return true;
		}
		if (key == 262) { // RIGHT
			cursor = Math.min(fileName.length(), cursor + 1);
			return true;
		}

		return true;
	}

	public boolean charTyped(char chr) {
		if (!focused) return false;

		if (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == '/') {
			fileName = fileName.substring(0, cursor) + chr + fileName.substring(cursor);
			cursor++;
		}
		return true;
	}
}
