package caeruleusTait.world.preview.client.gui.screens;

import caeruleusTait.world.preview.client.gui.screens.settings.CacheTab;
import caeruleusTait.world.preview.client.gui.screens.settings.GeneralTab;
import caeruleusTait.world.preview.client.gui.screens.settings.SamplingTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import static caeruleusTait.world.preview.client.WorldPreviewComponents.SETTINGS_TITLE;

public class ModMenuSettingsScreen extends Screen {

    public static final ResourceLocation FOOTER_SEPERATOR = ResourceLocation.parse("textures/gui/footer_separator.png");

    private final Screen lastScreen;
    private final TabManager tabManager;
    private TabNavigationBar tabNavigationBar;
    private GridLayout bottomButtons;

    public ModMenuSettingsScreen(Screen lastScreen) {
        super(SETTINGS_TITLE);
        this.lastScreen = lastScreen;
        this.tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    }

    @Override
    protected void init() {
        tabNavigationBar = TabNavigationBar.builder(tabManager, this.width)
                .addTabs(
                        new GeneralTab(minecraft),
                        new CacheTab(minecraft, null),
                        new SamplingTab(minecraft)
                )
                .build();
        tabNavigationBar.selectTab(0, false);
        addRenderableWidget(tabNavigationBar);

        bottomButtons = new GridLayout().columnSpacing(10);
        GridLayout.RowHelper rowHelper = bottomButtons.createRowHelper(1);
        rowHelper.addChild(Button.builder(CommonComponents.GUI_BACK, button -> onClose()).build());
        this.bottomButtons.visitWidgets((abstractWidget) -> {
            abstractWidget.setTabOrderGroup(1);
            this.addRenderableWidget(abstractWidget);
        });

        repositionElements();
    }

    @Override
    public void repositionElements() {
        if (tabNavigationBar != null) {
            tabNavigationBar.setWidth(this.width);
            tabNavigationBar.arrangeElements();

            bottomButtons.arrangeElements();
            FrameLayout.centerInRectangle(this.bottomButtons, 0, this.height - 36, this.width, 36);
            int i = this.tabNavigationBar.getRectangle().bottom();
            ScreenRectangle screenRectangle = new ScreenRectangle(0, i, this.width, this.bottomButtons.getY() - i);
            this.tabManager.setTabArea(screenRectangle);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blit(RenderType::guiTextured, FOOTER_SEPERATOR, 0, Mth.roundToward(this.height - 36 - 2, 2), 0.0F, 0.0F, this.width, 2, 32, 2);
        super.render(guiGraphics, i, j, f);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen); // return
    }
}
