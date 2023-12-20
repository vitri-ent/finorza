package io.pyke.vitri.finorza.inference.api;

public interface IKeyboardHandler {
	void vitri$onKey(int key, int scanCode, int action, int modifiers);

	boolean vitri$isKeyPressed(int key);
}
