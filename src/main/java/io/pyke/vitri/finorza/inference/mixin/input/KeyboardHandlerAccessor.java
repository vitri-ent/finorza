package io.pyke.vitri.finorza.inference.mixin.input;

public interface KeyboardHandlerAccessor {
	void vitri$onKey(int key, int scanCode, int action, int modifiers);

	boolean vitri$isKeyPressed(int key);
}
