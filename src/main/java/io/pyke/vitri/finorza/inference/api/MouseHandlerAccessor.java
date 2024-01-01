package io.pyke.vitri.finorza.inference.api;

public interface MouseHandlerAccessor {
	void vitri$onPress(int button, int action, int mods);

	void vitri$onMove(double xpos, double ypos);
}
