package betterquesting.client.gui2;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.client.gui.misc.INeedsRefresh;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.rewards.IReward;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.controls.IPanelButton;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.events.IPEventListener;
import betterquesting.api2.client.gui.events.PEventBroadcaster;
import betterquesting.api2.client.gui.events.PanelEvent;
import betterquesting.api2.client.gui.events.types.PEventButton;
import betterquesting.api2.client.gui.misc.*;
import betterquesting.api2.client.gui.panels.CanvasEmpty;
import betterquesting.api2.client.gui.panels.CanvasTextured;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.client.gui.panels.bars.PanelVScrollBar;
import betterquesting.api2.client.gui.panels.content.PanelGeneric;
import betterquesting.api2.client.gui.panels.content.PanelLine;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.api2.client.gui.resources.textures.IGuiTexture;
import betterquesting.api2.client.gui.resources.textures.SimpleTexture;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetLine;
import betterquesting.api2.client.gui.themes.presets.PresetTexture;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.network.handlers.NetQuestAction;
import betterquesting.questing.QuestDatabase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.vector.Vector4f;

import java.util.List;

import static org.lwjgl.Sys.openURL;

public class GuiQuest extends GuiScreenCanvas implements IPEventListener, INeedsRefresh
{
    private final int questID;
    
    private IQuest quest;
    
    private PanelButton btnDetect;
    private PanelButton btnClaim;
    
    private CanvasEmpty cvInner;
    private CanvasScrolling cvDesc;
    private PanelVScrollBar paDescScroll;
    
    private IGuiRect rectReward;
    private IGuiRect rectTask;
    
    private CanvasEmpty pnReward;
    private CanvasEmpty pnTask;
    
    public GuiQuest(GuiScreen parent, int questID)
    {
        super(parent);
        this.questID = questID;
    }
    
    @Override
    public void initPanel()
    {
        super.initPanel();
        
        this.quest = QuestDatabase.INSTANCE.getValue(questID);
        
        if(quest == null)
        {
            mc.displayGuiScreen(this.parent);
            return;
        }
    
        PEventBroadcaster.INSTANCE.register(this, PEventButton.class);
    
        // Background panel
        CanvasTextured cvBackground = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), PresetTexture.PANEL_MAIN.getTexture());
        this.addPanel(cvBackground);
    
        PanelTextBox panTxt = new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 16, 0, -32), 0), QuestTranslation.translate(quest.getProperty(NativeProps.NAME))).setAlignment(1);
        panTxt.setColor(PresetColor.TEXT_HEADER.getColor());
        cvBackground.addPanel(panTxt);
    
        if(QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(mc.thePlayer))
        {
            cvBackground.addPanel(new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, -100, -16, 100, 16, 0), 0, QuestTranslation.translate("gui.back")));
            cvBackground.addPanel(new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, 0, -16, 100, 16, 0), 1, QuestTranslation.translate("betterquesting.btn.edit")));
        } else
        {
            cvBackground.addPanel(new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, -100, -16, 200, 16, 0), 0, QuestTranslation.translate("gui.back")));
        }
        
        cvInner = new CanvasEmpty(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(16, 32, 16, 24), 0));
        cvBackground.addPanel(cvInner);
        
        if(quest.getRewards().size() > 0)
        {
            cvDesc = new CanvasScrolling(new GuiTransform(new Vector4f(0F, 0F, 0.5F, 0.5F), new GuiPadding(0, 0, 16, 16), 0));
            cvInner.addPanel(cvDesc);
            PanelTextBox paDesc = new PanelTextBox(new GuiRectangle(0, 0, cvDesc.getTransform().getWidth(), 0), QuestTranslation.translate(quest.getProperty(NativeProps.DESC)), true);
            paDesc.setColor(PresetColor.TEXT_MAIN.getColor());//.setFontSize(10);
            cvDesc.addCulledPanel(paDesc, false);
            
            paDescScroll = new PanelVScrollBar(new GuiTransform(GuiAlign.quickAnchor(GuiAlign.TOP_CENTER, GuiAlign.MID_CENTER), new GuiPadding(-16, 0, 8, 16), 0));
            cvInner.addPanel(paDescScroll);
            cvDesc.setScrollDriverY(paDescScroll);
            paDescScroll.setEnabled(cvDesc.getScrollBounds().getHeight() > 0);
    
            btnClaim = new PanelButton(new GuiTransform(new Vector4f(0F, 1F, 0.5F, 1F), new GuiPadding(0, -16, 8, 0), 0), 6, QuestTranslation.translate("betterquesting.btn.claim"));
            btnClaim.setActive(false);
            cvInner.addPanel(btnClaim);
            
            rectReward = new GuiTransform(new Vector4f(0F, 0.5F, 0.5F, 1F), new GuiPadding(0, 0, 8, 16), 0);
            rectReward.setParent(cvInner.getTransform());
            
            refreshRewardPanel();
        } else
        {
            cvDesc = new CanvasScrolling(new GuiTransform(GuiAlign.HALF_LEFT, new GuiPadding(0, 0, 16, 0), 0));
            cvInner.addPanel(cvDesc);
            PanelTextBox paDesc = new PanelTextBox(new GuiRectangle(0, 0, cvDesc.getTransform().getWidth(), 0), QuestTranslation.translate(quest.getProperty(NativeProps.DESC)), true);
            paDesc.setColor(PresetColor.TEXT_MAIN.getColor());//.setFontSize(10);
            cvDesc.addCulledPanel(paDesc, false);
            
            paDescScroll = new PanelVScrollBar(new GuiTransform(GuiAlign.quickAnchor(GuiAlign.TOP_CENTER, GuiAlign.BOTTOM_CENTER), new GuiPadding(-16, 0, 8, 0), 0));
            cvInner.addPanel(paDescScroll);
            cvDesc.setScrollDriverY(paDescScroll);
            paDescScroll.setEnabled(cvDesc.getScrollBounds().getHeight() > 0);
        }

        refreshRewardDescPanel();
        
        //if(quest.getTasks().size() > 0)
        {
            btnDetect = new PanelButton(new GuiTransform(new Vector4f(0.5F, 1F, 1F, 1F), new GuiPadding(8, -16, 0, 0), 0), 7, QuestTranslation.translate("betterquesting.btn.detect_submit"));
            btnDetect.setActive(false);
            cvInner.addPanel(btnDetect);
            
            rectTask = new GuiTransform(GuiAlign.HALF_RIGHT, new GuiPadding(8, 16, 0, 16), 0);
            rectTask.setParent(cvInner.getTransform());
            
            refreshTaskPanel();
        }
    
        IGuiRect ls0 = new GuiTransform(GuiAlign.TOP_CENTER, 0, 0, 0, 0, 0);
        ls0.setParent(cvInner.getTransform());
        IGuiRect le0 = new GuiTransform(GuiAlign.BOTTOM_CENTER, 0, 0, 0, 0, 0);
        le0.setParent(cvInner.getTransform());
        PanelLine paLine0 = new PanelLine(ls0, le0, PresetLine.GUI_DIVIDER.getLine(), 1, PresetColor.GUI_DIVIDER.getColor(), 1);
        cvInner.addPanel(paLine0);
    }
    
    @Override
    public void refreshGui()
    {
        this.refreshTaskPanel();
        this.refreshRewardDescPanel();
        this.refreshRewardPanel();
        this.updateButtons();
    }
    
    @Override
    public boolean onMouseClick(int mx, int my, int click)
    {
        if(super.onMouseClick(mx, my, click))
        {
            this.updateButtons();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean onMouseScroll(int mx, int my, int scroll)
    {
        if(super.onMouseScroll(mx, my, scroll))
        {
            this.updateButtons();
            return true;
        }
    
        return false;
    }
    
    @Override
    public boolean onKeyTyped(char c, int keycode)
    {
        if(super.onKeyTyped(c, keycode))
        {
            this.updateButtons();
            return true;
        }
        
        return false;
    }
    
    @Override
    public void onPanelEvent(PanelEvent event)
    {
        if(event instanceof PEventButton)
        {
            onButtonPress((PEventButton)event);
        }
    }
    
    private void onButtonPress(PEventButton event)
    {
        IPanelButton btn = event.getButton();
        
        if(btn.getButtonID() == 0) // Exit
        {
            mc.displayGuiScreen(this.parent);
        } else if(btn.getButtonID() == 1) // Edit
        {
            //mc.displayGuiScreen(new GuiQuestEditor(this, quest));
            mc.displayGuiScreen(new betterquesting.client.gui2.editors.GuiQuestEditor(this, questID));
        } else if(btn.getButtonID() == 6) // Reward claim
        {
            NetQuestAction.requestClaim(new int[]{questID});
        } else if(btn.getButtonID() == 7) // Task detect/submit
        {
            NetQuestAction.requestDetect(new int[]{questID});
        }
    }
    
    private void refreshRewardPanel()
    {
        if(pnReward != null)
        {
            cvInner.removePanel(pnReward);
        }
        
        if(rectReward == null)
        {
            this.initPanel();
            return;
        }

        pnReward = new CanvasEmpty(rectReward);
        cvInner.addPanel(pnReward);
        int yOffset = 0;

        CanvasScrolling cvList = new CanvasScrolling(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 8, 0), 0));
        pnReward.addPanel(cvList);

        PanelVScrollBar scList = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(-8, 0, 0, 0), 0));
        pnReward.addPanel(scList);
        cvList.setScrollDriverY(scList);

        for (DBEntry<IReward> entry : quest.getRewards().getEntries()) {
            IReward rew = entry.getValue();

            PanelTextBox titleReward = new PanelTextBox(new GuiTransform(new Vector4f(), 0, yOffset, rectReward.getWidth(), 12, 0), QuestTranslation.translate(rew.getUnlocalisedName()));
            titleReward.setColor(PresetColor.TEXT_HEADER.getColor()).setAlignment(1);
            titleReward.setEnabled(true);
            cvList.addPanel(titleReward);
            yOffset += 12;

            IGuiPanel rewardGui = rew.getRewardGui(new GuiTransform(GuiAlign.FULL_BOX, 0, 0, rectReward.getWidth(), rectReward.getHeight(), 111), new DBEntry<>(questID, quest));
            rewardGui.initPanel();
            // Wrapping into canvas allow avoid empty space at end
            CanvasEmpty tempCanvas = new CanvasEmpty(new GuiTransform(GuiAlign.TOP_LEFT, 0, yOffset, rectReward.getWidth(), rewardGui.getTransform().getHeight() - rewardGui.getTransform().getY(), 1));
            cvList.addPanel(tempCanvas);
            tempCanvas.addPanel(rewardGui);
            yOffset += tempCanvas.getTransform().getHeight();
        }
        updateButtons();
    }

    private void refreshRewardDescPanel()
    {
        if(cvDesc != null) {
            cvInner.removePanel(cvDesc);
            cvInner.removePanel(paDescScroll);
        }

        cvDesc = new CanvasScrolling(new GuiTransform(GuiAlign.HALF_LEFT, new GuiPadding(0, 0, 16, 18), 0));
        cvInner.addPanel(cvDesc);

        String desc = QuestTranslation.translate(quest.getProperty(NativeProps.DESC));
        int panelPrevY = 0;
        int panelPrevHeight = 0;

        if(desc.contains("{Embed}")) {
            String[] descSplit = desc.split("\\{Embed\\}");
            for(String split : descSplit) {
                if(split.startsWith("TypeImage") || split.startsWith("TypeLink")) {
                    try {
                        String[] embedEntry = split.split(";");
                        int width = Math.min(cvDesc.getTransform().getWidth(), Integer.parseInt(embedEntry[2]));
                        int height = Integer.parseInt(embedEntry[3]);

                        if(embedEntry[0].contentEquals("TypeImage")) {
                            IGuiTexture embedImage = new SimpleTexture(new ResourceLocation(embedEntry[1]), new GuiRectangle(0, 0, Integer.parseInt(embedEntry[4]), Integer.parseInt(embedEntry[5])));
                            cvDesc.addCulledPanel(new PanelGeneric(new GuiRectangle((cvDesc.getTransform().getWidth()-width)/2, panelPrevY+panelPrevHeight+8, width, height), embedImage), false);
                        }
                        else if(embedEntry[0].contentEquals("TypeLink")) {
                            PanelButton btnLink = new PanelButton(new GuiRectangle((cvDesc.getTransform().getWidth()-width)/2, panelPrevY+panelPrevHeight+8, width, height), 69, embedEntry[4]);
                            btnLink.setClickAction((b) -> {
                                if (Minecraft.getMinecraft().gameSettings.chatLinksPrompt) {
                                    GuiScreen oldScreen = Minecraft.getMinecraft().currentScreen;
                                    Minecraft.getMinecraft().displayGuiScreen(new GuiConfirmOpenLink(new GuiYesNoCallback()
                                    {
                                        @Override
                                        public void confirmClicked(boolean result, int id)
                                        {
                                            if (result)
                                            {
                                                openURL(embedEntry[1]);
                                            }

                                            Minecraft.getMinecraft().displayGuiScreen(oldScreen);
                                        }
                                    }, embedEntry[1], 0, false));
                                } else {
                                    openURL(embedEntry[1]);
                                }
                            });
                            btnLink.setTextAlignment(1);
                            cvDesc.addCulledPanel(btnLink, false);
                        }

                        panelPrevY = panelPrevY+panelPrevHeight+8;
                        panelPrevHeight = height;
                    }
                    catch(Exception ex) {}
                }
                else {
                    PanelTextBox paDesc = new PanelTextBox(new GuiRectangle(0, panelPrevY+panelPrevHeight+12, cvDesc.getTransform().getWidth(), 0), split, true);
                    paDesc.setColor(PresetColor.TEXT_MAIN.getColor());
                    cvDesc.addCulledPanel(paDesc, false);

                    panelPrevY = panelPrevY+panelPrevHeight+12;
                    panelPrevHeight = paDesc.getTransform().getHeight();
                }
            }
            panelPrevY += 32;
        }
        else {
            PanelTextBox paDesc = new PanelTextBox(new GuiRectangle(0, 0, cvDesc.getTransform().getWidth(), 0), desc, true);
            paDesc.setColor(PresetColor.TEXT_MAIN.getColor());
            cvDesc.addCulledPanel(paDesc, false);
            panelPrevY = 32;
            panelPrevHeight = paDesc.getTransform().getHeight()+8;
        }

        paDescScroll = new PanelVScrollBar(new GuiTransform(GuiAlign.quickAnchor(GuiAlign.TOP_CENTER, GuiAlign.BOTTOM_CENTER), new GuiPadding(-16, 0, 8, 18), 0));
        cvInner.addPanel(paDescScroll);
        cvDesc.setScrollDriverY(paDescScroll);
        paDescScroll.setEnabled(cvDesc.getScrollBounds().getHeight() > 0);

        if(quest.getRewards().size()<=0)
        {
            updateButtons();
            return;
        }

        for(int index = 0; index<quest.getRewards().size(); index++) {
            IReward rew = quest.getRewards().getEntries().get(index).getValue();
            PanelTextBox rwdTitle = new PanelTextBox(new GuiRectangle(0, panelPrevY + panelPrevHeight, cvDesc.getTransform().getWidth(), 16, 0), QuestTranslation.translate(rew.getUnlocalisedName())).setColor(PresetColor.TEXT_HEADER.getColor()).setAlignment(1);
            IGuiPanel pnReward = rew.getRewardGui(new GuiRectangle(0, panelPrevY + panelPrevHeight + 16, cvDesc.getTransform().getWidth(), 32, 0), new DBEntry<>(questID, quest));

            cvDesc.addCulledPanel(rwdTitle, false);
            cvDesc.addCulledPanel(pnReward, false);

            int height = rectReward.getHeight()-32;

            CanvasEmpty pnRewardCanvas = (CanvasEmpty)pnReward;
            for(IGuiPanel pnl : pnRewardCanvas.getChildren()) {
                if(pnl.getClass().isAssignableFrom(CanvasScrolling.class)) {
                    CanvasScrolling pnlScrolling = (CanvasScrolling)pnl;
                    height = pnlScrolling.getScrollBounds().getHeight() + 32;
                }
            }

            IGuiPanel pnRewardReplacement = rew.getRewardGui(new GuiRectangle(0, panelPrevY + panelPrevHeight + 16, rectReward.getWidth(), height, 0), new DBEntry<>(questID, quest));
            cvDesc.removePanel(pnReward);
            cvDesc.addCulledPanel(pnRewardReplacement, false);

            CanvasEmpty pnRewardReplacementCanvas = (CanvasEmpty)pnRewardReplacement;
            for(IGuiPanel pnlRepl : pnRewardReplacementCanvas.getChildren()) {
                if(pnlRepl.getClass().isAssignableFrom(PanelVScrollBar.class)) {
                    pnRewardReplacementCanvas.removePanel(pnlRepl);
                }
            }

            panelPrevY = panelPrevY + panelPrevHeight + 16;
            panelPrevHeight = height+8;
        }

        paDescScroll.setEnabled(cvDesc.getScrollBounds().getHeight() > 0);

        updateButtons();
    }

    private void refreshTaskPanel()
    {
        if(pnTask != null)
        {
            cvInner.removePanel(pnTask);
        }

        pnTask = new CanvasEmpty(rectTask);
        cvInner.addPanel(pnTask);
        CanvasScrolling cvList = new CanvasScrolling(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 8, 0), 0));
        pnTask.addPanel(cvList);

        PanelVScrollBar scList = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(-8, 0, 0, 0), 0));
        pnTask.addPanel(scList);
        cvList.setScrollDriverY(scList);

        int yOffset = 0;
        List<DBEntry<ITask>> entries = quest.getTasks().getEntries();
        for (int i = 0; i < entries.size(); i++) {
            ITask tsk = entries.get(i).getValue();
            String taskName = (i + 1) + ". " + QuestTranslation.translate(tsk.getUnlocalisedName());
            PanelTextBox titleReward = new PanelTextBox(new GuiTransform(new Vector4f(), 0, yOffset, rectTask.getWidth(), 12, 0), taskName);
            titleReward.setColor(PresetColor.TEXT_HEADER.getColor()).setAlignment(1);
            titleReward.setEnabled(true);
            cvList.addPanel(titleReward);
            yOffset += 10;
            IGuiPanel taskGui = tsk.getTaskGui(new GuiTransform(GuiAlign.FULL_BOX, 0, 0, rectTask.getWidth(), rectTask.getHeight(), 0), new DBEntry<>(questID, quest));
            taskGui.initPanel();
            // Wrapping into canvas allow avoid empty space at end
            CanvasEmpty tempCanvas = new CanvasEmpty(new GuiTransform(GuiAlign.TOP_LEFT, 0, yOffset, rectTask.getWidth(), taskGui.getTransform().getHeight() - taskGui.getTransform().getY(), 1));
            cvList.addPanel(tempCanvas);
            tempCanvas.addPanel(taskGui);
            int guiHeight = tempCanvas.getTransform().getHeight();
            yOffset += guiHeight;
            //Indent from the previous
            yOffset += 8;
        }
        updateButtons();
    }
    
    private void updateButtons()
    {
        Minecraft mc = Minecraft.getMinecraft();
        
        if(btnClaim != null)
        {
            // Claim button state
            btnClaim.setActive(quest.getRewards().size() > 0 && quest.canClaim(mc.thePlayer));
        }
        
        if(btnDetect != null)
        {
            // Detect/submit button state
            btnDetect.setActive(quest.canSubmit(mc.thePlayer));
        }
    }
}
