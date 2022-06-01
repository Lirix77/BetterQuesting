package betterquesting.client.gui2;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.client.gui.misc.INeedsRefresh;
import betterquesting.api.enums.EnumQuestVisibility;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestLine;
import betterquesting.api.questing.IQuestLineEntry;
import betterquesting.api2.cache.QuestCache;
import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.controls.IPanelButton;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.controls.PanelButtonQuest;
import betterquesting.api2.client.gui.controls.PanelButtonStorage;
import betterquesting.api2.client.gui.events.IPEventListener;
import betterquesting.api2.client.gui.events.PEventBroadcaster;
import betterquesting.api2.client.gui.events.PanelEvent;
import betterquesting.api2.client.gui.events.types.PEventButton;
import betterquesting.api2.client.gui.misc.*;
import betterquesting.api2.client.gui.panels.CanvasTextured;
import betterquesting.api2.client.gui.panels.bars.PanelVScrollBar;
import betterquesting.api2.client.gui.panels.content.PanelGeneric;
import betterquesting.api2.client.gui.panels.content.PanelLine;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import betterquesting.api2.client.gui.panels.lists.CanvasQuestLine;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.api2.client.gui.resources.colors.GuiColorPulse;
import betterquesting.api2.client.gui.resources.colors.GuiColorStatic;
import betterquesting.api2.client.gui.resources.textures.GuiTextureColored;
import betterquesting.api2.client.gui.resources.textures.OreDictTexture;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetIcon;
import betterquesting.api2.client.gui.themes.presets.PresetLine;
import betterquesting.api2.client.gui.themes.presets.PresetTexture;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.client.gui2.editors.GuiQuestLinesEditor;
import betterquesting.client.gui2.editors.designer.GuiDesigner;
import betterquesting.client.gui2.editors.nbt.GuiNbtEditor;
import betterquesting.network.handlers.NetQuestAction;
import betterquesting.network.handlers.NetSettingSync;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.QuestLineDatabase;
import betterquesting.storage.QuestSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;

import java.util.*;

public class GuiQuestLines extends GuiScreenCanvas implements IPEventListener, INeedsRefresh
{
    private IQuestLine selectedLine = null;
    private static int selectedLineId = -1;
    
    private CanvasQuestLine cvQuest;
    
    private CanvasScrolling cvDesc;
    private PanelVScrollBar scDesc;
    private CanvasScrolling cvLines;
    private PanelTextBox txDesc;
    
    private PanelButton claimAll;
    
    private final List<PanelButtonStorage<DBEntry<IQuestLine>>> btnListRef = new ArrayList<>();
    
    public GuiQuestLines(GuiScreen parent)
    {
        super(parent);
    }
    
    @Override
    public void refreshGui()
    {
        refreshContent();
    }
    
    @Override
    public void initPanel()
    {
        super.initPanel();
        
        if(selectedLineId >= 0)
        {
            selectedLine = QuestLineDatabase.INSTANCE.getValue(selectedLineId);
            if(selectedLine == null) selectedLineId = -1;
        } else
        {
            selectedLine = null;
        }
        
        boolean canEdit = QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(mc.player);
        PEventBroadcaster.INSTANCE.register(this, PEventButton.class);
        
        CanvasTextured cvBackground = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), PresetTexture.PANEL_MAIN.getTexture());
        this.addPanel(cvBackground);
        if (canEdit) {
            PanelButton btnEdit = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER,-116, -16, 16, 16, 0), 3, "").setIcon(PresetIcon.ICON_GEAR.getTexture());
            btnEdit.setClickAction((b) -> mc.displayGuiScreen(new GuiQuestLinesEditor(this)));
            cvBackground.addPanel(btnEdit);
            PanelButton btnConfigEdit = new PanelButton(new GuiTransform(GuiAlign.TOP_LEFT, new GuiPadding(0, 0, -16, -16), 0), 4, "").setIcon(PresetIcon.ICON_GEAR.getTexture());
            btnConfigEdit.setClickAction((b)-> mc.displayGuiScreen(new GuiNbtEditor(this, QuestSettings.INSTANCE.writeToNBT(new NBTTagCompound()), (value) ->
                    {
                        QuestSettings.INSTANCE.readFromNBT(value);
                        NetSettingSync.requestEdit();
                    }))
            );
            cvBackground.addPanel(btnConfigEdit);
        }
        PanelButton btnExit = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, -100, -16, 100, 16, 0), 0, QuestTranslation.translate("betterquesting.home.exit"));
        btnExit.setClickAction((b) -> mc.displayGuiScreen(parent));
        cvBackground.addPanel(btnExit);
        PanelButton btnTheme = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, 0, -16, 100, 16, 0), 0, QuestTranslation.translate("betterquesting.home.theme"));
        btnTheme.setClickAction((b) -> mc.displayGuiScreen(new GuiThemes(this)));
        cvBackground.addPanel(btnTheme);
        CanvasTextured cvFrame = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(174, 16, 16, 66), 0), PresetTexture.AUX_FRAME_0.getTexture());
        cvBackground.addPanel(cvFrame);

        cvLines = new CanvasScrolling(new GuiTransform(GuiAlign.LEFT_EDGE, new GuiPadding(16, 16, -158, 16), 0));
        cvBackground.addPanel(cvLines);
        PanelVScrollBar scLines = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(0, 0, -8, 0), 0));
        cvLines.setScrollDriverY(scLines);
        cvBackground.addPanel(scLines);
        scLines.getTransform().setParent(this.cvLines.getTransform());
        refreshList();

        cvDesc = new CanvasScrolling(new GuiTransform(GuiAlign.BOTTOM_EDGE, new GuiPadding(174, -66, 24, 16), 0));
        cvBackground.addPanel(cvDesc);
        txDesc = new PanelTextBox(new GuiRectangle(0, 0, cvDesc.getTransform().getWidth(), 0, 0), "", true);
        txDesc.setColor(PresetColor.TEXT_MAIN.getColor());
        this.cvDesc.addCulledPanel(txDesc, false);
        scDesc = new PanelVScrollBar(new GuiTransform(GuiAlign.BOTTOM_RIGHT, new GuiPadding(-24, -66, 16, 16), 0));
        cvDesc.setScrollDriverY(scDesc);
        cvBackground.addPanel(scDesc);

        if(canEdit) {
            PanelButton btnDesign = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_RIGHT, -48, -16, 16, 16, -2), 6, "");
            btnDesign.setIcon(PresetIcon.ICON_SORT.getTexture());
            btnDesign.setClickAction((b) -> {
                if (selectedLine != null) mc.displayGuiScreen(new GuiDesigner(this, selectedLine));
            });
            btnDesign.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.designer")));
            cvFrame.addPanel(btnDesign);
        }
        PanelButton fitView = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_RIGHT, -16, -16, 16, 16, -2), 5, "");
        fitView.setIcon(PresetIcon.ICON_BOX_FIT.getTexture());
        fitView.setClickAction((b) -> {
            if(cvQuest.getQuestLine() != null) cvQuest.fitToWindow();
        });
        fitView.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.zoom_fit")));
        cvFrame.addPanel(fitView);
        
        claimAll = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_RIGHT, -32, -16, 16, 16, -2), 4, "");
        claimAll.setIcon(PresetIcon.ICON_CHEST.getTexture());
        claimAll.setClickAction((b) -> {
            if(cvQuest.getQuestButtons().size() <= 0) return;
            List<Integer> claimIdList = new ArrayList<>();
            for(PanelButtonQuest pbQuest : cvQuest.getQuestButtons())
            {
                IQuest q = pbQuest.getStoredValue().getValue();
                if(q.getRewards().size() > 0 && q.canClaim(mc.player)) claimIdList.add(pbQuest.getStoredValue().getID());
            }
            
            int[] cIDs = new int[claimIdList.size()];
            for(int i = 0; i < cIDs.length; i++)
            {
                cIDs[i] = claimIdList.get(i);
            }
    
            NetQuestAction.requestClaim(cIDs);
            claimAll.setIcon(PresetIcon.ICON_CHEST.getTexture(), new GuiColorStatic(0xFF444444), 0);
        });
        claimAll.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.claim_all")));
        cvFrame.addPanel(this.claimAll);
        
        // === CHAPTER VIEWPORT ===
        
        CanvasQuestLine oldCvQuest = cvQuest;
        cvQuest = new CanvasQuestLine(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), 2);
        cvFrame.addPanel(cvQuest);
    
        if(selectedLine != null)
        {
            cvQuest.setQuestLine(selectedLine);
            
            if(oldCvQuest != null)
            {
                cvQuest.setZoom(oldCvQuest.getZoom());
                cvQuest.setScrollX(oldCvQuest.getScrollX());
                cvQuest.setScrollY(oldCvQuest.getScrollY());
                cvQuest.refreshScrollBounds();
                cvQuest.updatePanelScroll();
            }

            txDesc.setText(QuestTranslation.translate(selectedLine.getUnlocalisedDescription()));
            cvDesc.refreshScrollBounds();
            scDesc.setEnabled(this.cvDesc.getScrollBounds().getHeight() > 0);
        }
        
        // === MISC ===

        IGuiRect ls0 = new GuiTransform(GuiAlign.TOP_LEFT, 16, 16, 0, 0, 0);
        ls0.setParent(cvBackground.getTransform());
        IGuiRect le0 = new GuiTransform(GuiAlign.TOP_LEFT, 166, 16, 0, 0, 0);
        le0.setParent(cvBackground.getTransform());
        PanelLine paLine0 = new PanelLine(ls0, le0, PresetLine.GUI_DIVIDER.getLine(), 1, PresetColor.GUI_DIVIDER.getColor(), -1);
        cvBackground.addPanel(paLine0);
        IGuiRect ls1 = new GuiTransform(GuiAlign.BOTTOM_LEFT, 16, -16, 0, 0, 0);
        ls1.setParent(cvBackground.getTransform());
        IGuiRect le1 = new GuiTransform(GuiAlign.BOTTOM_LEFT, 166, -16, 0, 0, 0);
        le1.setParent(cvBackground.getTransform());
        PanelLine paLine1 = new PanelLine(ls1, le1, PresetLine.GUI_DIVIDER.getLine(), 1, PresetColor.GUI_DIVIDER.getColor(), 1);
        cvBackground.addPanel(paLine1);
        IGuiRect ls3 = new GuiTransform(GuiAlign.BOTTOM_LEFT, 174, -16, 0, 0, 0);
        ls3.setParent(cvBackground.getTransform());
        IGuiRect le3 = new GuiTransform(GuiAlign.BOTTOM_RIGHT, -16, -16, 0, 0, 0);
        le3.setParent(cvBackground.getTransform());
        PanelLine paLine3 = new PanelLine(ls3, le3, PresetLine.GUI_DIVIDER.getLine(), 1, PresetColor.GUI_DIVIDER.getColor(), 1);
        cvBackground.addPanel(paLine3);

        refreshClaimAll();
    }
    
    @Override
    public void onPanelEvent(PanelEvent event)
    {
        if(event instanceof PEventButton)
        {
            onButtonPress((PEventButton)event);
        }
    }
    
    // TODO: Change CanvasQuestLine to NOT need these panel events anymore
    private void onButtonPress(PEventButton event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        IPanelButton btn = event.getButton();
        
        if(btn.getButtonID() == 2 && btn instanceof PanelButtonStorage) // Quest Instance Select
        {
            @SuppressWarnings("unchecked")
            DBEntry<IQuest> quest = ((PanelButtonStorage<DBEntry<IQuest>>)btn).getStoredValue();
            GuiHome.bookmark = new GuiQuest(this, quest.getID());
            
            mc.displayGuiScreen(GuiHome.bookmark);
        }
    }
    
    private void refreshList()
    {
        boolean canEdit = QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(mc.player);
        List<DBEntry<IQuestLine>> lineList = QuestLineDatabase.INSTANCE.getSortedEntries();
        cvLines.resetCanvas();
        btnListRef.clear();
        UUID playerID = QuestingAPI.getQuestingUUID(mc.player);
        int n = 0;
        int listW = cvLines.getTransform().getWidth();
        for(DBEntry<IQuestLine> dbEntry : lineList)
        {
            IQuestLine ql = dbEntry.getValue();
            EnumQuestVisibility vis = ql.getProperty(NativeProps.VISIBILITY);
            if(!canEdit && vis == EnumQuestVisibility.HIDDEN) continue;
        
            boolean show = false;
            boolean unlocked = false;
            boolean complete = false;
            boolean allComplete = true;
            boolean pendingClaim = false;
        
            if(canEdit)
            {
                show = true;
                unlocked = true;
                complete = true;
            }
            
            for(DBEntry<IQuestLineEntry> qID : ql.getEntries())
            {
                IQuest q = QuestDatabase.INSTANCE.getValue(qID.getID());
                if(q == null) continue;
                
                if(allComplete && !q.isComplete(playerID)) allComplete = false;
                if(!pendingClaim && q.isComplete(playerID) && !q.hasClaimed(playerID)) pendingClaim = true;
                if(!unlocked && q.isUnlocked(playerID)) unlocked = true;
                if(!complete && q.isComplete(playerID)) complete = true;
                if(!show && QuestCache.isQuestShown(q, playerID, mc.player)) show = true;
                if(unlocked && complete && show && pendingClaim && !allComplete) break;
            }
        
            if(vis == EnumQuestVisibility.COMPLETED && !complete)
            {
                continue;
            } else if(vis == EnumQuestVisibility.UNLOCKED && !unlocked)
            {
                continue;
            }
            
            int val = pendingClaim ? 1 : 0;
            if(allComplete) val |= 2;
            if(!show) val |= 4;

            cvLines.addPanel(new PanelGeneric(new GuiRectangle(0, n * 16, 16, 16, 0), new OreDictTexture(1F,ql.getProperty(NativeProps.ICON), false, true)));
            if((val & 1) > 0)
            {
                cvLines.addPanel(new PanelGeneric(new GuiRectangle(8, n * 16 + 8, 8, 8, -1), new GuiTextureColored(PresetIcon.ICON_NOTICE.getTexture(), new GuiColorStatic(0xFFFFFF00))));
            } else if((val & 2) > 0)
            {
                cvLines.addPanel(new PanelGeneric(new GuiRectangle(8, n * 16 + 8, 8, 8, -1), new GuiTextureColored(PresetIcon.ICON_TICK.getTexture(), new GuiColorStatic(0xFF00FF00))));
            }
            PanelButtonStorage<DBEntry<IQuestLine>> btnLine = new PanelButtonStorage<>(new GuiRectangle(16, n++ * 16, listW - 16, 16, 0), 1, QuestTranslation.translate(ql.getUnlocalisedName()), dbEntry);
            btnLine.setTextAlignment(0);
            btnLine.setActive((val & 4) == 0 && dbEntry.getID() != selectedLineId);
            btnLine.setCallback((q) -> {
                btnListRef.forEach((b) -> {if(b.getStoredValue().getID() == selectedLineId) b.setActive(true);});
                btnLine.setActive(false);
                selectedLine = q.getValue();
                selectedLineId = q.getID();
                cvQuest.setQuestLine(q.getValue());
                txDesc.setText(QuestTranslation.translate(q.getValue().getUnlocalisedDescription()));
                cvDesc.refreshScrollBounds();
                refreshClaimAll();
                scDesc.setEnabled(this.cvDesc.getScrollBounds().getHeight() > 0);
            });
            cvLines.addPanel(btnLine);
            btnListRef.add(btnLine);
        }
        cvLines.refreshScrollBounds();
    }

    private void refreshContent()
    {
        if(selectedLineId >= 0)
        {
            selectedLine = QuestLineDatabase.INSTANCE.getValue(selectedLineId);
            if(selectedLine == null) selectedLineId = -1;
        } else
        {
            selectedLine = null;
        }
        cvQuest.setQuestLine(selectedLine);
        if (selectedLine != null) {
            txDesc.setText(QuestTranslation.translate(selectedLine.getUnlocalisedDescription()));
        } else {
            txDesc.setText("");
        }
        cvDesc.refreshScrollBounds();
        refreshClaimAll();
        scDesc.setEnabled(cvDesc.getScrollBounds().getHeight() > 0);
    }
    
    private void refreshClaimAll()
    {
        if(cvQuest.getQuestLine() == null || cvQuest.getQuestButtons().size() <= 0)
        {
            claimAll.setActive(false);
            claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorStatic(0xFF444444), 0);
            return;
        }
        
        for(PanelButtonQuest btn : cvQuest.getQuestButtons())
        {
            if(btn.getStoredValue().getValue().canClaim(mc.player))
            {
                claimAll.setActive(true);
                claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorPulse(0xFFFFFFFF, 0xFF444444, 2F, 0F), 0);
                return;
            }
        }
        
        claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorStatic(0xFF444444), 0);
        claimAll.setActive(false);
    }
}
