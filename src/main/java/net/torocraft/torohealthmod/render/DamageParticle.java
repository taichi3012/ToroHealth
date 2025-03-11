package net.torocraft.torohealthmod.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.torocraft.torohealthmod.config.ConfigurationHandler;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class DamageParticle extends EntityFX {

  protected static final float GRAVITY = -0.3F;
  protected static final int LIFESPAN = 18;
  protected static final double BOUNCE_STRENGTH = 1.5F;

  protected String text;
  protected boolean shouldOnTop = true;
  protected float scale = 1.0F;
  private int damage;

  public DamageParticle(int damage, World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
    super(world, posX, posY, posZ, motionX, motionY, motionZ);
    this.motionX = motionX;
    this.motionY = motionY;
    this.motionZ = motionZ;
    this.particleTextureJitterX = 0.0F;
    this.particleTextureJitterY = 0.0F;
    this.particleGravity = GRAVITY;
    this.particleMaxAge = LIFESPAN;
    this.noClip = true;
    this.damage = damage;
    this.text = Integer.toString(Math.abs(damage));
  }

  protected DamageParticle(World worldIn, double posXIn, double posYIn, double posZIn) {
    this(0, worldIn, posXIn, posYIn, posZIn, 0, 0, 0);
  }

  @Override
  public void renderParticle(WorldRenderer worldRendererIn, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
    float rotationYaw = (-Minecraft.getMinecraft().thePlayer.rotationYaw);
    float rotationPitch = Minecraft.getMinecraft().thePlayer.rotationPitch;

    final float locX = ((float) (this.prevPosX + (this.posX - this.prevPosX) * partialTicks - interpPosX));
    final float locY = ((float) (this.prevPosY + (this.posY - this.prevPosY) * partialTicks - interpPosY));
    final float locZ = ((float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - interpPosZ));

    float scaleByDistance = MathHelper.sqrt_float(locX * locX + locY * locY + locZ * locZ) * 0.08f + 0.5f;
    GL11.glPushMatrix();
    if (this.shouldOnTop) {
      GL11.glDepthFunc(GL11.GL_ALWAYS);
    } else {
      GL11.glDepthFunc(GL11.GL_LEQUAL);
    }
    GL11.glTranslatef(locX, locY, locZ);
    GL11.glRotatef(rotationYaw, 0.0F, 1.0F, 0.0F);
    GL11.glRotatef(rotationPitch, 1.0F, 0.0F, 0.0F);

    GL11.glScalef(-1.0F, -1.0F, 1.0F);
    GL11.glScalef(0.04F, 0.04F, 0.04F);
    GL11.glScalef(this.scale * scaleByDistance, this.scale * scaleByDistance, this.scale * scaleByDistance);

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 0.003662109F);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
    GL11.glDepthMask(true);
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glEnable(GL11.GL_DEPTH_TEST);
    GL11.glDisable(GL11.GL_LIGHTING);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glEnable(GL11.GL_ALPHA_TEST);
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

    int color = ConfigurationHandler.damageColor;
    if (damage < 0) {
      color = ConfigurationHandler.healColor;
    }

    int alpha = 0xFF000000;
    if (particleMaxAge - particleAge < 10) {
      alpha = (int) (251F * ((particleMaxAge - particleAge + 1 - partialTicks) * 0.1F)) + 4;
      alpha <<= 24;
    }

    final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
    fontRenderer.drawStringWithShadow(this.text, -MathHelper.floor_float(fontRenderer.getStringWidth(this.text) / 2.0F) + 1, -MathHelper.floor_float(fontRenderer.FONT_HEIGHT / 2.0F) + 1, color | alpha);

    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    GL11.glDepthFunc(GL11.GL_LEQUAL);

    GL11.glPopMatrix();
  }

  @Override
  public void onUpdate() {
    super.onUpdate();
    this.motionX *= 0.75d;
    this.motionZ *= 0.75d;
  }

  @Override
  public int getFXLayer() {
    return 3;
  }

}
