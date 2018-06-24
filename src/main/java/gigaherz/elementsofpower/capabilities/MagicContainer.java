package gigaherz.elementsofpower.capabilities;

import gigaherz.elementsofpower.database.MagicAmounts;

public class MagicContainer implements IMagicContainer
{
    private MagicAmounts capacity;
    private MagicAmounts containedMagic;

    public MagicContainer() {}

    @Override
    public MagicAmounts getCapacity()
    {
        return capacity;
    }

    @Override
    public void setCapacity(MagicAmounts capacity)
    {
        this.capacity = capacity;
    }

    @Override
    public MagicAmounts getContainedMagic()
    {
        return containedMagic;
    }

    @Override
    public void setContainedMagic(MagicAmounts containedMagic)
    {
        this.containedMagic = containedMagic;
    }
}
