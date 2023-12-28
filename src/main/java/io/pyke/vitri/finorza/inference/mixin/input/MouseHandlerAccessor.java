package io.pyke.vitri.finorza.inference.mixin.input;

public interface MouseHandlerAccessor {
	void vitri$onPress(int button, int action, int mods);

	void vitri$onMove(double xpos, double ypos);
}
