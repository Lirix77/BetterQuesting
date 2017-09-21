package adv_director.rw2.api.client.gui.panels;

import net.minecraft.client.renderer.GlStateManager;
import adv_director.rw2.api.client.gui.misc.GuiRectangle;
import adv_director.rw2.api.client.gui.misc.IGuiRect;
import adv_director.rw2.api.client.gui.resources.IGuiTexture;

public class CanvasTextured extends CanvasEmpty
{
	private final IGuiTexture bgTexture;
	
	public CanvasTextured(IGuiRect rect, IGuiTexture texture)
	{
		super(rect);
		
		this.bgTexture = texture;
	}
	
	@Override
	public void drawPanel(int mx, int my, float partialTick)
	{
		IGuiRect bounds = new GuiRectangle(this.getTransform());
		GlStateManager.pushMatrix();
		GlStateManager.color(1F, 1F, 1F, 1F);
		bgTexture.drawTexture(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 0F);
		GlStateManager.popMatrix();
		
		super.drawPanel(mx, my, partialTick);
	}
}
