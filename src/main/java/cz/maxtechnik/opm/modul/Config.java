package cz.maxtechnik.opm.modul;

import cz.maxtechnik.opm.OpmModKeys;
import cz.maxtechnik.opm.config.AnchorX;
import cz.maxtechnik.opm.config.AnchorY;
import cz.maxtechnik.opm.util.MovableWidget;
import cz.maxtechnik.opm.util.OpmButton;
import cz.maxtechnik.opm.util.OpmColors;
import cz.maxtechnik.opm.util.SidelistWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Config extends Screen {

	public static class ConfigWidgetHolder {
		public final MovableWidget widget;
		public SidelistWidget.ScoreboardBounds bounds;
		public OpmButton button;

		public ConfigWidgetHolder(MovableWidget widget) {
			this.widget = widget;
		}

		public void update(Font font, int screenWidth, int screenHeight) {
			this.bounds = widget.getBounds(font, screenWidth, screenHeight);
			if (button == null) {
				button = new OpmButton(font, Component.empty(), bounds.x(), bounds.y(), bounds.width(), bounds.height(), b -> {});
				button.setBackGroundColors(OpmColors.TRANSPARENT, OpmColors.BLUE, OpmColors.TRANSPARENT_WHITE);
			} else {
				button.setX(bounds.x());
				button.setY(bounds.y());
				button.setWidth(bounds.width());
				button.setHeight(bounds.height());
			}
		}
	}

	private final List<ConfigWidgetHolder> widgets = new ArrayList<>();
	private ConfigWidgetHolder focusedWidget = null;
	private int grabOffsetX = 0;
	private int grabOffsetY = 0;

	public Config() {
		super(Component.translatable("screen.opm.config"));
	}

	@Override
	public void init() {
		super.init();
		widgets.clear();

		// Sem stačí v budoucnu jednoduše připsat jakýkoliv nový widget:
		registerWidget(new SidelistWidget());
		// registerWidget(new ArmorHudWidget());
		// registerWidget(new CoordsHudWidget());

		for (ConfigWidgetHolder holder : widgets) {
			holder.update(font, width, height);
			addRenderableWidget(holder.button);
		}
	}

	private void registerWidget(MovableWidget widget) {
		widgets.add(new ConfigWidgetHolder(widget));
	}

	@Override
	public void renderBackground(@NotNull GuiGraphics gui,int mouseX,int mouseY,float partialTick) {
		// Prázdné tělo: zabrání ztmavení i rozostření (bluru) pozadí
	}

	@Override
	public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
		// Plynulý posun aktivně taženého widgetu
		if (focusedWidget != null) {
			int boxX = mouseX - grabOffsetX;
			int boxY = mouseY - grabOffsetY;

			int anchorOffsetX = switch (focusedWidget.widget.getAnchorX()) {
				case RIGHT -> focusedWidget.bounds.width();
				case MIDDLE -> focusedWidget.bounds.width() / 2;
				default -> 0;
			};
			int anchorOffsetY = switch (focusedWidget.widget.getAnchorY()) {
				case BOTTOM -> focusedWidget.bounds.height();
				case MIDDLE -> focusedWidget.bounds.height() / 2;
				default -> 0;
			};

			focusedWidget.widget.setX(boxX + anchorOffsetX);
			focusedWidget.widget.setY(boxY + anchorOffsetY);
			focusedWidget.update(font, width, height);
		}

		// Kotvu vykreslíme pro aktuálně vybraný widget
		if (focusedWidget != null) {
			renderAnchor(gui, focusedWidget.widget.getAnchorX(), focusedWidget.widget.getAnchorY());
		}

		// Vykreslení tlačítek všech registrovaných widgetů
		for (ConfigWidgetHolder holder : widgets) {
			holder.button.render(gui, mouseX, mouseY, partialTicks);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			if (focusedWidget != null) {
				// Druhý klik kamkoliv widget pustí a uloží na disk
				focusedWidget.widget.save();
				focusedWidget = null;
				return true;
			}

			// Chycení: najdeme widget, na který hráč kliknul
			for (ConfigWidgetHolder holder : widgets) {
				if (holder.button.isMouseOver(mouseX, mouseY)) {
					focusedWidget = holder;
					grabOffsetX = (int) mouseX - holder.bounds.x();
					grabOffsetY = (int) mouseY - holder.bounds.y();
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (key == GLFW.GLFW_KEY_ESCAPE || key == OpmModKeys.CONFIG.getKey().getValue()) {
			onClose();
			return true;
		}

		if (focusedWidget != null) {
			boolean changed = switch (key) {
				case GLFW.GLFW_KEY_UP -> { focusedWidget.widget.setY(focusedWidget.widget.getY() - 1); yield true; }
				case GLFW.GLFW_KEY_DOWN -> { focusedWidget.widget.setY(focusedWidget.widget.getY() + 1); yield true; }
				case GLFW.GLFW_KEY_LEFT -> { focusedWidget.widget.setX(focusedWidget.widget.getX() - 1); yield true; }
				case GLFW.GLFW_KEY_RIGHT -> { focusedWidget.widget.setX(focusedWidget.widget.getX() + 1); yield true; }
				case GLFW.GLFW_KEY_KP_7 -> { focusedWidget.widget.setAnchorX(AnchorX.LEFT); yield true; }
				case GLFW.GLFW_KEY_KP_8 -> { focusedWidget.widget.setAnchorX(AnchorX.MIDDLE); yield true; }
				case GLFW.GLFW_KEY_KP_9 -> { focusedWidget.widget.setAnchorX(AnchorX.RIGHT); yield true; }
				case GLFW.GLFW_KEY_KP_4 -> { focusedWidget.widget.setAnchorY(AnchorY.TOP); yield true; }
				case GLFW.GLFW_KEY_KP_1 -> { focusedWidget.widget.setAnchorY(AnchorY.MIDDLE); yield true; }
				case GLFW.GLFW_KEY_KP_0 -> { focusedWidget.widget.setAnchorY(AnchorY.BOTTOM); yield true; }
				case GLFW.GLFW_KEY_SPACE -> { focusedWidget.widget.save(); yield false; }
				default -> false;
			};

			if (changed) {
				focusedWidget.update(font, width, height);
				return true;
			}
		}

		return super.keyPressed(key, scan, mods);
	}

	@Override
	public void onClose() {
		if (focusedWidget != null) {
			focusedWidget.widget.save();
			focusedWidget = null;
		}
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void renderAnchor(GuiGraphics gui, AnchorX ax, AnchorY ay) {
		int anchorX = 1;
		int anchorY = 0;
		switch (ax) {
			case RIGHT -> anchorX = width - font.width("⚓");
			case MIDDLE -> anchorX = width / 2 - font.width("⚓") / 2;
			default -> {}
		}
		switch (ay) {
			case BOTTOM -> anchorY = height - 9;
			case MIDDLE -> anchorY = height / 2 - 4;
			default -> {}
		}
		gui.drawString(font, "⚓", anchorX + 2, anchorY + 2, OpmColors.BLUE);
		gui.renderOutline(anchorX, anchorY, font.width("⚓") + 4, 12, OpmColors.BLUE);
	}
}