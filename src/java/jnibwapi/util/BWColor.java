package jnibwapi.util;

public class BWColor {

	public static final int RED = 111;
	public static final int BLUE = 165;
	public static final int TEAL = 159;
	public static final int PURPLE = 164;
	public static final int ORANGE = 179;
	public static final int BROWN = 19;
	public static final int WHITE = 255;
	public static final int YELLOW = 135;
	public static final int GREEN = 117;
	public static final int CYAN = 128;
	public static final int BLACK = 0;
	public static final int GREY = 74;

	// x02 - Cyan (Default)
	// x03 - Yellow
	// x04 - White
	// x05 - Grey (no override)
	// x06 - Red
	// x07 - Green
	// x08 - Red (P1)
	// x09 - Tab
	// x0A - Newline
	// x0B - Invisible (no override)
	// x0C - Remove beyond (no override)
	// x0D - Clear formatting?
	// x0E - Blue (P2)
	// x0F - Teal (P3)
	// x10 - Purple (P4)
	// x11 - Orange (P5)
	// x12 - Right Align
	// x13 - Center Align
	// x14 - Invisible
	// x15 - Brown (p6)
	// x16 - White (p7)
	// x17 - Yellow (p8)
	// x18 - Green (p9)
	// x19 - Brighter Yellow (p10)
	// x1A - Cyan (player default)
	// x1B - Pinkish (p11)
	// x1C - Dark Cyan (p12)
	// x1D - Greygreen
	// x1E - Bluegrey
	// x1F - Turquoise

	public static String getToStringHex(int color) {
		switch (color) {
		case CYAN:
			return "\u0002";
		case YELLOW:
			return "\u0003";
		case WHITE:
			return "\u0004";
		case GREY:
			return "\u0005";
		case RED:
			return "\u0006";
		case GREEN:
			return "\u0007";
		case BLUE:
			return "\u000E";
		case PURPLE:
			return "\u0010";
		case ORANGE:
			return "\u0011";
		case BROWN:
			return "\u0015";
			// case :
			// return "\u0018";
			// case :
			// return "\u0019";
			// case :
			// return "\u001A";
			// case :
			// return "\u001B";
			// case :
			// return "\u001C";
			// case :
			// return "\u001D";
			// case :
			// return "\u001E";
		case TEAL:
			return "\u001F";
		default:
			return "ERROR";
		}
	}

}
