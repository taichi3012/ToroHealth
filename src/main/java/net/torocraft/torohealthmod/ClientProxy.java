package net.torocraft.torohealthmod;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.torocraft.torohealthmod.config.ConfigurationHandler;
import net.torocraft.torohealthmod.gui.GuiEntityStatus;
import net.torocraft.torohealthmod.render.DamageParticle;
import org.apache.commons.lang3.tuple.Pair;

public class ClientProxy extends CommonProxy {

  GuiEntityStatus entityStatusGUI;
  private Minecraft mc = Minecraft.getMinecraft();
  private final Map<EntityLivingBase, Float> healthMap = new WeakHashMap<>();
  private final Map<EntityLivingBase, Pair<Long, Float>> AccumulatedDamage = new WeakHashMap<>();
  private EntityLivingBase pointedEntity;

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

    float currentHealth = entity.getHealth();
    if (!healthMap.containsKey(entity)) {
      healthMap.put(entity, currentHealth);
      return;
    }

    float prevHealth = healthMap.get(entity);
    if (currentHealth == prevHealth) {
      return;
    }

    displayParticle(entity, MathHelper.ceiling_float_int(prevHealth - currentHealth));
    healthMap.put(entity, currentHealth);
    if (entity == pointedEntity) {
      entityStatusGUI.setEntity(entity);
    }

    Pair<Long, Float> pair = AccumulatedDamage.get(entity);
    long time = System.currentTimeMillis();
    if (pair == null || time - pair.getLeft() > ConfigurationHandler.hideDelay) {
      AccumulatedDamage.put(entity, Pair.of(time, prevHealth));
      return;
    }

    if (currentHealth < pair.getRight()) {
      AccumulatedDamage.put(entity, Pair.of(time, pair.getRight()));
    } else {
      AccumulatedDamage.put(entity, Pair.of(time, currentHealth));
    }
  }

  private void displayParticle(Entity entity, int damage) {
    if (damage == 0) {
      return;
    }

    World world = entity.worldObj;
    Vec3 vec = new Vec3(0d, 0d, 1d)
      .rotateYaw(-MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw + 90) * (float) Math.PI / 180);
    double rand = world.rand.nextGaussian() / 8;
    double scale = (rand + (rand <= 0 ? -0.2 : 0.2)) * (entity.getDistanceToEntity(mc.thePlayer) * 0.02f + 0.95f);
    double motionX = vec.xCoord * scale;
    double motionY = vec.yCoord;
    double motionZ = vec.zCoord * scale;
    DamageParticle damageIndicator = new DamageParticle(damage, world, entity.posX, entity.posY + entity.height / 2F + Math.abs(world.rand.nextGaussian()) / 3F, entity.posZ, motionX, motionY, motionZ);
    Minecraft.getMinecraft().effectRenderer.addEffect(damageIndicator);

  }

  @Override
  public void setEntityInCrosshairs(float partialTicks) {
    EntityLivingBase hit = getPointedEntity(partialTicks);
    if (hit != null) {
      entityStatusGUI.setEntity(pointedEntity = hit);
    }
  }

  @Override
  public float getAccumulatedDamageReference(EntityLivingBase entity) {
    Pair<Long, Float> pair = AccumulatedDamage.get(entity);
    long time = System.currentTimeMillis();
    if (pair == null || time - pair.getLeft() > ConfigurationHandler.hideDelay) {
      return 0F;
    }
    return pair.getRight();
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
      if (entity == observer || entity == mc.thePlayer.ridingEntity) {
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
