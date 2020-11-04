package net.gudenau.minecraft.fps.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.world.Difficulty;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RabbitEntity.class)
public abstract class MontyPython extends AnimalEntity{
    @Shadow public abstract boolean tryAttack(Entity target);
    
    @Shadow public abstract int getRabbitType();
    
    @SuppressWarnings("ConstantConditions")
    private MontyPython(){
        super(null, null);
    }
    
    @Inject(
        method = "chooseType",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void chooseType(WorldAccess world, CallbackInfoReturnable<Integer> cir){
        if(world.getDifficulty() == Difficulty.PEACEFUL){
            return;
        }
        
        if(world.getRandom().nextInt(1000000) == 1){
            cir.setReturnValue(99);
        }
    }
    
    private float gud_fps$getDamageModifier(Entity entity){
        if(entity instanceof LivingEntity){
            LivingEntity living = (LivingEntity)entity;
            float modifier = 1;
    
            EntityAttributeInstance attribute = living.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
            if(attribute != null){
                modifier += attribute.getValue() * 2;
            }
            attribute = living.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
            if(attribute != null){
                modifier += attribute.getValue() * 4;
            }
            attribute = living.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            if(attribute != null){
                modifier += attribute.getValue() * 2;
            }
            StatusEffectInstance effect = living.getStatusEffect(StatusEffects.STRENGTH);
            if(effect != null){
                modifier *= (effect.getAmplifier() + 1);
            }
            effect = living.getStatusEffect(StatusEffects.RESISTANCE);
            if(effect != null){
                modifier *= (effect.getAmplifier() + 1);
            }
            return modifier;
        }else{
            return 1;
        }
    }
    
    @ModifyConstant(
        method = "tryAttack",
        constant = @Constant(floatValue = 8),
        remap = false
    )
    private float tryAttack$getEvilDamage(float original, Entity target){
        return 8 * gud_fps$getDamageModifier(target);
    }
    
    @Inject(
        method = "damage",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
        if(getRabbitType() == 99){
            if(isInvulnerableTo(source)){
                cir.setReturnValue(false);
            }
            
            float modifier = gud_fps$getDamageModifier(source.getAttacker());
            double dmodifier = Math.pow(2, modifier);
            if(dmodifier > Float.MAX_VALUE){
                cir.setReturnValue(false);
            }else{
                cir.setReturnValue(super.damage(source, (float)(amount / dmodifier)));
            }
        }
    }
}
