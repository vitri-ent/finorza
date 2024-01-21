package io.pyke.vitri.finorza.inference.config;

import com.google.gson.annotations.SerializedName;

public final class Config {
	public boolean synchronizeIntegratedServer = false;
	public int controlPort = 24351;
	public CursorSize cursorSize = CursorSize.PX_16;
	public WindowSize windowSize = WindowSize.R_720;
	public ObservationFrameSize observationFrameSize = ObservationFrameSize.MineRL;
	public boolean compressObservation = true;

	public enum CursorSize {
		@SerializedName("4x4") PX_4(4),
		@SerializedName("8x8") PX_8(8),
		@SerializedName("16x16") PX_16(16);

		public final int size;

		CursorSize(int size) {
			this.size = size;
		}
	}

	public enum WindowSize {
		@SerializedName("360p") R_360(640, 360),
		@SerializedName("720p") R_720(1280, 720),
		@SerializedName("1080p") R_1080(1920, 1080);

		public final int width;
		public final int height;

		WindowSize(int width, int height) {
			this.width = width;
			this.height = height;
		}
	}

	public enum ObservationFrameSize {
		@SerializedName("minerl") MineRL,
		@SerializedName("act-rvit") ACT_RViT,
		@SerializedName("native") Native;
	}
}
