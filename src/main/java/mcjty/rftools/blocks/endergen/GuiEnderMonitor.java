package mcjty.rftools.blocks.endergen;

import mcjty.container.GenericGuiContainer;
import mcjty.gui.Window;
import mcjty.gui.events.ChoiceEvent;
import mcjty.gui.layout.HorizontalLayout;
import mcjty.gui.layout.VerticalLayout;
import mcjty.gui.widgets.ChoiceLabel;
import mcjty.gui.widgets.Label;
import mcjty.gui.widgets.Panel;
import mcjty.gui.widgets.Widget;
import mcjty.rftools.RFTools;
import mcjty.network.Argument;
import net.minecraft.inventory.Container;
import org.lwjgl.input.Mouse;

import java.awt.*;

public class GuiEnderMonitor extends GenericGuiContainer<EnderMonitorTileEntity> {
    public static final int MONITOR_WIDTH = 140;
    public static final int MONITOR_HEIGHT = 30;

    private ChoiceLabel mode;

    public GuiEnderMonitor(EnderMonitorTileEntity enderMonitorTileEntity, Container container) {
        super(enderMonitorTileEntity, container, RFTools.GUI_MANUAL_MAIN, "endermon");
        xSize = MONITOR_WIDTH;
        ySize = MONITOR_HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();

        Panel toplevel = new Panel(mc, this).setFilledRectThickness(2).setLayout(new VerticalLayout());

        Label label = new Label(mc, this).setText("Mode:");
        initGuiMode();

        Panel bottomPanel = new Panel(mc, this).setLayout(new HorizontalLayout()).addChild(label).addChild(mode);
        toplevel.addChild(bottomPanel);

        toplevel.setBounds(new Rectangle(guiLeft, guiTop, MONITOR_WIDTH, MONITOR_HEIGHT));
        window = new Window(this, toplevel);
    }

    private void initGuiMode() {
        mode = new ChoiceLabel(mc, this).setDesiredHeight(13).setDesiredWidth(80);
        for (EnderMonitorMode m : EnderMonitorMode.values()) {
            mode.addChoices(m.getDescription());
        }

        mode.setChoiceTooltip(EnderMonitorMode.MODE_LOSTPEARL.getDescription(), "Send a redstone pulse when a", "pearl is lost");
        mode.setChoiceTooltip(EnderMonitorMode.MODE_PEARLFIRED.getDescription(), "Send a redstone pulse when a", "pearl is fired");
        mode.setChoiceTooltip(EnderMonitorMode.MODE_PEARLARRIVED.getDescription(), "Send a redstone pulse when a", "pearl arrives");
        mode.setChoice(tileEntity.getMode().getDescription());
        mode.addChoiceEvent(new ChoiceEvent() {
            @Override
            public void choiceChanged(Widget parent, String newChoice) {
                changeMode();
            }
        });
    }

    private void changeMode() {
        EnderMonitorMode newMode = EnderMonitorMode.getMode(mode.getCurrentChoice());
        tileEntity.setMode(newMode);
        sendServerCommand(EnderMonitorTileEntity.CMD_MODE, new Argument("mode", newMode.getDescription()));
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float v, int i, int i2) {
        drawWindow();
        java.util.List<String> tooltips = window.getTooltips();
        if (tooltips != null) {
            int x = Mouse.getEventX() * width / mc.displayWidth;
            int y = height - Mouse.getEventY() * height / mc.displayHeight - 1;
            drawHoveringText(tooltips, x, y, mc.fontRenderer);
        }
    }
}
