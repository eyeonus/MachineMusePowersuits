package net.machinemuse.powersuits.client.render.modelspec;

import com.google.common.base.Objects;
import net.machinemuse.numina.utils.map.MuseRegistry;

public abstract class Spec extends MuseRegistry<PartSpec> {
    private final String name;
    private final boolean isDefault;
    private final EnumSpecType specType;

    public Spec(String name, boolean isDefault, EnumSpecType specType) {
        this.name = name;
        this.isDefault = isDefault;
        this.specType = specType;
    }

    public abstract String getDisaplayName();

    public Iterable<PartSpec> getPartSpecs() {
        return this.elems();
    }

    public PartSpec getPart(String partName) {
        for (PartSpec partSpec: this.elems())
            if (partSpec.partName.equals(partName))
                return partSpec;
        return null;
    }

    public Iterable<String> getPartSpecNames() {
        return this.names();
    }

    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public EnumSpecType getSpecType() {
        return specType;
    }

    public String getOwnName() {
        String name = ModelRegistry.getInstance().getName(this);
        return (name != null) ? name : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Spec spec = (Spec) o;
        return isDefault() == spec.isDefault() &&
                Objects.equal(getName(), spec.getName()) &&
                getSpecType() == spec.getSpecType();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName(), isDefault(), getSpecType());
    }
}