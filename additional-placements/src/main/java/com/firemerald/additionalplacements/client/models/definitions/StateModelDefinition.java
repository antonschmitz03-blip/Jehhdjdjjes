package com.firemerald.additionalplacements.client.models.definitions;

import com.mojang.math.Quadrant;
import net.minecraft.resources.Identifier;

public record StateModelDefinition(String model, Quadrant xRotation, Quadrant yRotation) {
	public StateModelDefinition(String model, Quadrant yRotation) {
		this(model, Quadrant.R0, yRotation);
	}

	public StateModelDefinition(String model) {
		this(model, Quadrant.R0, Quadrant.R0);
	}

	public Identifier location(Identifier prefix) {
		return prefix.withSuffix(model);
	}
}