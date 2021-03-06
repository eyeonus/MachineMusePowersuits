package net.machinemuse.powersuits.client.render.modelspec;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Objects;

/**
 * Ported to Java by lehjr on 11/8/16.
 */
@SideOnly(Side.CLIENT)
public class ModelPartSpec extends PartSpec {
    public boolean defaultglow;

    public ModelPartSpec(ModelSpec modelSpec,
                         Binding binding,
                         String partName,
                         Integer defaultcolourindex,
                         Boolean defaultglow) {
        super(modelSpec, binding, partName, defaultcolourindex);
        this.defaultglow = (defaultglow != null) ? defaultglow : false;
    }

    @Override
    public String getDisaplayName() {
        return I18n.format(new StringBuilder("model.")
                .append(this.spec.getOwnName())
                .append(".")
                .append(this.partName)
                .append(".partName")
                .toString());
    }

    public boolean getGlow() {
        return this.defaultglow;
    }

    public boolean getGlow(NBTTagCompound nbt) {
        return nbt.hasKey("glow") ? nbt.getBoolean("glow") : this.defaultglow;
    }

    public void setGlow(NBTTagCompound nbt, boolean g) {
        if (g == this.defaultglow) nbt.removeTag("glow");
        else nbt.setBoolean("glow", g);
    }

    public List<BakedQuad> getQuads() {
        return ((ModelSpec)(this.spec)).getModel().getQuadsforPart(this.partName);
    }

    public NBTTagCompound multiSet(NBTTagCompound nbt, Integer colourIndex, Boolean glow) {
        super.multiSet(nbt, colourIndex);
        this.setGlow(nbt, (glow != null) ? glow : false);
        return nbt;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(defaultglow);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final ModelPartSpec other = (ModelPartSpec) obj;
        return Objects.equals(this.defaultglow, other.defaultglow);
    }
}

