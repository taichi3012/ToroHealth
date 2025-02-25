package net.torocraft.torohealthmod;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.torocraft.torohealthmod.config.ConfigurationHandler;
import net.torocraft.torohealthmod.gui.GuiEntityStatus;
import net.torocraft.torohealthmod.render.DamageParticle;

public class ClientProxy extends CommonProxy {

  GuiEntityStatus entityStatusGUI;
  private Minecraft mc = Minecraft.getMinecraft();

  @Override
  public void preInit(FMLPreInitializationEvent e) {
    super.preInit(e);
    entityStatusGUI = new GuiEntityStatus();
  }

  @Override
  public void init(FMLInitializationEvent e) {
    super.init(e);
  }

  @Override
  public void postInit(FMLPostInitializationEvent e) {
    super.postInit(e);
    MinecraftForge.EVENT_BUS.register(entityStatusGUI);
  }

  @Override
  public void displayDamageDealt(EntityLivingBase entity) {

    if (!entity.worldObj.isRemote) {
      return;
    }

    if (!ConfigurationHandler.showDamageParticles) {
      return;
    }

    int currentHealth = (int) Math.ceil(entity.getHealth());

    if (entity.getEntityData().hasKey("health")) {
      int entityHealth = ((NBTTagInt) entity.getEntityData().getTag("health")).getInt();

      if (entityHealth != currentHealth) {
        displayParticle(entity, (int) entityHealth - currentHealth);
      }
    }

    entity.getEntityData().setTag("health", new NBTTagInt(currentHealth));
  }

  private void displayParticle(Entity entity, int damage) {
    if (damage == 0) {
      return;
    }

    World world = entity.worldObj;
    double motionX = world.rand.nextGaussian() * 0.02;
    double motionY = 0.5f;
    double motionZ = world.rand.nextGaussian() * 0.02;
    DamageParticle damageIndicator = new DamageParticle(damage, world, entity.posX, entity.posY + entity.height, entity.posZ, motionX, motionY, motionZ);
    Minecraft.getMinecraft().effectRenderer.addEffect(damageIndicator);

  }

  @Override
  public void setEntityInCrosshairs(float partialTicks) {
    EntityLivingBase hit = getPointedEntity(partialTicks);
    if (hit != null) {
      entityStatusGUI.setEntity(hit);
    }
  }

  private EntityLivingBase getPointedEntity(float partialTicks) {
    Entity observer = this.mc.getRenderViewEntity();
    if (observer == null || this.mc.theWorld == null) {
      return null;
    }

    double reach = 50d;
    Vec3 positionEyes = observer.getPositionEyes(partialTicks);
    MovingObjectPosition objectMouseOver = observer.rayTrace(reach, partialTicks);
    if (objectMouseOver != null) {
      reach = objectMouseOver.hitVec.distanceTo(positionEyes);
    }

    Vec3 positionPointed = positionEyes.add(mulScalar(observer.getLook(partialTicks), reach));
    AxisAlignedBB box = new AxisAlignedBB(
      positionEyes.xCoord, positionEyes.yCoord, positionEyes.zCoord,
      positionPointed.xCoord, positionPointed.yCoord, positionPointed.zCoord
    );

    double nearlest = reach;
    EntityLivingBase result = null;
    for (EntityLivingBase entity : this.mc.theWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
      if (entity == observer) {
        continue;
      }

      float borderSize = entity.getCollisionBorderSize();
      AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
      if (axisalignedbb.isVecInside(positionEyes)) {
        return entity;
      }

      MovingObjectPosition movingObjectPosition = axisalignedbb.calculateIntercept(positionEyes, positionPointed);
      if (movingObjectPosition != null) {
        double distance = positionEyes.distanceTo(movingObjectPosition.hitVec);
        if (distance < nearlest) {
          result = entity;
          nearlest = distance;
        }
      }
    }

    return result;
  }

  private Vec3 mulScalar(Vec3 vec, double a) {
    return new Vec3(vec.xCoord * a, vec.yCoord * a, vec.zCoord * a);
  }

  private boolean isSolidBlock(BlockPos pos, BlockPos prevPos) {
    return prevPos.compareTo(pos) == 0;
  }

}
