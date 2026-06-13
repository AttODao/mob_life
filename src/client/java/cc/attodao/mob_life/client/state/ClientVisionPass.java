package cc.attodao.mob_life.client.state;

public final class ClientVisionPass {
	private static boolean distancePass;

	private ClientVisionPass() {
	}

	public static boolean isDistancePass() {
		return distancePass;
	}

	public static void setDistancePass(boolean active) {
		distancePass = active;
	}
}
