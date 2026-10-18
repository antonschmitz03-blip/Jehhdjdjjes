package io.github.antonschmitz.diamondmachines;

import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.kinetics.saw.SawMovementBehaviour;

/** Saw on a contraption: cuts as much faster as the placed diamond saw does. */
public class DiamondSawMovementBehaviour extends SawMovementBehaviour {
    @Override
    protected float getBlockBreakingSpeed(MovementContext context) {
        return super.getBlockBreakingSpeed(context) * DiamondMachines.BREAK_SPEED_MULTIPLIER;
    }
}
