package io.github.antonschmitz.diamondmachines;

import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.kinetics.drill.DrillMovementBehaviour;

/** Drill on a contraption: breaks blocks as much faster as the placed diamond drill does. */
public class DiamondDrillMovementBehaviour extends DrillMovementBehaviour {
    @Override
    protected float getBlockBreakingSpeed(MovementContext context) {
        return super.getBlockBreakingSpeed(context) * DiamondMachines.BREAK_SPEED_MULTIPLIER;
    }
}
