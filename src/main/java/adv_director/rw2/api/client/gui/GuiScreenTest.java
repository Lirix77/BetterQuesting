package adv_director.rw2.api.client.gui;

import java.awt.Color;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.util.vector.Vector4f;
import adv_director.rw2.api.client.gui.controls.IValueIO;
import adv_director.rw2.api.client.gui.controls.PanelButton;
import adv_director.rw2.api.client.gui.misc.GuiAlign;
import adv_director.rw2.api.client.gui.misc.GuiPadding;
import adv_director.rw2.api.client.gui.misc.GuiTransform;
import adv_director.rw2.api.client.gui.misc.IGuiRect;
import adv_director.rw2.api.client.gui.panels.CanvasTextured;
import adv_director.rw2.api.client.gui.panels.bars.PanelHBarFill;
import adv_director.rw2.api.client.gui.panels.content.PanelPlayerPortrait;
import adv_director.rw2.api.client.gui.themes.TexturePreset;
import adv_director.rw2.api.client.gui.themes.ThemeRegistry;

public class GuiScreenTest extends GuiScreenCanvas
{
	public GuiScreenTest(GuiScreen parent, GuiTransform transform)
	{
		super(parent, transform);
	}
	
	@Override
	public void initGui()
	{
		super.initGui();
		
		IGuiRect ctt = new GuiTransform(new Vector4f(0.05F, 0.05F, 0.95F, 0.95F), new GuiPadding(0, 0, 0, 0), 0);
		CanvasTextured cvt1 =  new CanvasTextured(ctt, ThemeRegistry.INSTANCE.getTexture(TexturePreset.PANEL_MAIN));
		this.addPanel(cvt1);
		
		IGuiRect btt1 = new GuiTransform(GuiAlign.BOTTOM_CENTER, new GuiPadding(-100, -16, 0, 0), -1);
		IGuiRect btt2 = new GuiTransform(GuiAlign.BOTTOM_CENTER, new GuiPadding(0, -16, -100, 0), -1);
		PanelButton btn1 = new PanelButton(btt1, 0, "Button 1");
		PanelButton btn2 = new PanelButton(btt2, 1, "Button 2");
		cvt1.addPanel(btn1);
		cvt1.addPanel(btn2);
		
		IGuiRect pfbt = new GuiTransform(GuiAlign.BOTTOM_EDGE, new GuiPadding(0, -32, 0, 0), 0);
		PanelHBarFill pfb = new PanelHBarFill(pfbt);
		pfb.setBarTexture(ThemeRegistry.INSTANCE.getTexture(TexturePreset.METER_H_0), ThemeRegistry.INSTANCE.getTexture(TexturePreset.METER_H_0));
		pfb.setFillColor(Color.RED.getRGB(), Color.GREEN.getRGB(), 0.25F, true);
		cvt1.addPanel(pfb);
		
		pfb.setFillDriver(new IValueIO<Float>()
		{
			@Override
			public Float readValue()
			{
				double d = Math.sin(Math.toRadians((Minecraft.getSystemTime()%10000L)/10000D * 360D));
				return (float)(d + 1F)/2F;
			}

			@Override
			public void writeValue(Float value)
			{
			}
		});
		
		try
		{
			IGuiRect pt1 = new GuiTransform(GuiAlign.TOP_LEFT, new GuiPadding(0, 0, -64, -64), 0);
			IGuiRect pt2 = new GuiTransform(GuiAlign.TOP_LEFT, new GuiPadding(64, 0, -112, -48), 0);
			IGuiRect pt3 = new GuiTransform(GuiAlign.TOP_LEFT, new GuiPadding(112, 0, -144, -32), 0);
			PanelPlayerPortrait pp1 = new PanelPlayerPortrait(pt1, UUID.fromString("10755ea6-9721-467a-8b5c-92adf689072c"), "Darkosto");
			PanelPlayerPortrait pp2 = new PanelPlayerPortrait(pt2, UUID.fromString("ef35a72a-ef00-4c2a-a2a9-58a54a7bb9fd"), "GreatOrator");
			PanelPlayerPortrait pp3 = new PanelPlayerPortrait(pt3, UUID.fromString("4412cc00-65de-43ff-b19a-10e0ec64cc4a"), "Funwayguy");
			cvt1.addPanel(pp1);
			cvt1.addPanel(pp2);
			cvt1.addPanel(pp3);
		} catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void drawScreen(int mx, int my, float partialTick)
	{
		this.drawDefaultBackground();
		
		super.drawScreen(mx, my, partialTick);
	}
}